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
     * Owns a single recording session at a time, toggled on and off by
     * ScreenCaptureManager. update() only advances a nanosecond accumulator
     * against RECORDING_FRAME_RATE, entirely independent of the engine's
     * own frame rate, and banks how many frames are owed once the
     * accumulator clears a full interval, capped per call by
     * RECORDING_MAX_CATCHUP_FRAMES — pure bookkeeping, it never touches GL.
     * flush(), called exactly once per frame by ScreenCaptureManager from
     * the engine's own draw() authority, is the only place a frame is ever
     * pulled off the GPU. If the single-threaded ScreenCapture pool is
     * still busy encoding the buffer slot flush() needs, the capture is
     * skipped rather than blocked — the owed count carries forward, capped
     * by RECORDING_MAX_OWED_FRAMES, and is folded into the next successful
     * capture's repeat count so the written video still matches real
     * elapsed time without ever stalling the engine. Captures rotate
     * through a fixed pool of RECORDING_FRAME_BUFFER_COUNT pre-allocated
     * buffers rather than a single pair, giving the background encoder
     * several frames of slack before flush() would ever need to skip one.
     * Captured frames are handed to the ScreenCapture thread, which
     * appends them to an uncompressed AVI — the lossless track — and
     * encodes them into the standard MP4 with JCodec's pure-Java H.264
     * encoder, both driven off the same duplicated frame count so the two
     * outputs always agree in length.
     */

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern(EngineSetting.CAPTURE_TIMESTAMP_PATTERN);

    private static final int AVI_MAIN_HEADER_LENGTH = 56;
    private static final int AVI_STREAM_HEADER_LENGTH = 56;
    private static final int AVI_STREAM_FORMAT_LENGTH = 40;
    private static final int AVI_INDEX_ENTRY_LENGTH = 16;
    private static final int RIFF_CHUNK_HEADER_LENGTH = 8;

    private File recordingDirectory;
    private ThreadHandle encodingThread;
    private PboManager pboManager;
    private PboInstance pboInstance;

    private boolean recording;
    private WindowInstance activeWindow;
    private int width;
    private int height;
    private long frameCount;

    private final ByteBuffer[] frameBuffers = new ByteBuffer[EngineSetting.RECORDING_FRAME_BUFFER_COUNT];
    private final Future<?>[] pendingWrites = new Future<?>[EngineSetting.RECORDING_FRAME_BUFFER_COUNT];
    private int activeBufferIndex;

    // Pacing — decouples capture cadence from actual render cadence
    private long lastFrameNanos;
    private long accumulatedNanos;
    private long captureIntervalNanos;
    private int pendingCaptureFrames;
    private int owedFrames;

    private final byte[] aviScratch = new byte[Integer.BYTES];
    private RandomAccessFile aviFile;
    private long moviListSizeOffset;
    private long moviDataStartOffset;
    private long totalFramesFieldOffset;
    private long streamLengthFieldOffset;
    private final LongArrayList frameOffsets = new LongArrayList();
    private final IntArrayList frameSizes = new IntArrayList();

    private BufferedImage mp4FrameImage;
    private int[] mp4FramePixels;
    private AWTSequenceEncoder mp4Encoder;

    // Internal \\

    @Override
    protected void create() {
        this.recordingDirectory = ScreenCaptureIOUtility.resolveCaptureDirectory(
                internal.path, EngineSetting.RECORDING_OUTPUT_DIRECTORY);
        this.encodingThread = getThreadHandleFromThreadName(EngineSetting.SCREEN_CAPTURE_THREAD_NAME);
        this.captureIntervalNanos = EngineSetting.NANOS_PER_SECOND / EngineSetting.RECORDING_FRAME_RATE;
    }

    @Override
    protected void get() {
        this.pboManager = get(PboManager.class);
    }

    // Pacing \\

    @Override
    protected void update() {

        if (!recording)
            return;

        advanceCaptureClock();
    }

    private void advanceCaptureClock() {

        long now = System.nanoTime();
        accumulatedNanos += now - lastFrameNanos;
        lastFrameNanos = now;

        int framesDue = 0;

        while (accumulatedNanos >= captureIntervalNanos && framesDue < EngineSetting.RECORDING_MAX_CATCHUP_FRAMES) {
            accumulatedNanos -= captureIntervalNanos;
            framesDue++;
        }

        pendingCaptureFrames += framesDue;
    }

    // Draw Authority \\

    void flush() {

        if (!recording || pendingCaptureFrames <= 0)
            return;

        int framesToCapture = pendingCaptureFrames;
        pendingCaptureFrames = 0;

        captureFrame(framesToCapture);
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

        this.activeWindow = window;
        this.width = window.getWidth();
        this.height = window.getHeight();
        this.frameCount = 0;
        this.activeBufferIndex = 0;
        this.pendingCaptureFrames = 0;
        this.owedFrames = 0;
        this.frameOffsets.clear();
        this.frameSizes.clear();
        this.lastFrameNanos = System.nanoTime();
        this.accumulatedNanos = 0L;

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

        for (int i = 0; i < pendingWrites.length; i++)
            waitForPendingWrite(i);

        closeAviFile();
        closeMp4Encoder();

        this.activeWindow = null;
        this.accumulatedNanos = 0L;
        this.pendingCaptureFrames = 0;
        this.owedFrames = 0;
    }

    // Capture \\

    private void captureFrame(int repeatCount) {

        if (isWritePending(activeBufferIndex)) {
            owedFrames = Math.min(owedFrames + repeatCount, EngineSetting.RECORDING_MAX_OWED_FRAMES);
            return;
        }

        int totalRepeat = repeatCount + owedFrames;
        owedFrames = 0;

        ByteBuffer buffer = frameBuffers[activeBufferIndex];

        internal.windowPlatform.makeContextCurrent(activeWindow.getGLWindow());
        boolean hasFrame = pboInstance.capture(activeWindow, width, height, buffer);
        internal.windowPlatform.restoreMainContext();

        if (!hasFrame) {
            owedFrames = Math.min(totalRepeat, EngineSetting.RECORDING_MAX_OWED_FRAMES);
            return;
        }

        int bufferIndex = activeBufferIndex;
        pendingWrites[bufferIndex] = executeAsync(encodingThread, () -> writeFrameRepeated(buffer, totalRepeat));

        frameCount += totalRepeat;
        activeBufferIndex = (activeBufferIndex + 1) % frameBuffers.length;
    }

    private void drainPendingCapture() {

        waitForPendingWrite(activeBufferIndex);

        int totalRepeat = 1 + owedFrames;
        owedFrames = 0;

        ByteBuffer buffer = frameBuffers[activeBufferIndex];

        internal.windowPlatform.makeContextCurrent(activeWindow.getGLWindow());
        boolean hasFrame = pboInstance.tryRetrieve(buffer);
        internal.windowPlatform.restoreMainContext();

        if (!hasFrame)
            return;

        int bufferIndex = activeBufferIndex;
        pendingWrites[bufferIndex] = executeAsync(encodingThread, () -> writeFrameRepeated(buffer, totalRepeat));
        frameCount += totalRepeat;
    }

    private void writeFrameRepeated(ByteBuffer buffer, int repeatCount) {

        convertToMp4Pixels(buffer);

        for (int i = 0; i < repeatCount; i++) {
            buffer.rewind();
            writeAviFrame(buffer);
            encodeMp4Frame();
        }
    }

    private boolean isWritePending(int index) {
        Future<?> pending = pendingWrites[index];
        return pending != null && !pending.isDone();
    }

    private void waitForPendingWrite(int index) {

        Future<?> pending = pendingWrites[index];

        if (pending == null)
            return;

        try {
            pending.get();
        } catch (InterruptedException | ExecutionException e) {
            throwException("Video frame encode failed", e);
        }

        pendingWrites[index] = null;
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
            writeFourCC("movi");
            this.moviDataStartOffset = aviFile.getFilePointer();

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

            frameOffsets.add(chunkStart - moviDataStartOffset);
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