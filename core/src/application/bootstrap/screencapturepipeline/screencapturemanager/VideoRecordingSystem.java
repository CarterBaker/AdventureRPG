package application.bootstrap.screencapturepipeline.screencapturemanager;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jcodec.api.awt.AWTSequenceEncoder;

import application.bootstrap.renderpipeline.pbo.PboInstance;
import application.bootstrap.renderpipeline.pbomanager.PboManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

class VideoRecordingSystem extends SystemPackage {

    /*
     * Owns a single toggled recording session: frames are captured off the
     * GPU through a PboInstance and handed to two independent
     * single-threaded pools — one writing a lossless AVI incrementally, one
     * encoding a standard MP4 — so a slow MP4 pass can never throttle AVI
     * capture or vice versa. Every dispatch is paced by recomputing, from
     * the wall clock, how many nominal frames are due versus how many have
     * already been written, clamped per dispatch so a stall can never
     * balloon into one enormous blocking batch and so no elapsed time is
     * ever silently discarded. A frame buffer is only reused once both
     * writers have finished reading it. Stopping a recording queues a
     * finalization task on each writer's own pool; dispose() awaits both
     * synchronously so a shutdown mid-recording can never leave a corrupt
     * file.
     */

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern(EngineSetting.CAPTURE_TIMESTAMP_PATTERN);

    private static final int AVI_MAIN_HEADER_LENGTH = 56;
    private static final int AVI_STREAM_HEADER_LENGTH = 56;
    private static final int AVI_STREAM_FORMAT_LENGTH = 40;
    private static final int AVI_INDEX_ENTRY_LENGTH = 16;
    private static final int RIFF_CHUNK_HEADER_LENGTH = 8;

    private File recordingDirectory;
    private ThreadHandle aviWriteThread;
    private ThreadHandle videoEncodeThread;
    private PboManager pboManager;
    private PboInstance pboInstance;

    private boolean recording;
    private WindowInstance activeWindow;
    private int width;
    private int height;
    private long frameCount;

    private final ByteBuffer[] frameBuffers = new ByteBuffer[EngineSetting.RECORDING_FRAME_BUFFER_COUNT];
    private final Future<?>[] pendingAviWrites = new Future<?>[EngineSetting.RECORDING_FRAME_BUFFER_COUNT];
    private final Future<?>[] pendingMp4Writes = new Future<?>[EngineSetting.RECORDING_FRAME_BUFFER_COUNT];
    private int activeBufferIndex;

    // Pacing
    private long recordingStartNanos;
    private long captureIntervalNanos;
    private boolean fallingBehind;

    private final byte[] aviScratch = new byte[Integer.BYTES];
    private RandomAccessFile aviFile;
    private long moviListSizeOffset;
    private long moviIndexBaseOffset;
    private long totalFramesFieldOffset;
    private long streamLengthFieldOffset;
    private final LongArrayList frameOffsets = new LongArrayList();
    private final IntArrayList frameSizes = new IntArrayList();

    private BufferedImage mp4FrameImage;
    private int[] mp4FramePixels;
    private AWTSequenceEncoder mp4Encoder;

    private Future<?> pendingAviFinalize;
    private Future<?> pendingMp4Finalize;

    // Internal \\

    @Override
    protected void create() {
        this.recordingDirectory = ScreenCaptureIOUtility.resolveCaptureDirectory(
                internal.path, EngineSetting.RECORDING_OUTPUT_DIRECTORY);
        this.aviWriteThread = getThreadHandleFromThreadName(EngineSetting.VIDEO_WRITE_THREAD_NAME);
        this.videoEncodeThread = getThreadHandleFromThreadName(EngineSetting.VIDEO_ENCODE_THREAD_NAME);
        this.captureIntervalNanos = EngineSetting.NANOS_PER_SECOND / EngineSetting.RECORDING_FRAME_RATE;
    }

    @Override
    protected void get() {
        this.pboManager = get(PboManager.class);
    }

    // Draw Authority \\

    void flush() {

        if (!recording)
            return;

        captureFrame();
    }

    // Toggle \\

    void toggleRecording(WindowInstance window) {
        if (recording)
            stopRecording();
        else
            startRecording(window);
    }

    // Session \\

