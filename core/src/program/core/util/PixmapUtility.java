package program.core.util;

import program.core.engine.EngineUtility;
import program.core.util.image.Pixmap;

import java.awt.image.BufferedImage;

public class PixmapUtility extends EngineUtility {

    /*
     * Converts a BufferedImage to a Pixmap with ARGB to RGBA repacking.
     * Optional vertical flip for GL coordinate system alignment. Single
     * entry point for all GPU upload paths — pixel format logic lives here.
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
}