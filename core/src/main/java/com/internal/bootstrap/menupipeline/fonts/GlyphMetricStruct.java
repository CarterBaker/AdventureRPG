package com.internal.bootstrap.menupipeline.fonts;

import com.internal.core.engine.StructPackage;

/*
 * Immutable metrics for a single rasterized glyph. Carries its pixel-space
 * position within the font atlas, its rendered dimensions, and the layout
 * values needed to position and advance the cursor during text layout.
 */
public class GlyphMetricStruct extends StructPackage {

    // Atlas position — pixel space
    public final int atlasX;
    public final int atlasY;

    // Rendered glyph dimensions in pixels
    public final int width;
    public final int height;

    // Offset from cursor baseline to top-left of glyph image in pixels
    public final int bearingX;
    public final int bearingY;

    // How far to advance the cursor after drawing this glyph in pixels
    public final int advance;

    public GlyphMetricStruct(
            int atlasX, int atlasY,
            int width, int height,
            int bearingX, int bearingY,
            int advance) {
        this.atlasX = atlasX;
        this.atlasY = atlasY;
        this.width = width;
        this.height = height;
        this.bearingX = bearingX;
        this.bearingY = bearingY;
        this.advance = advance;
    }
}