    private void startRecording(WindowInstance window) {

        if (isFinalizing())
            throwException("Cannot start a new recording while the previous recording is still finalizing.");

        this.activeWindow = window;
        this.width = window.getWidth();
        this.height = window.getHeight();
        this.frameCount = 0;
        this.activeBufferIndex = 0;
        this.recordingStartNanos = System.nanoTime();
        this.fallingBehind = false;
        this.frameOffsets.clear();
        this.frameSizes.clear();

        ensureFrameBuffers();

        if (pboInstance == null)
            pboInstance = pboManager.createPbo(window, width, height);

        String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
        String baseName = EngineSetting.RECORDING_FILE_PREFIX + timestamp;

        openAviFile(new File(recordingDirectory, baseName + "." + EngineSetting.RECORDING_LOSSLESS_EXTENSION));
        openMp4Encoder(new File(recordingDirectory, baseName + "." + EngineSetting.RECORDING_STANDARD_EXTENSION));

        this.recording = true;
    }

    private void stopRecording() {

        this.recording = false;

        drainPendingCapture();

        this.pendingAviFinalize = executeAsync(aviWriteThread, this::finalizeAvi);
        this.pendingMp4Finalize = executeAsync(videoEncodeThread, this::finalizeMp4);

        this.activeWindow = null;
    }

    private void finalizeAvi() {
        closeAviFile();
    }

    private void finalizeMp4() {
        closeMp4Encoder();
    }

    private boolean isFinalizing() {
        return (pendingAviFinalize != null && !pendingAviFinalize.isDone())
                || (pendingMp4Finalize != null && !pendingMp4Finalize.isDone());
    }

    // Shutdown \\

    @Override
    protected void dispose() {

        if (recording)
            stopRecording();

        awaitFinalize(pendingAviFinalize);
        awaitFinalize(pendingMp4Finalize);
    }

