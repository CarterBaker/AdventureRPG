package engine.assets.image;

import java.awt.image.BufferedImage;

import java.nio.ByteBuffer;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class PixmapUtility extends EngineUtility {

    /*
     * Converts a BufferedImage to a Pixmap with ARGB to RGBA repacking.
     * Optional vertical flip for GL coordinate system alignment. Single
     * entry point for all GPU upload paths — pixel format logic lives here,
     * as do the way back to a BufferedImage for saving, opaque whenever every
     * pixel is, and copying one Pixmap's pixels into another of its size.
     */

    // Conversion \\

    public static Pixmap fromBufferedImage(BufferedImage image, boolean flipVertical) {

        int w = image.getWidth();
        int h = image.getHeight();
        int[] argb = new int[w * h];
        image.getRGB(0, 0, w, h, argb, 0, w);

        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        for (int y = 0; y < h; y++) {

            int srcY = flipVertical ? (h - 1 - y) : y;

            for (int x = 0; x < w; x++) {
                int c = argb[srcY * w + x];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                int a = (c >> 24) & 0xFF;
                pixmap.drawPixel(x, y, (r << 24) | (g << 16) | (b << 8) | a);
            }
        }

        return pixmap;
    }

    public static BufferedImage toBufferedImage(Pixmap pixmap) {

        int w = pixmap.getWidth();
        int h = pixmap.getHeight();
        int[] argb = new int[w * h];
        boolean opaque = true;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int a = pixmap.getPixel(x, y) & EngineSetting.PACKED_COLOR_CHANNEL_MASK;

                argb[y * w + x] = pixmap.getPixelRGB(x, y) | (a << EngineSetting.PACKED_COLOR_ALPHA_SHIFT);
                opaque &= a == EngineSetting.PACKED_COLOR_CHANNEL_MASK;
            }
        }

        BufferedImage image = new BufferedImage(
                w, h, opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, w, h, argb, 0, w);

        return image;
    }

    // Copy \\

    public static void copyPixels(Pixmap source, Pixmap target) {

        if (source.getWidth() != target.getWidth() || source.getHeight() != target.getHeight())
            throwException("Cannot copy a " + source.getWidth() + "x" + source.getHeight() + " pixmap into a "
                    + target.getWidth() + "x" + target.getHeight() + " one.");

        ByteBuffer sourcePixels = source.getPixels().duplicate();
        ByteBuffer targetPixels = target.getPixels().duplicate();

        sourcePixels.clear();
        targetPixels.clear();
        targetPixels.put(sourcePixels);
    }
}
