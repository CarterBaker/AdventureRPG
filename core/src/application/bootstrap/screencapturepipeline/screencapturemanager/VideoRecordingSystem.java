package application.bootstrap.screencapturepipeline.screencapturemanager;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.common.Codec;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

import application.bootstrap.renderpipeline.pbo.PBOInstance;
import application.bootstrap.renderpipeline.pbomanager.PBOManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class VideoRecordingSystem extends SystemPackage {

    /*
     * Owns a single toggled recording session and encodes it to MP4 live, so
     * the file is finished the moment recording stops. The main thread only
     * reads frames back through a PBO straight into a free slot of a small
     * ring; the VideoWrite thread scales and converts each slot to YUV and
     * the VideoEncode thread compresses and muxes it. Every frame carries the
     * real time it was on screen, so playback always matches real time: when
     * the encoder is busy no readback happens and the last frame is held.
     */

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern(EngineSetting.CAPTURE_TIMESTAMP_PATTERN);

    // Internal
    private File recordingDirectory;
    private ThreadHandle convertThread;
    private ThreadHandle encodeThread;
    private PBOManager pboManager;
    private PBOInstance pboInstance;

    // Session
    private boolean recording;
    private String activeBaseName;
    private WindowInstance activeWindow;
    private File activeFile;
    private int width;
    private int height;

    // Reference Clock
    private long recordingStartNanos;
    private long captureIntervalNanos;
    private long lastCaptureTick;
    private long queuedReadbackTick;
    private boolean readbackQueued;
    private long firstFrameTick;

    // Frame Ring
    private final ByteBuffer[] frameBuffers = new ByteBuffer[EngineSetting.RECORDING_BUFFER_COUNT];
    private final Picture[] framePictures = new Picture[EngineSetting.RECORDING_BUFFER_COUNT];
    private final long[] framePts = new long[EngineSetting.RECORDING_BUFFER_COUNT];
    private final long[] frameDurations = new long[EngineSetting.RECORDING_BUFFER_COUNT];
    private final Runnable[] convertTasks = new Runnable[EngineSetting.RECORDING_BUFFER_COUNT];
    private final Runnable[] encodeTasks = new Runnable[EngineSetting.RECORDING_BUFFER_COUNT];
    private final Future<?>[] pendingConversions = new Future<?>[EngineSetting.RECORDING_BUFFER_COUNT];
    private final Future<?>[] pendingEncodes = new Future<?>[EngineSetting.RECORDING_BUFFER_COUNT];
    private int activeSlot;
    private int heldSlot;
    private long heldFrameTick;

    // Scaling
    private int encodeWidth;
    private int encodeHeight;
    private int scaleSourceWidth = EngineSetting.INDEX_NOT_FOUND;
    private int scaleSourceHeight = EngineSetting.INDEX_NOT_FOUND;
    private int[] columnByteOffsets;
    private int[] rowByteOffsets;

    // Encoder
    private SeekableByteChannel outputChannel;
    private MP4Muxer muxer;
    private MuxerTrack videoTrack;
    private H264Encoder encoder;
    private ByteBuffer encodeOutputBuffer;
    private long encodedFrameCount;
    private volatile boolean encoderHealthy = true;
    private volatile String encoderFailureReason;
    private Future<?> pendingFinalize;

    // Internal \\

    @Override
    protected void create() {

        this.recordingDirectory = ScreenCaptureIOUtility.resolveCaptureDirectory(
                internal.path, EngineSetting.RECORDING_OUTPUT_DIRECTORY);
        this.convertThread = getThreadHandleFromThreadName(EngineSetting.VIDEO_WRITE_THREAD_NAME);
        this.encodeThread = getThreadHandleFromThreadName(EngineSetting.VIDEO_ENCODE_THREAD_NAME);
        this.captureIntervalNanos = EngineSetting.NANOS_PER_SECOND / EngineSetting.RECORDING_FRAME_RATE;

        for (int i = 0; i < EngineSetting.RECORDING_BUFFER_COUNT; i++) {
            int slot = i;
            convertTasks[i] = () -> convertSlot(slot);
            encodeTasks[i] = () -> encodeSlot(slot);
        }
    }

    @Override
    protected void get() {
        this.pboManager = get(PBOManager.class);
    }

    // Draw Authority \\

    void flush() {

        if (encoderFailureReason != null) {
            String reason = encoderFailureReason;
            encoderFailureReason = null;
            handleEncoderFailure(reason);
        }

        if (!recording)
            return;

        try {
            captureFrame();
        } catch (RuntimeException e) {
            errorLog("Video recording \"" + activeBaseName + "\" stopped: unexpected error in the capture pipeline — "
                    + e.getMessage());
            stopRecording();
        }
    }

    // Toggle \\

    void toggleRecording(WindowInstance window) {
        if (recording)
            stopRecording();
        else
            startRecording(window);
    }

    void releaseWindow(WindowInstance window) {
        if (recording && activeWindow == window)
            stopRecording();
    }

    // Session \\

    private void startRecording(WindowInstance window) {

        if (pendingFinalize != null && !pendingFinalize.isDone()) {
            errorLog("Video recording start request ignored — the previous recording is still finalizing.");
            return;
        }

        this.activeWindow = window;
        this.width = window.getWidth();
        this.height = window.getHeight();
        this.recordingStartNanos = System.nanoTime();
        this.lastCaptureTick = 0;
        this.queuedReadbackTick = 0;
        this.readbackQueued = false;
        this.firstFrameTick = EngineSetting.INDEX_NOT_FOUND;
        this.activeSlot = 0;
        this.heldSlot = EngineSetting.INDEX_NOT_FOUND;
        this.encoderHealthy = true;
        this.encoderFailureReason = null;

        computeEncodeDimensions();
        ensureFrameBuffers();

        if (pboInstance == null)
            pboInstance = pboManager.createPbo(window, width, height);

        String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
        this.activeBaseName = EngineSetting.RECORDING_FILE_PREFIX + timestamp;
        this.activeFile = new File(recordingDirectory, activeBaseName + "." + EngineSetting.RECORDING_EXTENSION);

        File outputFile = activeFile;
        executeAsync(encodeThread, () -> openEncoder(outputFile));

        this.recording = true;
        timeStampLog("Started video recording: " + activeBaseName
                + " (" + encodeWidth + "x" + encodeHeight + " at " + EngineSetting.RECORDING_FRAME_RATE + " fps)");
    }

    private void stopRecording() {

        this.recording = false;

        releaseHeldFrame();

        String finishedBaseName = activeBaseName;
        File finishedFile = activeFile;
        pendingFinalize = executeAsync(encodeThread, () -> finalizeEncoder(finishedBaseName, finishedFile));

        this.activeWindow = null;

        timeStampLog("Stopped video recording: " + finishedBaseName);
    }

    private void handleEncoderFailure(String reason) {

        if (!recording)
            return;

        errorLog("Video recording \"" + activeBaseName + "\" stopped: " + reason);
        stopRecording();
    }

    private void signalEncoderFailure(String reason) {

        encoderHealthy = false;

        if (encoderFailureReason == null)
            encoderFailureReason = reason;
    }

    // Shutdown \\

    @Override
    protected void dispose() {

        if (recording)
            stopRecording();

        if (pendingFinalize == null)
            return;

        log("Waiting for pending video recording finalization before shutdown...");

        try {
            pendingFinalize.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            errorLog("Recording finalization failed during shutdown: " + e.getCause());
        }
    }

    // Capture \\

    private void captureFrame() {

        long currentTick = (System.nanoTime() - recordingStartNanos) / captureIntervalNanos;

        if (currentTick <= lastCaptureTick)
            return;

        if (isSlotBusy(activeSlot))
            return;

        internal.windowPlatform.makeContextCurrent(activeWindow.getGLWindow());
        boolean hasFrame = pboInstance.capture(activeWindow, width, height, frameBuffers[activeSlot]);
        internal.windowPlatform.restoreMainContext();

        long frameTick = queuedReadbackTick;
        boolean frameBelongsToSession = readbackQueued;

        this.queuedReadbackTick = currentTick;
        this.readbackQueued = true;
        this.lastCaptureTick = currentTick;

        if (hasFrame && frameBelongsToSession)
            acceptFrame(frameTick);
    }

    private void acceptFrame(long frameTick) {

        if (firstFrameTick == EngineSetting.INDEX_NOT_FOUND)
            firstFrameTick = frameTick;

        if (heldSlot != EngineSetting.INDEX_NOT_FOUND)
            dispatchSlot(heldSlot, frameTick - heldFrameTick);

        this.heldSlot = activeSlot;
        this.heldFrameTick = frameTick;
        this.activeSlot = (activeSlot + 1) % EngineSetting.RECORDING_BUFFER_COUNT;
    }

    private void releaseHeldFrame() {

        if (heldSlot == EngineSetting.INDEX_NOT_FOUND)
            return;

        long currentTick = (System.nanoTime() - recordingStartNanos) / captureIntervalNanos;
        dispatchSlot(heldSlot, Math.max(1L, currentTick - heldFrameTick));

        this.heldSlot = EngineSetting.INDEX_NOT_FOUND;
    }

    // Dispatch \\

    private void dispatchSlot(int slot, long durationTicks) {

        framePts[slot] = heldFrameTick - firstFrameTick;
        frameDurations[slot] = durationTicks;

        pendingConversions[slot] = executeAsync(convertThread, convertTasks[slot]);
        pendingEncodes[slot] = executeAsync(encodeThread, encodeTasks[slot]);
    }

    private boolean isSlotBusy(int slot) {
        Future<?> encode = pendingEncodes[slot];
        return encode != null && !encode.isDone();
    }

    // Buffers \\

    private void ensureFrameBuffers() {

        int requiredCapacity = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;

        for (int i = 0; i < EngineSetting.RECORDING_BUFFER_COUNT; i++) {

            if (frameBuffers[i] == null || frameBuffers[i].capacity() != requiredCapacity)
                frameBuffers[i] = ByteBuffer.allocateDirect(requiredCapacity).order(ByteOrder.LITTLE_ENDIAN);

            Picture picture = framePictures[i];

            if (picture == null || picture.getWidth() != encodeWidth || picture.getHeight() != encodeHeight)
                framePictures[i] = Picture.create(encodeWidth, encodeHeight, ColorSpace.YUV420J);
        }
    }

    // Scaling \\

    private void computeEncodeDimensions() {

        if (width == scaleSourceWidth && height == scaleSourceHeight)
            return;

        int maxWidth = EngineSetting.RECORDING_MAX_WIDTH;

        int targetWidth = width <= maxWidth ? width : maxWidth;
        int targetHeight = width <= maxWidth ? height : Math.round(height * ((float) maxWidth / width));

        this.encodeWidth = targetWidth & ~1;
        this.encodeHeight = targetHeight & ~1;

        int stride = width * EngineSetting.BYTES_PER_PIXEL_BGRA;

        this.columnByteOffsets = new int[encodeWidth];
        for (int x = 0; x < encodeWidth; x++)
            columnByteOffsets[x] = Math.min(width - 1, x * width / encodeWidth) * EngineSetting.BYTES_PER_PIXEL_BGRA;

        this.rowByteOffsets = new int[encodeHeight];
        for (int y = 0; y < encodeHeight; y++)
            rowByteOffsets[y] = (height - 1 - Math.min(height - 1, y * height / encodeHeight)) * stride;

        this.scaleSourceWidth = width;
        this.scaleSourceHeight = height;
    }

    // Convert \\

    private void convertSlot(int slot) {

        ByteBuffer frame = frameBuffers[slot];
        Picture picture = framePictures[slot];

        byte[] lumaPlane = picture.getPlaneData(0);
        byte[] blueChromaPlane = picture.getPlaneData(1);
        byte[] redChromaPlane = picture.getPlaneData(2);
        int lumaStride = picture.getPlaneWidth(0);
        int chromaStride = picture.getPlaneWidth(1);

        for (int y = 0; y < encodeHeight; y += 2) {

            int sourceRowA = rowByteOffsets[y];
            int sourceRowB = rowByteOffsets[y + 1];
            int lumaRowA = y * lumaStride;
            int lumaRowB = lumaRowA + lumaStride;
            int chromaIndex = (y >> 1) * chromaStride;

            for (int x = 0; x < encodeWidth; x += 2) {

                int columnA = columnByteOffsets[x];
                int columnB = columnByteOffsets[x + 1];

                int pixelA = frame.getInt(sourceRowA + columnA);
                int pixelB = frame.getInt(sourceRowA + columnB);
                int pixelC = frame.getInt(sourceRowB + columnA);
                int pixelD = frame.getInt(sourceRowB + columnB);

                lumaPlane[lumaRowA + x] = toLuma(pixelA);
                lumaPlane[lumaRowA + x + 1] = toLuma(pixelB);
                lumaPlane[lumaRowB + x] = toLuma(pixelC);
                lumaPlane[lumaRowB + x + 1] = toLuma(pixelD);

                int red = ((pixelA >> 16) & 0xFF) + ((pixelB >> 16) & 0xFF)
                        + ((pixelC >> 16) & 0xFF) + ((pixelD >> 16) & 0xFF);
                int green = ((pixelA >> 8) & 0xFF) + ((pixelB >> 8) & 0xFF)
                        + ((pixelC >> 8) & 0xFF) + ((pixelD >> 8) & 0xFF);
                int blue = (pixelA & 0xFF) + (pixelB & 0xFF) + (pixelC & 0xFF) + (pixelD & 0xFF);

                blueChromaPlane[chromaIndex] = toChroma(EngineSetting.YUV_BLUE_CHROMA_RED * red
                        + EngineSetting.YUV_BLUE_CHROMA_GREEN * green + EngineSetting.YUV_BLUE_CHROMA_BLUE * blue);
                redChromaPlane[chromaIndex] = toChroma(EngineSetting.YUV_RED_CHROMA_RED * red
                        + EngineSetting.YUV_RED_CHROMA_GREEN * green + EngineSetting.YUV_RED_CHROMA_BLUE * blue);
                chromaIndex++;
            }
        }
    }

    private byte toLuma(int pixel) {

        int luma = (EngineSetting.YUV_LUMA_RED * ((pixel >> 16) & 0xFF)
                + EngineSetting.YUV_LUMA_GREEN * ((pixel >> 8) & 0xFF)
                + EngineSetting.YUV_LUMA_BLUE * (pixel & 0xFF)
                + EngineSetting.YUV_ROUNDING) >> EngineSetting.YUV_FIXED_POINT_SHIFT;

        return (byte) (luma + Byte.MIN_VALUE);
    }

    private byte toChroma(int weightedSum) {

        int chroma = (weightedSum + EngineSetting.YUV_CHROMA_ROUNDING) >> EngineSetting.YUV_CHROMA_FIXED_POINT_SHIFT;

        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, chroma));
    }

    // Encode \\

    private void openEncoder(File file) {
        try {

            this.outputChannel = NIOUtils.writableChannel(file);
            this.muxer = MP4Muxer.createMP4MuxerToChannel(outputChannel);
            this.videoTrack = muxer.addVideoTrack(Codec.H264, VideoCodecMeta.createSimpleVideoCodecMeta(
                    new Size(encodeWidth, encodeHeight), ColorSpace.YUV420J));

            this.encoder = H264Encoder.createH264Encoder();
            encoder.setMotionSearchRange(EngineSetting.RECORDING_MOTION_SEARCH_RANGE);

            int requiredCapacity = encoder.estimateBufferSize(framePictures[0]);

            if (encodeOutputBuffer == null || encodeOutputBuffer.capacity() < requiredCapacity)
                this.encodeOutputBuffer = ByteBuffer.allocate(requiredCapacity);

            this.encodedFrameCount = 0;

            log("Opened recording file: " + file.getName());
        } catch (IOException e) {
            NIOUtils.closeQuietly(outputChannel);
            this.outputChannel = null;
            this.muxer = null;
            file.delete();
            signalEncoderFailure("could not open " + file.getAbsolutePath() + " — " + e.getMessage());
        }
    }

    private void encodeSlot(int slot) {

        if (!awaitConversion(slot) || !encoderHealthy)
            return;

        try {

            encodeOutputBuffer.clear();
            VideoEncoder.EncodedFrame encodedFrame = encoder.encodeFrame(framePictures[slot], encodeOutputBuffer);

            videoTrack.addFrame(Packet.createPacket(
                    encodedFrame.getData(),
                    framePts[slot],
                    EngineSetting.RECORDING_FRAME_RATE,
                    frameDurations[slot],
                    encodedFrameCount,
                    encodedFrame.isKeyFrame() ? Packet.FrameType.KEY : Packet.FrameType.INTER,
                    null));

            encodedFrameCount++;
        } catch (IOException | RuntimeException e) {
            signalEncoderFailure("encoding failed — " + e.getMessage());
        }
    }

    private boolean awaitConversion(int slot) {
        try {
            pendingConversions[slot].get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            signalEncoderFailure("frame conversion failed — " + e.getCause());
            return false;
        }
    }

    private void finalizeEncoder(String baseName, File file) {

        if (muxer == null)
            return;

        boolean success = encoderHealthy && encodedFrameCount > 0;

        try {
            if (success)
                muxer.finish();
        } catch (IOException e) {
            errorLog("Failed to finalize recording \"" + baseName + "\": " + e.getMessage());
            success = false;
        } finally {
            NIOUtils.closeQuietly(outputChannel);
            this.outputChannel = null;
            this.muxer = null;
            this.videoTrack = null;
            this.encoder = null;
        }

        if (success)
            timeStampLog("Video recording saved: " + file.getName() + " (" + encodedFrameCount + " frames)");
        else if (file.delete())
            errorLog("Discarded incomplete recording: " + file.getName());
    }
}