    private void awaitFinalize(Future<?> future) {

        if (future == null)
            return;

        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throwException("Recording finalization failed during shutdown", e.getCause());
        }
    }

    // Capture \\

    private void captureFrame() {

        if (isBufferBusy(activeBufferIndex))
            return;

        long expectedFrameCount = (System.nanoTime() - recordingStartNanos) / captureIntervalNanos;
        long due = expectedFrameCount - frameCount;

        if (due <= 0)
            return;

        updateBacklogState(due);

        int repeatCount = (int) Math.min(due, EngineSetting.RECORDING_MAX_CATCHUP_FRAMES);

        ByteBuffer buffer = frameBuffers[activeBufferIndex];

        internal.windowPlatform.makeContextCurrent(activeWindow.getGLWindow());
        boolean hasFrame = pboInstance.capture(activeWindow, width, height, buffer);
        internal.windowPlatform.restoreMainContext();

        if (!hasFrame)
            return;

        dispatchWrites(activeBufferIndex, buffer, repeatCount);

        frameCount += repeatCount;
        activeBufferIndex = (activeBufferIndex + 1) % frameBuffers.length;
    }

    private void updateBacklogState(long due) {
        boolean behind = due > EngineSetting.RECORDING_MAX_CATCHUP_FRAMES;
        if (behind && !fallingBehind)
            debug("Video recording cannot sustain the target frame rate — frames are being dropped to stay in sync with real time.");
        fallingBehind = behind;
    }

    private void drainPendingCapture() {

        if (isBufferBusy(activeBufferIndex))
            return;

        long expectedFrameCount = (System.nanoTime() - recordingStartNanos) / captureIntervalNanos;
        long due = Math.max(1L, expectedFrameCount - frameCount);
        int repeatCount = (int) Math.min(due, EngineSetting.RECORDING_MAX_CATCHUP_FRAMES);

        ByteBuffer buffer = frameBuffers[activeBufferIndex];

        internal.windowPlatform.makeContextCurrent(activeWindow.getGLWindow());
        boolean hasFrame = pboInstance.tryRetrieve(buffer);
        internal.windowPlatform.restoreMainContext();

        if (!hasFrame)
            return;

        dispatchWrites(activeBufferIndex, buffer, repeatCount);
        frameCount += repeatCount;
    }

    private void dispatchWrites(int bufferIndex, ByteBuffer buffer, int repeatCount) {
        pendingAviWrites[bufferIndex] = executeAsync(aviWriteThread, () -> writeAviFrames(buffer, repeatCount));
        pendingMp4Writes[bufferIndex] = executeAsync(videoEncodeThread, () -> writeMp4Frames(buffer, repeatCount));
    }

    private boolean isBufferBusy(int index) {
        Future<?> avi = pendingAviWrites[index];
        Future<?> mp4 = pendingMp4Writes[index];
        return (avi != null && !avi.isDone()) || (mp4 != null && !mp4.isDone());
    }

    private void writeAviFrames(ByteBuffer buffer, int repeatCount) {
        for (int i = 0; i < repeatCount; i++) {
            buffer.rewind();
            writeAviFrame(buffer);
        }
    }

    private void writeMp4Frames(ByteBuffer buffer, int repeatCount) {
        convertToMp4Pixels(buffer);
        for (int i = 0; i < repeatCount; i++)
            encodeMp4Frame();
    }

    private void ensureFrameBuffers() {

        int requiredCapacity = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;

        if (frameBuffers[0] != null && frameBuffers[0].capacity() == requiredCapacity)
            return;

        for (int i = 0; i < frameBuffers.length; i++)
            frameBuffers[i] = ByteBuffer.allocateDirect(requiredCapacity);

        this.mp4FrameImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.mp4FramePixels = ((DataBufferInt) mp4FrameImage.getRaster().getDataBuffer()).getData();
    }

    // AVI Container \\

    private void openAviFile(File file) {

        try {

            this.aviFile = new RandomAccessFile(file, "rw");
            aviFile.setLength(0);

            writeFourCC("RIFF");
            writeIntLE(0);
            writeFourCC("AVI ");

            writeFourCC("LIST");
            long hdrlSizeOffset = aviFile.getFilePointer();
            writeIntLE(0);
            writeFourCC("hdrl");

            writeFourCC("avih");
            writeIntLE(AVI_MAIN_HEADER_LENGTH);
            writeMainHeader();

            writeFourCC("LIST");
            long strlSizeOffset = aviFile.getFilePointer();
            writeIntLE(0);
            writeFourCC("strl");

            writeFourCC("strh");
            writeIntLE(AVI_STREAM_HEADER_LENGTH);
            writeStreamHeader();

            writeFourCC("strf");
            writeIntLE(AVI_STREAM_FORMAT_LENGTH);
            writeStreamFormat();

            patchListSize(strlSizeOffset);
            patchListSize(hdrlSizeOffset);

            writeFourCC("LIST");
            this.moviListSizeOffset = aviFile.getFilePointer();
            writeIntLE(0);
            this.moviIndexBaseOffset = aviFile.getFilePointer();
            writeFourCC("movi");

        } catch (IOException e) {
            throwException("Failed to open lossless recording file: " + file.getAbsolutePath(), e);
        }
    }

    private void writeMainHeader() throws IOException {

        int microSecondsPerFrame = 1_000_000 / EngineSetting.RECORDING_FRAME_RATE;
        int bytesPerFrame = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;

        writeIntLE(microSecondsPerFrame);
        writeIntLE(bytesPerFrame * EngineSetting.RECORDING_FRAME_RATE);
        writeIntLE(0);
        writeIntLE(EngineSetting.AVI_FLAG_HAS_INDEX);
        this.totalFramesFieldOffset = aviFile.getFilePointer();
        writeIntLE(0);
        writeIntLE(0);
        writeIntLE(1);
        writeIntLE(bytesPerFrame);
        writeIntLE(width);
        writeIntLE(height);
        writeIntLE(0);
        writeIntLE(0);
        writeIntLE(0);
        writeIntLE(0);
    }

    private void writeStreamHeader() throws IOException {

        writeFourCC("vids");
        writeFourCC(EngineSetting.AVI_UNCOMPRESSED_FOURCC);
        writeIntLE(0);
        writeShortLE((short) 0);
        writeShortLE((short) 0);
        writeIntLE(0);
        writeIntLE(1);
        writeIntLE(EngineSetting.RECORDING_FRAME_RATE);
        writeIntLE(0);
        this.streamLengthFieldOffset = aviFile.getFilePointer();
        writeIntLE(0);
        writeIntLE(width * height * EngineSetting.BYTES_PER_PIXEL_BGRA);
        writeIntLE(-1);
        writeIntLE(0);
        writeShortLE((short) 0);
        writeShortLE((short) 0);
        writeShortLE((short) width);
        writeShortLE((short) height);
    }

    private void writeStreamFormat() throws IOException {

        writeIntLE(AVI_STREAM_FORMAT_LENGTH);
        writeIntLE(width);
        writeIntLE(height);
        writeShortLE((short) 1);
        writeShortLE((short) (EngineSetting.BYTES_PER_PIXEL_BGRA * 8));
        writeIntLE(0);
        writeIntLE(width * height * EngineSetting.BYTES_PER_PIXEL_BGRA);
        writeIntLE(0);
        writeIntLE(0);
        writeIntLE(0);
        writeIntLE(0);
    }

    private void patchListSize(long sizeFieldOffset) throws IOException {

        long currentPosition = aviFile.getFilePointer();

        aviFile.seek(sizeFieldOffset);
        writeIntLE((int) (currentPosition - sizeFieldOffset - Integer.BYTES));
        aviFile.seek(currentPosition);
    }

    private void writeAviFrame(ByteBuffer buffer) {

        try {

            long chunkStart = aviFile.getFilePointer();
            int frameBytes = buffer.remaining();

            writeFourCC("00db");
            writeIntLE(frameBytes);
            aviFile.getChannel().write(buffer);

            if ((frameBytes & 1) != 0)
                aviFile.write(0);

            frameOffsets.add(chunkStart - moviIndexBaseOffset);
            frameSizes.add(frameBytes);

        } catch (IOException e) {
            throwException("Failed to write recorded frame to lossless file", e);
        }
    }

    private void writeIndex() throws IOException {

        writeFourCC("idx1");
        writeIntLE(frameOffsets.size() * AVI_INDEX_ENTRY_LENGTH);

        for (int i = 0; i < frameOffsets.size(); i++) {
            writeFourCC("00db");
            writeIntLE(EngineSetting.AVI_INDEX_FLAG_KEYFRAME);
            writeIntLE((int) frameOffsets.getLong(i));
            writeIntLE(frameSizes.getInt(i));
        }
    }

    private void closeAviFile() {

        try {

            long moviListEndOffset = aviFile.getFilePointer();

            writeIndex();

            long fileEndOffset = aviFile.getFilePointer();

            aviFile.seek(4);
            writeIntLE((int) (fileEndOffset - RIFF_CHUNK_HEADER_LENGTH));

            aviFile.seek(moviListSizeOffset);
            writeIntLE((int) (moviListEndOffset - moviListSizeOffset - Integer.BYTES));

            aviFile.seek(totalFramesFieldOffset);
            writeIntLE((int) frameCount);

            aviFile.seek(streamLengthFieldOffset);
            writeIntLE((int) frameCount);

            aviFile.close();

        } catch (IOException e) {
            throwException("Failed to finalize lossless recording file", e);
        } finally {
            this.aviFile = null;
        }
    }

    private void writeFourCC(String tag) throws IOException {
        aviFile.write(tag.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeIntLE(int value) throws IOException {
        aviScratch[0] = (byte) value;
        aviScratch[1] = (byte) (value >>> 8);
        aviScratch[2] = (byte) (value >>> 16);
        aviScratch[3] = (byte) (value >>> 24);
        aviFile.write(aviScratch, 0, Integer.BYTES);
    }

    private void writeShortLE(short value) throws IOException {
        aviScratch[0] = (byte) value;
        aviScratch[1] = (byte) (value >>> 8);
        aviFile.write(aviScratch, 0, Short.BYTES);
    }

    // MP4 Encoder \\

    private void openMp4Encoder(File outputFile) {
        try {
            this.mp4Encoder = AWTSequenceEncoder.createSequenceEncoder(
                    outputFile, EngineSetting.RECORDING_FRAME_RATE);
        } catch (IOException e) {
            throwException("Failed to open standard recording encoder: " + outputFile.getAbsolutePath(), e);
        }
    }

    private void convertToMp4Pixels(ByteBuffer buffer) {

        int stride = width * EngineSetting.BYTES_PER_PIXEL_BGRA;

        for (int row = 0; row < height; row++) {

            int rowStart = row * stride;
            int destinationRow = (height - 1 - row) * width;

            for (int col = 0; col < width; col++) {

                int index = rowStart + col * EngineSetting.BYTES_PER_PIXEL_BGRA;

                int b = buffer.get(index) & 0xFF;
                int g = buffer.get(index + 1) & 0xFF;
                int r = buffer.get(index + 2) & 0xFF;

                mp4FramePixels[destinationRow + col] = (r << 16) | (g << 8) | b;
            }
        }
    }

    private void encodeMp4Frame() {
        try {
            mp4Encoder.encodeImage(mp4FrameImage);
        } catch (IOException e) {
            throwException("Failed to encode standard recording frame", e);
        }
    }

    private void closeMp4Encoder() {
        try {
            mp4Encoder.finish();
        } catch (IOException e) {
            throwException("Failed to finalize standard recording file", e);
        } finally {
            this.mp4Encoder = null;
        }
    }
}