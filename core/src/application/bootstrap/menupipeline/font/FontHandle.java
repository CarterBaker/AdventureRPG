package application.bootstrap.menupipeline.font;

import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class FontHandle extends HandlePackage {

    /*
     * Immutable font definition owned by FontManager. Holds the GPU texture
     * handle for the rasterized glyph atlas, the atlas pixel size, the full
     * glyph metric table keyed by codepoint, and one pre-built origin-space
     * DynamicModelHandle per glyph. Glyph models are static forever — label
     * creation merges them with cursor offsets into a FontInstance model.
     * Never mutated after bootstrap completes.
     */

    // Identity
    private String name;
    private int gpuHandle;
    private int materialID;
    private int atlasPixelSize;
    private int rasterPixelSize;

    // Glyphs
    private Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs;

    // Constructor \\

    public void constructor(
            String name,
            int gpuHandle,
            int materialID,
            int atlasPixelSize,
            int rasterPixelSize,
            Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs) {

        // Identity
        this.name = name;
        this.gpuHandle = gpuHandle;
        this.materialID = materialID;
        this.atlasPixelSize = atlasPixelSize;
        this.rasterPixelSize = rasterPixelSize;

        // Glyphs
        this.glyphs = glyphs;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int getGPUHandle() {
        return gpuHandle;
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

    public Int2ObjectOpenHashMap<GlyphMetricStruct> getGlyphs() {
        return glyphs;
    }
}
