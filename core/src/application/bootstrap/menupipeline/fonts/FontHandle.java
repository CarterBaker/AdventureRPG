package application.bootstrap.menupipeline.fonts;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.core.engine.HandlePackage;
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

    // Glyphs
    private Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs;
    private Int2ObjectOpenHashMap<DynamicModelHandle> glyphModels;

    // Constructor \\

    public void constructor(
            String name,
            int gpuHandle,
            int materialID,
            int atlasPixelSize,
            Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs,
            Int2ObjectOpenHashMap<DynamicModelHandle> glyphModels) {

        // Identity
        this.name = name;
        this.gpuHandle = gpuHandle;
        this.materialID = materialID;
        this.atlasPixelSize = atlasPixelSize;

        // Glyphs
        this.glyphs = glyphs;
        this.glyphModels = glyphModels;
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

    public boolean hasGlyph(int codepoint) {
        return glyphs.containsKey(codepoint);
    }

    public GlyphMetricStruct getGlyph(int codepoint) {
        return glyphs.get(codepoint);
    }

    public DynamicModelHandle getGlyphModel(int codepoint) {
        return glyphModels.get(codepoint);
    }

    public Int2ObjectOpenHashMap<GlyphMetricStruct> getGlyphs() {
        return glyphs;
    }

    public Int2ObjectOpenHashMap<DynamicModelHandle> getGlyphModels() {
        return glyphModels;
    }
}