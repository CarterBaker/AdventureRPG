package application.bootstrap.screencapturepipeline.screencapturemanager;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class ScreenshotSystem extends SystemPackage {

    /*
     * Captures a single still frame from a window's front buffer on demand,
     * writing both a lossless TGA — a direct byte-for-byte dump of the raw
     * BGRA readback, since TGA's bottom-left origin matches OpenGL's
     * bottom-up row order with no conversion — and a standard PNG built
     * into a recycled ARGB BufferedImage. The pixel buffer and PNG image
     * are allocated once and only reallocated if the target window resizes.
     */

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern(EngineSetting.CAPTURE_TIMESTAMP_PATTERN);

    private File screenshotDirectory;

    private ByteBuffer pixelBuffer;
    private BufferedImage pngImage;
    private int[] pngPixels;
    private int bufferedWidth;
    private int bufferedHeight;

    // Internal \\

    @Override
    protected void create() {
        this.screenshotDirectory = ScreenCaptureIOUtility.resolveCaptureDirectory(
                internal.path, EngineSetting.SCREENSHOT_OUTPUT_DIRECTORY);
    }

    // Capture \\

    void capture(WindowInstance window) {

        int width = window.getWidth();
        int height = window.getHeight();

        ensureBuffers(width, height);

        internal.windowPlatform.makeContextCurrent(window.getGLWindow());
        pixelBuffer.clear();
        ScreenCaptureGLUtility.readFrontBuffer(width, height, pixelBuffer);
        internal.windowPlatform.restoreMainContext();

        // glReadPixels writes into the buffer's backing memory directly and
        // never advances its position. clear() already leaves position at 0
        // and limit at capacity — exactly the full frame — so the buffer is
        // immediately ready to read from. Flipping here would incorrectly
        // collapse limit back down to the untouched position of 0.

        String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
        String baseName = EngineSetting.SCREENSHOT_FILE_PREFIX + timestamp;

        writeTga(new File(screenshotDirectory, baseName + "." + EngineSetting.SCREENSHOT_LOSSLESS_EXTENSION),
                width, height);
        writePng(new File(screenshotDirectory, baseName + "." + EngineSetting.SCREENSHOT_STANDARD_FORMAT),
                width, height);
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