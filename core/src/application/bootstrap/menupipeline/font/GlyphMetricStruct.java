package application.bootstrap.menupipeline.font;

public class GlyphMetricStruct {

    /*
     * Immutable glyph layout record. Carries only the metrics needed to
     * position a glyph quad — bearing, advance, and pixel dimensions.
     * UV coordinates live in the glyph's TextureHandle in TextureManager.
     */

    public final int width;
    public final int height;
    public final int bearingX;
    public final int bearingY;
    public final int advance;

    public GlyphMetricStruct(
            int width, int height,
            int bearingX, int bearingY,
            int advance) {

        this.width = width;
        this.height = height;
        this.bearingX = bearingX;
        this.bearingY = bearingY;
        this.advance = advance;
    }
}