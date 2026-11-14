package application.bootstrap.menupipeline.font;

import application.bootstrap.shaderpipeline.texture.TextureHandle;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class FontHandle extends HandlePackage {

    /*
     * Immutable font definition owned by FontManager. Holds the TextureHandle
     * for the rasterized glyph atlas, the atlas pixel size, and two per-glyph
     * tables keyed by codepoint — metrics for layout, TextureHandle for UVs.
     * Never mutated after bootstrap completes.
     */

    // Identity
    private String name;
    private int materialID;
    private int atlasPixelSize;
    private int rasterPixelSize;

    // Atlas
    private TextureHandle atlasHandle;

    // Glyphs
    private Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs;
    private Int2ObjectOpenHashMap<TextureHandle> glyphHandles;

    // Constructor \\

    public void constructor(
            String name,
            TextureHandle atlasHandle,
            int materialID,
            int atlasPixelSize,
            int rasterPixelSize,
            Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs,
            Int2ObjectOpenHashMap<TextureHandle> glyphHandles) {

        this.name = name;
        this.atlasHandle = atlasHandle;
        this.materialID = materialID;
        this.atlasPixelSize = atlasPixelSize;
        this.rasterPixelSize = rasterPixelSize;
        this.glyphs = glyphs;
        this.glyphHandles = glyphHandles;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public TextureHandle getAtlasHandle() {
        return atlasHandle;
    }

    public int getMaterialID() {
        return materialID;
    }

    public int getAtlasPixelSize() {
        return atlasPixelSize;
    }

    public int getRasterPixelSize() {
        return rasterPixelSize;
    }

    public boolean hasGlyph(int codepoint) {
        return glyphs.containsKey(codepoint);
    }

    public GlyphMetricStruct getGlyph(int codepoint) {
        return glyphs.get(codepoint);
    }

    public TextureHandle getGlyphHandle(int codepoint) {
        return glyphHandles.get(codepoint);
    }

    public Int2ObjectOpenHashMap<GlyphMetricStruct> getGlyphs() {
        return glyphs;
    }
}