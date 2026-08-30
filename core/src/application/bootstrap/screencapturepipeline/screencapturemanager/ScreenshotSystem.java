package application.bootstrap.screencapturepipeline.screencapturemanager;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import application.bootstrap.renderpipeline.pbo.PboInstance;
import application.bootstrap.renderpipeline.pbomanager.PboManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class ScreenshotSystem extends SystemPackage {

    /*
     * Captures a single still frame from a window's front buffer on demand.
     * capture() only records intent — it never touches GL, so it is safe to
     * call from any phase of any frame — and is ignored while a capture is
     * already in flight or its previous write hasn't finished, matching a
     * physical shutter: one press, one photo. All GPU work happens inside
     * flush(), called exactly once per frame by ScreenCaptureManager from
     * the engine's own draw() authority: one call queues the GPU readback
     * through a PboInstance, created once and reused for every future
     * screenshot, and a later call polls it once the queued read is ready
     * (by construction, at least one frame later) before dispatching the
     * TGA and PNG writes to the single-threaded ScreenCapture pool — every
     * step is a non-blocking poll, so a slow disk can never stall a frame.
     * The pixel buffer and PNG image are allocated once and only
     * reallocated if the target window resizes.
     */

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern(EngineSetting.CAPTURE_TIMESTAMP_PATTERN);

    private File screenshotDirectory;
    private ThreadHandle encodingThread;
    private PboManager pboManager;
    private PboInstance pboInstance;
    private Future<?> pendingWrite;

    private ByteBuffer pixelBuffer;
    private BufferedImage pngImage;
    private int[] pngPixels;
    private int bufferedWidth;
    private int bufferedHeight;

    private boolean captureRequested;
    private boolean awaitingReadback;
    private WindowInstance requestedWindow;
    private int requestedWidth;
    private int requestedHeight;

    // Internal \\

    @Override
    protected void create() {
        this.screenshotDirectory = ScreenCaptureIOUtility.resolveCaptureDirectory(
                internal.path, EngineSetting.SCREENSHOT_OUTPUT_DIRECTORY);
        this.encodingThread = getThreadHandleFromThreadName(EngineSetting.SCREEN_CAPTURE_THREAD_NAME);
    }

    @Override
    protected void get() {
        this.pboManager = get(PboManager.class);
    }

    // Capture Request \\

    void capture(WindowInstance window) {

        if (captureRequested || awaitingReadback)
            return;

        if (pendingWrite != null && !pendingWrite.isDone())
            return;

        this.requestedWindow = window;
        this.captureRequested = true;
    }

    // Draw Authority \\

    void flush() {

        if (awaitingReadback) {
            retrieveReadback();
            return;
        }

        if (captureRequested)
            queueReadback();
    }

    private void queueReadback() {

        captureRequested = false;

        this.requestedWidth = requestedWindow.getWidth();
        this.requestedHeight = requestedWindow.getHeight();

        ensureBuffers(requestedWidth, requestedHeight);

        if (pboInstance == null)
            pboInstance = pboManager.createPbo(requestedWindow, requestedWidth, requestedHeight);

        internal.windowPlatform.makeContextCurrent(requestedWindow.getGLWindow());
        pboInstance.queue(requestedWindow, requestedWidth, requestedHeight);
        internal.windowPlatform.restoreMainContext();

        this.awaitingReadback = true;
    }

    private void retrieveReadback() {

        internal.windowPlatform.makeContextCurrent(requestedWindow.getGLWindow());
        boolean hasFrame = pboInstance.tryRetrieve(pixelBuffer);
        internal.windowPlatform.restoreMainContext();

        if (!hasFrame)
            return;

        this.awaitingReadback = false;
        dispatchWrite(requestedWidth, requestedHeight);
    }

    private void dispatchWrite(int width, int height) {

        String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
        String baseName = EngineSetting.SCREENSHOT_FILE_PREFIX + timestamp;

        File tgaFile = new File(screenshotDirectory, baseName + "." + EngineSetting.SCREENSHOT_LOSSLESS_EXTENSION);
        File pngFile = new File(screenshotDirectory, baseName + "." + EngineSetting.SCREENSHOT_STANDARD_FORMAT);

        pendingWrite = executeAsync(encodingThread, () -> writeCapturedFrame(tgaFile, pngFile, width, height));
    }

    private void writeCapturedFrame(File tgaFile, File pngFile, int width, int height) {
        writeTga(tgaFile, width, height);
        writePng(pngFile, width, height);
    }

    // Buffers \\

    private void ensureBuffers(int width, int height) {

        if (pixelBuffer != null && bufferedWidth == width && bufferedHeight == height)
            return;

        this.pixelBuffer = ByteBuffer.allocateDirect(width * height * EngineSetting.BYTES_PER_PIXEL_BGRA);
        this.pngImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.pngPixels = ((DataBufferInt) pngImage.getRaster().getDataBuffer()).getData();
        this.bufferedWidth = width;
        this.bufferedHeight = height;
    }

    // TGA \\

    private void writeTga(File file, int width, int height) {

        try (FileOutputStream stream = new FileOutputStream(file)) {

            byte[] header = new byte[EngineSetting.TGA_HEADER_LENGTH_BYTES];
            header[2] = (byte) EngineSetting.TGA_IMAGE_TYPE_UNCOMPRESSED_TRUECOLOR;
            header[12] = (byte) width;
            header[13] = (byte) (width >>> 8);
            header[14] = (byte) height;
            header[15] = (byte) (height >>> 8);
            header[16] = (byte) EngineSetting.TGA_PIXEL_DEPTH_BITS;
            header[17] = (byte) EngineSetting.TGA_IMAGE_DESCRIPTOR_BOTTOM_LEFT_ALPHA;

            stream.write(header);

            pixelBuffer.rewind();
            stream.getChannel().write(pixelBuffer);

        } catch (IOException e) {
            throwException("Failed to write screenshot TGA file: " + file.getAbsolutePath(), e);
        }
    }

    // PNG \\

    private void writePng(File file, int width, int height) {

        int stride = width * EngineSetting.BYTES_PER_PIXEL_BGRA;

        for (int row = 0; row < height; row++) {

            int rowStart = row * stride;
            int destinationRow = (height - 1 - row) * width;

            for (int col = 0; col < width; col++) {

                int index = rowStart + col * EngineSetting.BYTES_PER_PIXEL_BGRA;

                int b = pixelBuffer.get(index) & 0xFF;
                int g = pixelBuffer.get(index + 1) & 0xFF;
                int r = pixelBuffer.get(index + 2) & 0xFF;
                int a = pixelBuffer.get(index + 3) & 0xFF;

                pngPixels[destinationRow + col] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        try {
            ImageIO.write(pngImage, EngineSetting.SCREENSHOT_STANDARD_FORMAT, file);
        } catch (IOException e) {
            throwException("Failed to write screenshot PNG file: " + file.getAbsolutePath(), e);
        }
    }
}