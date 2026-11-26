package engine.assets.image;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;

import java.nio.ByteBuffer;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class PixmapUtility extends EngineUtility {

    /*
     * Converts a BufferedImage to a Pixmap's RGBA bytes, copying an 8-bit sRGB
     * image's rows straight from its raster and repacking any other through
     * ARGB. Optional vertical flip for GL coordinate system alignment. Single
     * entry point for all GPU upload paths — pixel format logic lives here,
     * as do the way back to a BufferedImage for saving, opaque whenever every
     * pixel is, and copying one Pixmap's pixels into another of its size.
     */

    // Conversion \\

    public static Pixmap fromBufferedImage(BufferedImage image, boolean flipVertical) {

        Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGBA8888);
        writeRGBA(image, flipVertical, pixmap.getPixels());

        return pixmap;
    }

    // An image's pixels written into a buffer of its size as RGBA bytes, row by row
    static void writeRGBA(BufferedImage image, boolean flipVertical, ByteBuffer pixels) {

        if (isByteSRGB(image))
            copyRows(image, flipVertical, pixels);
        else
            repackRows(image, flipVertical, pixels);
    }

    // An 8-bit sRGB image whose raster hands each pixel's bands over in R, G, B (, A) order
    private static boolean isByteSRGB(BufferedImage image) {

        if (!(image.getColorModel() instanceof ComponentColorModel colorModel))
            return false;

        int components = colorModel.getNumComponents();

        return colorModel.getTransferType() == DataBuffer.TYPE_BYTE
                && colorModel.getColorSpace().isCS_sRGB()
                && !colorModel.isAlphaPremultiplied()
                && (components == EngineSetting.COLOR_CHANNEL_COUNT
                        || components == EngineSetting.COLOR_CHANNEL_COUNT - 1);
    }

    private static void copyRows(BufferedImage image, boolean flipVertical, ByteBuffer pixels) {

        int w = image.getWidth();
        int h = image.getHeight();
        Raster raster = image.getRaster();
        int components = image.getColorModel().getNumComponents();
        byte[] row = new byte[w * components];
        int rowBytes = w * EngineSetting.COLOR_CHANNEL_COUNT;
        byte opaque = (byte) EngineSetting.PACKED_COLOR_CHANNEL_MASK;

        for (int y = 0; y < h; y++) {

            raster.getDataElements(0, flipVertical ? (h - 1 - y) : y, w, 1, row);
            int dst = y * rowBytes;

            if (components == EngineSetting.COLOR_CHANNEL_COUNT) {
                pixels.put(dst, row, 0, rowBytes);
                continue;
            }

            for (int x = 0, src = 0; x < w; x++, src += components, dst += EngineSetting.COLOR_CHANNEL_COUNT) {
                pixels.put(dst, row[src]);
                pixels.put(dst + 1, row[src + 1]);
                pixels.put(dst + 2, row[src + 2]);
                pixels.put(dst + 3, opaque);
            }
        }
    }

    private static void repackRows(BufferedImage image, boolean flipVertical, ByteBuffer pixels) {

        int w = image.getWidth();
        int h = image.getHeight();
        int[] argb = new int[w];

        for (int y = 0; y < h; y++) {

            image.getRGB(0, flipVertical ? (h - 1 - y) : y, w, 1, argb, 0, w);
            int dst = y * w * EngineSetting.COLOR_CHANNEL_COUNT;

            for (int x = 0; x < w; x++, dst += EngineSetting.COLOR_CHANNEL_COUNT) {
                int c = argb[x];
                pixels.put(dst, (byte) ((c >> 16) & 0xFF));
                pixels.put(dst + 1, (byte) ((c >> 8) & 0xFF));
                pixels.put(dst + 2, (byte) (c & 0xFF));
                pixels.put(dst + 3, (byte) ((c >> 24) & 0xFF));
            }
        }
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
