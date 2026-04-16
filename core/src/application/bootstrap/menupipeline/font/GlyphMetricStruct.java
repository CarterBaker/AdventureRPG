package application.bootstrap.menupipeline.font;

import engine.root.StructPackage;

public class GlyphMetricStruct extends StructPackage {

    /*
     * Immutable metrics for a single rasterized glyph. Carries its pixel-space
     * position within the font atlas, rendered dimensions, and layout values
     * needed to position and advance the cursor during text assembly.
     * Public final fields are intentional — accessed directly in hot text
     * layout loops to avoid getter overhead.
     */

    // Atlas position
    public final int atlasX;
    public final int atlasY;

    // Dimensions
    public final int width;
    public final int height;

    // Cursor layout
    public final int bearingX;
    public final int bearingY;
    public final int advance;

    // Constructor \\

    public GlyphMetricStruct(
            int atlasX, int atlasY,
            int width, int height,
            int bearingX, int bearingY,
            int advance) {

        // Atlas position
        this.atlasX = atlasX;
        this.atlasY = atlasY;

        // Dimensions
        this.width = width;
        this.height = height;

        // Cursor layout
        this.bearingX = bearingX;
        this.bearingY = bearingY;
        this.advance = advance;
    }
}