package com.internal.core.util;

import java.awt.image.BufferedImage;

import com.internal.platform.graphics.Pixmap;
import com.internal.core.engine.UtilityPackage;

/*
 * Centralises BufferedImage → platform Pixmap conversion with ARGB → RGBA
 * repacking and optional vertical flip. Used by all GPU upload paths to
 * keep pixel-format logic in one place.
 */
public class PixmapUtility extends UtilityPackage {

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
                int a = (c >> 24) & 0xFF;
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = (c) & 0xFF;
                pixmap.drawPixel(x, y, (r << 24) | (g << 16) | (b << 8) | a);
            }
        }

        return pixmap;
    }
}