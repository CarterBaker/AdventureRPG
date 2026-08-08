package engine.assets.image;

import javax.imageio.ImageIO;

import engine.graphics.color.Color;
import engine.root.EngineUtility;

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
            EngineUtility.throwException("Failed loading image: " + file.getPath(), e);
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

    // Read \\

    public int getPixel(int x, int y) {
        int idx = (y * width + x) * 4;
        int r = pixels.get(idx) & 0xFF;
        int g = pixels.get(idx + 1) & 0xFF;
        int b = pixels.get(idx + 2) & 0xFF;
        int a = pixels.get(idx + 3) & 0xFF;
        return (r << 24) | (g << 16) | (b << 8) | a;
    }

    /*
     * This pixel repacked as 0xRRGGBB with alpha discarded — the exact
     * layout every "map_color" hex value in biome JSON parses into.
     * Anything matching a sampled pixel against an authored map color
     * must go through this rather than getPixel(), whose RGBA8888 layout
     * puts red 8 bits higher and would silently corrupt every comparison.
     */
    public int getPixelRGB(int x, int y) {
        return getPixel(x, y) >>> 8;
    }

    public Color getPixelColor(int x, int y) {
        int rgba = getPixel(x, y);
        float r = ((rgba >> 24) & 0xFF) / 255.0f;
        float g = ((rgba >> 16) & 0xFF) / 255.0f;
        float b = ((rgba >> 8) & 0xFF) / 255.0f;
        float a = (rgba & 0xFF) / 255.0f;
        return new Color(r, g, b, a);
    }

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