package application.bootstrap.screencapturepipeline.screencapturemanager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

class VideoRecordingSystem extends SystemPackage {

    /*
     * Owns a single recording session at a time, toggled on and off by
     * ScreenCaptureManager. Every engine frame while active, render() reads
     * the recording window's front buffer into one of two recycled BGRA
     * buffers and hands it to the ScreenCapture thread, which appends the
     * raw frame to an uncompressed AVI — the lossless, professional track —
     * and streams it into an ffmpeg process encoding the standard MP4 in
     * parallel. Alternating buffers lets the readback for frame N+1 proceed
     * while frame N is still being written to disk, relying on the
     * ScreenCapture thread being single-threaded so frames are never
     * reordered on either output.
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

    private boolean recording;
    private WindowInstance activeWindow;
    private int width;
    private int height;
    private long frameCount;

    private final ByteBuffer[] frameBuffers = new ByteBuffer[2];
    private final Future<?>[] pendingWrites = new Future<?>[2];
    private int activeBufferIndex;

    private final byte[] aviScratch = new byte[Integer.BYTES];
    private RandomAccessFile aviFile;
    private long moviListSizeOffset;
    private long moviDataStartOffset;
    private long totalFramesFieldOffset;
    private long streamLengthFieldOffset;
    private final LongArrayList frameOffsets = new LongArrayList();
    private final IntArrayList frameSizes = new IntArrayList();

    private Process encoderProcess;
    private OutputStream encoderStandardInput;
    private WritableByteChannel encoderChannel;

    // Internal \\

    @Override
    protected void create() {
        this.recordingDirectory = resolveDirectory(EngineSetting.RECORDING_OUTPUT_DIRECTORY);
        this.encodingThread = getThreadHandleFromThreadName(EngineSetting.SCREEN_CAPTURE_THREAD_NAME);
    }

    private File resolveDirectory(String subdirectoryName) {

        File directory = new File(EngineSetting.CAPTURE_ROOT_DIRECTORY, subdirectoryName);

        if (!directory.exists() && !directory.mkdirs())
            throwException("Failed to create capture output directory: " + directory.getAbsolutePath());

        return directory;
    }

    // Render \\

    @Override
    protected void render() {

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

        this.activeWindow = window;
        this.width = window.getWidth();
        this.height = window.getHeight();
        this.frameCount = 0;
        this.activeBufferIndex = 0;
        this.frameOffsets.clear();
        this.frameSizes.clear();

        ensureFrameBuffers();

        String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
        String baseName = EngineSetting.RECORDING_FILE_PREFIX + timestamp;

        openAviFile(new File(recordingDirectory, baseName + "." + EngineSetting.RECORDING_LOSSLESS_EXTENSION));
        openEncoderProcess(new File(recordingDirectory, baseName + "." + EngineSetting.RECORDING_STANDARD_EXTENSION));

        this.recording = true;
    }

    private void stopRecording() {

        this.recording = false;

        waitForPendingWrite(0);
        waitForPendingWrite(1);

        closeAviFile();
        closeEncoderProcess();

        this.activeWindow = null;
    }

    // Capture \\

    private void captureFrame() {

        waitForPendingWrite(activeBufferIndex);

        ByteBuffer buffer = frameBuffers[activeBufferIndex];
        buffer.clear();

        internal.windowPlatform.makeContextCurrent(activeWindow.getGLWindow());
        ScreenCaptureGLUtility.readFrontBuffer(width, height, buffer);
        internal.windowPlatform.restoreMainContext();

        buffer.flip();

        int bufferIndex = activeBufferIndex;
        pendingWrites[bufferIndex] = executeAsync(encodingThread, () -> writeFrame(buffer));

        frameCount++;
        activeBufferIndex = 1 - activeBufferIndex;
    }

    private void writeFrame(ByteBuffer buffer) {
        writeAviFrame(buffer);
        buffer.rewind();
        writeEncoderFrame(buffer);
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

        frameBuffers[0] = ByteBuffer.allocateDirect(requiredCapacity);
        frameBuffers[1] = ByteBuffer.allocateDirect(requiredCapacity);
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

    // Encoder Process \\

    private void openEncoderProcess(File outputFile) {

        try {

            ProcessBuilder builder = new ProcessBuilder(
                    EngineSetting.RECORDING_ENCODER_EXECUTABLE,
                    "-y",
                    "-f", "rawvideo",
                    "-pix_fmt", "bgra",
                    "-s", width + "x" + height,
                    "-r", String.valueOf(EngineSetting.RECORDING_FRAME_RATE),
                    "-i", "-",
                    "-vf", "vflip",
                    "-c:v", "libx264",
                    "-pix_fmt", "yuv420p",
                    outputFile.getAbsolutePath());

            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

            this.encoderProcess = builder.start();
            this.encoderStandardInput = encoderProcess.getOutputStream();
            this.encoderChannel = Channels.newChannel(encoderStandardInput);

        } catch (IOException e) {
            throwException("Failed to launch video encoder process", e);
        }
    }

    private void writeEncoderFrame(ByteBuffer buffer) {

        try {
            encoderChannel.write(buffer);
        } catch (IOException e) {
            throwException("Failed to stream frame to video encoder", e);
        }
    }

    private void closeEncoderProcess() {

        try {

            encoderChannel.close();
            encoderProcess.waitFor();

        } catch (IOException | InterruptedException e) {
            throwException("Failed to finalize video encoder process", e);
        } finally {
            this.encoderProcess = null;
            this.encoderChannel = null;
            this.encoderStandardInput = null;
        }
    }
}