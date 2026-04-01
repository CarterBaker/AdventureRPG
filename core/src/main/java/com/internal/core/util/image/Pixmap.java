package com.internal.core.util.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Pixmap {
    public enum Format { RGBA8888 }

    private final int width;
    private final int height;
    private final ByteBuffer pixels;

    public Pixmap(int width, int height, Format format) {
        this.width = width;
        this.height = height;
        this.pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
    }

    public Pixmap(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
            int[] argb = new int[width * height];
            image.getRGB(0, 0, width, height, argb, 0, width);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int c = argb[y * width + x];
                    int a = (c >> 24) & 0xFF;
                    int r = (c >> 16) & 0xFF;
                    int g = (c >> 8) & 0xFF;
                    int b = c & 0xFF;
                    int idx = (y * width + x) * 4;
                    pixels.put(idx, (byte) r);
                    pixels.put(idx + 1, (byte) g);
                    pixels.put(idx + 2, (byte) b);
                    pixels.put(idx + 3, (byte) a);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed loading image: " + file.getPath(), e);
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public ByteBuffer getPixels() { return pixels; }
    public void dispose() {}

    public void drawPixel(int x, int y, int rgba8888) {
        int idx = (y * width + x) * 4;
        pixels.put(idx, (byte) ((rgba8888 >> 24) & 0xFF));
        pixels.put(idx + 1, (byte) ((rgba8888 >> 16) & 0xFF));
        pixels.put(idx + 2, (byte) ((rgba8888 >> 8) & 0xFF));
        pixels.put(idx + 3, (byte) (rgba8888 & 0xFF));
    }
}
