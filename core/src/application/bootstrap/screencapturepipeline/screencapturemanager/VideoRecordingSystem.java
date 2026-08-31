package application.bootstrap.screencapturepipeline.screencapturemanager;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
     * Owns a single toggled recording session. Captures a lossless AVI track
     * in real time from the shared GPU readback — the same proven pipeline
     * ScreenshotSystem relies on — and, once a session stops, hands the
     * finished file to a background job that copies it and converts that
     * copy into a standard MP4 on the video encode thread, so the main
     * thread and the live capture path never touch MP4 encoding at all.
     * Starting a new recording is refused while either the AVI finalize or
     * the MP4 conversion of the previous session is still in flight.
     */

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern(EngineSetting.CAPTURE_TIMESTAMP_PATTERN);

    private File recordingDirectory;
    private ThreadHandle aviWriteThread;
    private ThreadHandle videoEncodeThread;
    private PboManager pboManager;
    private PboInstance pboInstance;

    private boolean recording;
    private String activeBaseName;
    private WindowInstance activeWindow;
    private File activeAviFile;
    private File activeMp4File;
    private int width;
    private int height;

    // Reference Clock
    private long recordingStartNanos;
    private long masterCaptureIntervalNanos;
    private long lastCaptureTick;
    private ByteBuffer captureScratchBuffer;

    // Lossless Track
    private boolean aviActive;
    private long aviCaptureIntervalNanos;
    private final ByteBuffer[] aviFrameBuffers = new ByteBuffer[EngineSetting.RECORDING_LOSSLESS_BUFFER_COUNT];
    private final Future<?>[] pendingAviWrites = new Future<?>[EngineSetting.RECORDING_LOSSLESS_BUFFER_COUNT];
    private int aviActiveBufferIndex;
    private int aviPendingBufferIndex;
    private long aviLastAccountedTick;
    private boolean aviStalled;
    private volatile boolean aviHealthy = true;
    private volatile String aviBackgroundFailureReason;

    private final byte[] aviScratch = new byte[Integer.BYTES];
    private RandomAccessFile aviFile;
    private long moviListSizeOffset;
    private long moviIndexBaseOffset;
    private long totalFramesFieldOffset;
    private long streamLengthFieldOffset;
    private final LongArrayList frameOffsets = new LongArrayList();
    private final IntArrayList frameSizes = new IntArrayList();

    // Standard Format Conversion
    private Future<?> pendingAviFinalize;
    private Future<?> pendingMp4Conversion;
    private ByteBuffer mp4ConversionReadBuffer;
    private byte[] mp4ConversionScratch;
    private int mp4EncodeWidth;
    private int mp4EncodeHeight;
    private int mp4SourceWidthCached = EngineSetting.INDEX_NOT_FOUND;
    private int mp4SourceHeightCached = EngineSetting.INDEX_NOT_FOUND;
    private int[] mp4ColumnLookup;
    private int[] mp4RowLookup;
    private BufferedImage mp4FrameImage;
    private int[] mp4FramePixels;

    // Internal \\

    @Override
    protected void create() {

        if (EngineSetting.RECORDING_LOSSLESS_FRAME_RATE % EngineSetting.RECORDING_STANDARD_FRAME_RATE != 0)
            throwException(
                    "RECORDING_LOSSLESS_FRAME_RATE must be an exact multiple of RECORDING_STANDARD_FRAME_RATE — "
                            + "the mp4 conversion step samples the lossless track by simple decimation.");

        this.recordingDirectory = ScreenCaptureIOUtility.resolveCaptureDirectory(
                internal.path, EngineSetting.RECORDING_OUTPUT_DIRECTORY);
        this.aviWriteThread = getThreadHandleFromThreadName(EngineSetting.VIDEO_WRITE_THREAD_NAME);
        this.videoEncodeThread = getThreadHandleFromThreadName(EngineSetting.VIDEO_ENCODE_THREAD_NAME);
        this.masterCaptureIntervalNanos = EngineSetting.NANOS_PER_SECOND / EngineSetting.RECORDING_LOSSLESS_FRAME_RATE;
        this.aviCaptureIntervalNanos = masterCaptureIntervalNanos;
    }

    @Override
    protected void get() {
        this.pboManager = get(PboManager.class);
    }

    // Draw Authority \\

    void flush() {

        if (aviBackgroundFailureReason != null) {
            String reason = aviBackgroundFailureReason;
            aviBackgroundFailureReason = null;
            handleAviFailure(reason);
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

    // Session \\

    private void startRecording(WindowInstance window) {

        if (isFinalizing()) {
            errorLog("Video recording start request ignored — the previous recording is still finalizing.");
            return;
        }

        this.activeWindow = window;
        this.width = window.getWidth();
        this.height = window.getHeight();
        this.recordingStartNanos = System.nanoTime();
        this.lastCaptureTick = 0;

        this.aviActiveBufferIndex = 0;
        this.aviPendingBufferIndex = EngineSetting.INDEX_NOT_FOUND;
        this.aviLastAccountedTick = 0;
        this.aviStalled = false;
        this.aviHealthy = true;
        this.aviBackgroundFailureReason = null;
        this.frameOffsets.clear();
        this.frameSizes.clear();

        ensureCaptureScratchBuffer();
        ensureAviBuffers();

        if (pboInstance == null)
            pboInstance = pboManager.createPbo(window, width, height);

        String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
        String baseName = EngineSetting.RECORDING_FILE_PREFIX + timestamp;
        this.activeBaseName = baseName;

        this.activeAviFile = new File(recordingDirectory, baseName + "." + EngineSetting.RECORDING_LOSSLESS_EXTENSION);
        this.activeMp4File = new File(recordingDirectory, baseName + "." + EngineSetting.RECORDING_STANDARD_EXTENSION);

        this.aviActive = openAviFile(activeAviFile);

        if (!aviActive) {
            errorLog("Video recording failed to start — lossless output failed to open.");
            this.activeWindow = null;
            return;
        }

        this.recording = true;
        timeStampLog("Started video recording: " + baseName);
    }

    private void stopRecording() {

        this.recording = false;

        timeStampLog("Stopped video recording: " + activeBaseName);

        finalizePendingAviFrame();
        pendingAviFinalize = executeAsync(aviWriteThread, this::finalizeAvi);
        this.aviActive = false;

        log("Dispatched lossless finalize for: " + activeBaseName);

        Future<?> aviFinalizeHandle = pendingAviFinalize;
        File finishedAviFile = this.activeAviFile;
        File finishedMp4File = this.activeMp4File;
        int finishedWidth = this.width;
        int finishedHeight = this.height;

        pendingMp4Conversion = executeAsync(videoEncodeThread, () -> convertRecordingToMp4(
                aviFinalizeHandle, finishedAviFile, finishedMp4File, finishedWidth, finishedHeight));

        log("Dispatched standard format conversion for: " + activeBaseName);

        this.activeWindow = null;
    }

    private void finalizeAvi() {
        if (aviFile == null)
            return;
        closeAviFile();
        timeStampLog("Lossless recording finalized: " + activeAviFile.getName());
    }

    private boolean isFinalizing() {
        return (pendingAviFinalize != null && !pendingAviFinalize.isDone())
                || (pendingMp4Conversion != null && !pendingMp4Conversion.isDone());
    }

    private void handleAviFailure(String reason) {

        if (!aviActive)
            return;

        errorLog("Video recording \"" + activeBaseName + "\" stopped: " + reason);
        stopRecording();
    }

    private void signalAviFailure(String reason) {
        if (aviBackgroundFailureReason == null)
            aviBackgroundFailureReason = reason;
    }

    // Shutdown \\

    @Override
    protected void dispose() {

        if (recording)
            stopRecording();

        log("Waiting for pending video recording finalization before shutdown...");

        awaitFinalize(pendingAviFinalize);
        awaitFinalize(pendingMp4Conversion);
    }

    private void awaitFinalize(Future<?> future) {

        if (future == null)
            return;

        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            errorLog("Recording finalization failed during shutdown: " + e.getCause());
        }
    }

    // Capture \\

    private void captureFrame() {

        long elapsedNanos = System.nanoTime() - recordingStartNanos;
        long currentTick = elapsedNanos / masterCaptureIntervalNanos;

        if (currentTick <= lastCaptureTick)
            return;

        if (currentTick - lastCaptureTick > EngineSetting.RECORDING_CAPTURE_MAX_OWED_FRAMES) {
            errorLog("Video recording \"" + activeBaseName + "\" stopped: GPU capture fell more than "
                    + EngineSetting.RECORDING_CAPTURE_MAX_OWED_FRAMES + " ticks behind real time.");
            stopRecording();
            return;
        }

        internal.windowPlatform.makeContextCurrent(activeWindow.getGLWindow());
        boolean hasFrame = pboInstance.capture(activeWindow, width, height, captureScratchBuffer);
        internal.windowPlatform.restoreMainContext();

        if (!hasFrame)
            return;

        lastCaptureTick = currentTick;

        offerAviFrame(elapsedNanos);
    }

    private void offerAviFrame(long elapsedNanos) {

        long currentTick = elapsedNanos / aviCaptureIntervalNanos;

        if (currentTick <= aviLastAccountedTick)
            return;

        if (currentTick - aviLastAccountedTick > EngineSetting.RECORDING_LOSSLESS_MAX_OWED_FRAMES) {
            handleAviFailure("write pipeline fell more than " + EngineSetting.RECORDING_LOSSLESS_MAX_OWED_FRAMES
                    + " ticks behind real time and could not recover in time");
            return;
        }

        if (isAviBufferBusy(aviActiveBufferIndex))
            return;

        captureScratchBuffer.rewind();
        ByteBuffer slot = aviFrameBuffers[aviActiveBufferIndex];
        slot.clear();
        slot.put(captureScratchBuffer);
        slot.flip();

        if (aviPendingBufferIndex != EngineSetting.INDEX_NOT_FOUND) {
            int repeatCount = (int) (currentTick - aviLastAccountedTick);
            aviStalled = logTrackStall(aviStalled, repeatCount);
            dispatchAviWrite(aviPendingBufferIndex, aviFrameBuffers[aviPendingBufferIndex], repeatCount);
        }

        aviPendingBufferIndex = aviActiveBufferIndex;
        aviLastAccountedTick = currentTick;
        aviActiveBufferIndex = (aviActiveBufferIndex + 1) % aviFrameBuffers.length;
    }

    private void finalizePendingAviFrame() {

        if (aviPendingBufferIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        long elapsedNanos = System.nanoTime() - recordingStartNanos;
        long currentTick = elapsedNanos / aviCaptureIntervalNanos;
        int repeatCount = (int) Math.min(
                Math.max(1L, currentTick - aviLastAccountedTick),
                EngineSetting.RECORDING_LOSSLESS_MAX_OWED_FRAMES);

        dispatchAviWrite(aviPendingBufferIndex, aviFrameBuffers[aviPendingBufferIndex], repeatCount);

        aviPendingBufferIndex = EngineSetting.INDEX_NOT_FOUND;
    }

    private boolean logTrackStall(boolean currentlyStalled, int repeatCount) {

        boolean isStalled = repeatCount > EngineSetting.RECORDING_LOSSLESS_FRAME_RATE;

        if (isStalled && !currentlyStalled)
            log("Lossless recording pipeline is falling behind real time — holding the last captured frame longer than usual.");

        return isStalled;
    }

    // Dispatch \\

    private void dispatchAviWrite(int bufferIndex, ByteBuffer buffer, int repeatCount) {
        pendingAviWrites[bufferIndex] = executeAsync(aviWriteThread, () -> writeAviFrames(buffer, repeatCount));
    }

    private boolean isAviBufferBusy(int index) {
        Future<?> avi = pendingAviWrites[index];
        return avi != null && !avi.isDone();
    }

    private void writeAviFrames(ByteBuffer buffer, int repeatCount) {

        if (!aviHealthy)
            return;

        for (int i = 0; i < repeatCount; i++) {
            buffer.rewind();
            if (!writeAviFrame(buffer)) {
                aviHealthy = false;
                signalAviFailure("lossless recording write failed");
                return;
            }
        }
    }

    // Buffers \\

    private void ensureCaptureScratchBuffer() {

        int requiredCapacity = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;

        if (captureScratchBuffer != null && captureScratchBuffer.capacity() == requiredCapacity)
            return;

        this.captureScratchBuffer = ByteBuffer.allocateDirect(requiredCapacity);
    }

    private void ensureAviBuffers() {

        int requiredCapacity = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;

        if (aviFrameBuffers[0] != null && aviFrameBuffers[0].capacity() == requiredCapacity)
            return;

        for (int i = 0; i < aviFrameBuffers.length; i++)
            aviFrameBuffers[i] = ByteBuffer.allocateDirect(requiredCapacity);
    }

    // AVI Container \\

    private boolean openAviFile(File file) {
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
            writeIntLE(EngineSetting.AVI_MAIN_HEADER_LENGTH_BYTES);
            writeMainHeader();

            writeFourCC("LIST");
            long strlSizeOffset = aviFile.getFilePointer();
            writeIntLE(0);
            writeFourCC("strl");

            writeFourCC("strh");
            writeIntLE(EngineSetting.AVI_STREAM_HEADER_LENGTH_BYTES);
            writeStreamHeader();

            writeFourCC("strf");
            writeIntLE(EngineSetting.AVI_STREAM_FORMAT_LENGTH_BYTES);
            writeStreamFormat();

            patchListSize(strlSizeOffset);
            patchListSize(hdrlSizeOffset);

            writeFourCC("LIST");
            this.moviListSizeOffset = aviFile.getFilePointer();
            writeIntLE(0);
            this.moviIndexBaseOffset = aviFile.getFilePointer();
            writeFourCC("movi");

            log("Opened lossless recording file: " + file.getName());

            return true;

        } catch (IOException e) {
            errorLog("Failed to open lossless recording file: " + file.getAbsolutePath() + " — " + e.getMessage());
            closeQuietly(this.aviFile);
            this.aviFile = null;
            file.delete();
            return false;
        }
    }

    private void writeMainHeader() throws IOException {

        int microSecondsPerFrame = 1_000_000 / EngineSetting.RECORDING_LOSSLESS_FRAME_RATE;
        int bytesPerFrame = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;

        writeIntLE(microSecondsPerFrame);
        writeIntLE(bytesPerFrame * EngineSetting.RECORDING_LOSSLESS_FRAME_RATE);
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
        writeIntLE(EngineSetting.RECORDING_LOSSLESS_FRAME_RATE);
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

        writeIntLE(EngineSetting.AVI_STREAM_FORMAT_LENGTH_BYTES);
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

    private boolean writeAviFrame(ByteBuffer buffer) {
        try {

            long chunkStart = aviFile.getFilePointer();
            int frameBytes = buffer.remaining();

            writeFourCC("00db");
            writeIntLE(frameBytes);

            FileChannel channel = aviFile.getChannel();
            while (buffer.hasRemaining())
                channel.write(buffer);

            if ((frameBytes & 1) != 0)
                aviFile.write(0);

            frameOffsets.add(chunkStart - moviIndexBaseOffset);
            frameSizes.add(frameBytes);

            return true;

        } catch (IOException e) {
            errorLog("Failed to write recorded frame to lossless file: " + e.getMessage());
            return false;
        }
    }

    private void writeIndex() throws IOException {

        writeFourCC("idx1");
        writeIntLE(frameOffsets.size() * EngineSetting.AVI_INDEX_ENTRY_LENGTH_BYTES);

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
            writeIntLE((int) (fileEndOffset - EngineSetting.RIFF_CHUNK_HEADER_LENGTH_BYTES));

            aviFile.seek(moviListSizeOffset);
            writeIntLE((int) (moviListEndOffset - moviListSizeOffset - Integer.BYTES));

            int totalFrames = frameOffsets.size();

            aviFile.seek(totalFramesFieldOffset);
            writeIntLE(totalFrames);

            aviFile.seek(streamLengthFieldOffset);
            writeIntLE(totalFrames);

            aviFile.close();

        } catch (IOException e) {
            errorLog("Failed to finalize lossless recording file: " + e.getMessage());
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

    // Standard Format Conversion \\

    private void convertRecordingToMp4(
            Future<?> aviFinalizeHandle,
            File sourceAviFile,
            File targetMp4File,
            int sourceWidth,
            int sourceHeight) {

        try {
            aviFinalizeHandle.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException e) {
            errorLog("Standard recording conversion skipped — lossless file failed to finalize: " + e.getCause());
            return;
        }

        if (frameOffsets.isEmpty()) {
            errorLog("Standard recording conversion skipped — no frames were captured for: "
                    + sourceAviFile.getName());
            return;
        }

        File workingCopy = new File(recordingDirectory,
                sourceAviFile.getName() + EngineSetting.RECORDING_CONVERSION_TEMP_SUFFIX);

        timeStampLog("Copying lossless recording for standard format conversion: " + sourceAviFile.getName());

        try {
            Files.copy(sourceAviFile.toPath(), workingCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            errorLog("Standard recording conversion failed — could not copy lossless file: " + e.getMessage());
            return;
        }

        try {
            runMp4Conversion(workingCopy, targetMp4File, sourceWidth, sourceHeight);
        } finally {
            workingCopy.delete();
            log("Removed temporary conversion copy: " + workingCopy.getName());
        }
    }

    private void runMp4Conversion(File sourceCopy, File targetMp4File, int sourceWidth, int sourceHeight) {

        computeMp4EncodeDimensions(sourceWidth, sourceHeight);
        ensureMp4ConversionBuffers(sourceWidth, sourceHeight);

        AWTSequenceEncoder encoder = openMp4Encoder(targetMp4File);

        if (encoder == null)
            return;

        timeStampLog("Converting to standard format: " + targetMp4File.getName()
                + " (" + mp4EncodeWidth + "x" + mp4EncodeHeight + ")");

        boolean success = true;
        int frameCount = frameOffsets.size();
        int frameByteCount = sourceWidth * sourceHeight * EngineSetting.BYTES_PER_PIXEL_BGRA;

        try (RandomAccessFile reader = new RandomAccessFile(sourceCopy, "r")) {

            FileChannel channel = reader.getChannel();

            for (int i = 0; i < frameCount && success; i++) {

                if (i % EngineSetting.RECORDING_STANDARD_FRAME_SAMPLE_STRIDE != 0)
                    continue;

                int frameBytes = frameSizes.getInt(i);

                if (frameBytes != frameByteCount)
                    throwException("Corrupt lossless recording frame while converting to mp4 — expected "
                            + frameByteCount + " bytes but found " + frameBytes);

                long payloadOffset = moviIndexBaseOffset + frameOffsets.getLong(i)
                        + EngineSetting.AVI_FRAME_CHUNK_HEADER_LENGTH_BYTES;

                channel.position(payloadOffset);
                mp4ConversionReadBuffer.clear();

                while (mp4ConversionReadBuffer.hasRemaining()) {
                    if (channel.read(mp4ConversionReadBuffer) == -1)
                        throwException("Unexpected end of lossless recording file while converting to mp4.");
                }

                mp4ConversionReadBuffer.flip();
                convertToMp4Pixels(mp4ConversionReadBuffer, sourceWidth);

                try {
                    encoder.encodeImage(mp4FrameImage);
                } catch (IOException e) {
                    errorLog("Standard recording conversion failed while encoding frame: " + e.getMessage());
                    success = false;
                }
            }

        } catch (IOException e) {
            errorLog("Standard recording conversion failed — could not read lossless file: " + e.getMessage());
            success = false;
        }

        try {
            encoder.finish();
        } catch (IOException e) {
            errorLog("Standard recording conversion failed while finalizing: " + e.getMessage());
            success = false;
        }

        if (success)
            timeStampLog("Converted recording to standard format: " + targetMp4File.getName());
        else
            targetMp4File.delete();
    }

    private AWTSequenceEncoder openMp4Encoder(File outputFile) {
        try {
            return AWTSequenceEncoder.createSequenceEncoder(outputFile, EngineSetting.RECORDING_STANDARD_FRAME_RATE);
        } catch (IOException e) {
            errorLog("Standard recording conversion failed — could not open encoder: " + outputFile.getAbsolutePath()
                    + " — " + e.getMessage());
            outputFile.delete();
            return null;
        }
    }

    private void computeMp4EncodeDimensions(int sourceWidth, int sourceHeight) {

        if (sourceWidth == mp4SourceWidthCached && sourceHeight == mp4SourceHeightCached)
            return;

        int maxWidth = EngineSetting.RECORDING_STANDARD_MAX_WIDTH;

        int encodeWidth = sourceWidth <= maxWidth ? sourceWidth : maxWidth;
        int encodeHeight = sourceWidth <= maxWidth ? sourceHeight
                : Math.round(sourceHeight * ((float) maxWidth / sourceWidth));

        if ((encodeWidth & 1) != 0)
            encodeWidth--;
        if ((encodeHeight & 1) != 0)
            encodeHeight--;

        this.mp4EncodeWidth = encodeWidth;
        this.mp4EncodeHeight = encodeHeight;

        this.mp4ColumnLookup = new int[encodeWidth];
        for (int x = 0; x < encodeWidth; x++)
            mp4ColumnLookup[x] = Math.min(sourceWidth - 1, x * sourceWidth / encodeWidth);

        this.mp4RowLookup = new int[encodeHeight];
        for (int y = 0; y < encodeHeight; y++)
            mp4RowLookup[y] = Math.min(sourceHeight - 1, y * sourceHeight / encodeHeight);

        this.mp4FrameImage = new BufferedImage(encodeWidth, encodeHeight, BufferedImage.TYPE_INT_RGB);
        this.mp4FramePixels = ((DataBufferInt) mp4FrameImage.getRaster().getDataBuffer()).getData();

        this.mp4SourceWidthCached = sourceWidth;
        this.mp4SourceHeightCached = sourceHeight;
    }

    private void ensureMp4ConversionBuffers(int sourceWidth, int sourceHeight) {

        int requiredCapacity = sourceWidth * sourceHeight * EngineSetting.BYTES_PER_PIXEL_BGRA;

        if (mp4ConversionReadBuffer == null || mp4ConversionReadBuffer.capacity() != requiredCapacity)
            this.mp4ConversionReadBuffer = ByteBuffer.allocateDirect(requiredCapacity);

        if (mp4ConversionScratch == null || mp4ConversionScratch.length != requiredCapacity)
            this.mp4ConversionScratch = new byte[requiredCapacity];
    }

    private void convertToMp4Pixels(ByteBuffer buffer, int sourceWidth) {

        buffer.rewind();
        buffer.get(mp4ConversionScratch);

        int stride = sourceWidth * EngineSetting.BYTES_PER_PIXEL_BGRA;

        for (int destRow = 0; destRow < mp4EncodeHeight; destRow++) {

            int sourceRow = mp4RowLookup[destRow];
            int rowStart = sourceRow * stride;
            int destinationRow = (mp4EncodeHeight - 1 - destRow) * mp4EncodeWidth;

            for (int destCol = 0; destCol < mp4EncodeWidth; destCol++) {

                int sourceCol = mp4ColumnLookup[destCol];
                int index = rowStart + sourceCol * EngineSetting.BYTES_PER_PIXEL_BGRA;

                int b = mp4ConversionScratch[index] & 0xFF;
                int g = mp4ConversionScratch[index + 1] & 0xFF;
                int r = mp4ConversionScratch[index + 2] & 0xFF;

                mp4FramePixels[destinationRow + destCol] = (r << 16) | (g << 8) | b;
            }
        }
    }

    // Utility \\

    private void closeQuietly(Closeable closeable) {
        if (closeable == null)
            return;
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }
}