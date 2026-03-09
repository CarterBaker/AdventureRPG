package com.internal.bootstrap.menupipeline.fonts;

import java.awt.image.BufferedImage;

import com.internal.core.util.atlas.AtlasTileData;

/*
 * Bootstrap-only container for a single rasterized glyph tile. Holds the
 * glyph's rendered image and the metrics needed to build GlyphMetricStruct
 * after packing. Derives tile dimensions from the rasterized image on
 * construction. Must not be held after bootstrap completes.
 */
public class FontTileData extends AtlasTileData {

    // Internal
    private int codepoint;
    private BufferedImage image;

    // Metrics — from rasterizer, used post-pack to build GlyphMetricStruct
    private int bearingX;
    private int bearingY;
    private int advance;

    // Internal \\

    public void constructor(
            int codepoint,
            BufferedImage image,
            int bearingX, int bearingY,
            int advance) {
        this.codepoint = codepoint;
        this.image = image;
        this.bearingX = bearingX;
        this.bearingY = bearingY;
        this.advance = advance;
        setTileDimensions(image.getWidth(), image.getHeight());
    }

    // Disposal \\

    public void clearImage() {
        this.image = null;
    }

    // Accessible \\

    public int getCodepoint() {
        return codepoint;
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getBearingX() {
        return bearingX;
    }

    public int getBearingY() {
        return bearingY;
    }

    public int getAdvance() {
        return advance;
    }
}