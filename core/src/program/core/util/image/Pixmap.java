package program.core.util.image;

import javax.imageio.ImageIO;

import program.core.engine.UtilityPackage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Pixmap {

    /*
     * CPU-side RGBA8888 pixel buffer. Loaded from file or allocated blank.
     * Owns a direct ByteBuffer ready for GPU upload. Dispose is a no-op —
     * GC handles the direct buffer at session end.
     */

    public enum Format {
        RGBA8888
    }

    // Dimensions
    private final int width;
    private final int height;

    // Pixels
    private final ByteBuffer pixels;

    // Constructors \\

    public Pixmap(int width, int height, Format format) {
        this.width = width;
        this.height = height;
        this.pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
    }

    public Pixmap(File file) {

        BufferedImage image;

        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            UtilityPackage.throwException("Failed loading image: " + file.getPath(), e);
            throw new AssertionError();
        }

        this.width = image.getWidth();
        this.height = image.getHeight();
        this.pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());

        int[] argb = new int[width * height];
        image.getRGB(0, 0, width, height, argb, 0, width);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = argb[y * width + x];
                int idx = (y * width + x) * 4;
                pixels.put(idx, (byte) ((c >> 16) & 0xFF));
                pixels.put(idx + 1, (byte) ((c >> 8) & 0xFF));
                pixels.put(idx + 2, (byte) (c & 0xFF));
                pixels.put(idx + 3, (byte) ((c >> 24) & 0xFF));
            }
        }
    }

    // Draw \\

    public void drawPixel(int x, int y, int rgba8888) {
        int idx = (y * width + x) * 4;
        pixels.put(idx, (byte) ((rgba8888 >> 24) & 0xFF));
        pixels.put(idx + 1, (byte) ((rgba8888 >> 16) & 0xFF));
        pixels.put(idx + 2, (byte) ((rgba8888 >> 8) & 0xFF));
        pixels.put(idx + 3, (byte) (rgba8888 & 0xFF));
    }

    // Accessible \\

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ByteBuffer getPixels() {
        return pixels;
    }

    public void dispose() {
    }
}