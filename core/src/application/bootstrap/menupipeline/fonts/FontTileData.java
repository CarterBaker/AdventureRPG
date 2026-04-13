package application.bootstrap.menupipeline.fonts;

import java.awt.image.BufferedImage;

import application.core.util.atlas.AtlasTileData;

public class FontTileData extends AtlasTileData {

    /*
     * Bootstrap-only container for a single rasterized glyph tile. Holds the
     * glyph's rendered image and the metrics needed to build GlyphMetricStruct
     * after atlas packing. Image reference is cleared after upload to free
     * heap memory. Must not be held after bootstrap completes.
     */

    // Internal
    private int codepoint;
    private BufferedImage image;

    // Metrics
    private int bearingX;
    private int bearingY;
    private int advance;

    // Constructor \\

    public void constructor(
            int codepoint,
            BufferedImage image,
            int bearingX,
            int bearingY,
            int advance) {

        // Internal
        this.codepoint = codepoint;
        this.image = image;

        // Metrics
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