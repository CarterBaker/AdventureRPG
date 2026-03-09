package com.internal.bootstrap.menupipeline.fonts;

import com.internal.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/*
 * Immutable font definition owned by FontManager. Holds the GPU texture
 * handle for the rasterized glyph atlas, the atlas pixel size, the full
 * glyph metric table keyed by codepoint, and one pre-built origin-space
 * DynamicModelHandle per glyph. The glyph models are static forever —
 * label creation merges them with cursor offsets into a FontInstance model.
 * Never mutated after bootstrap completes.
 */
public class FontHandle extends HandlePackage {

    // Internal
    private String name;
    private int gpuHandle;
    private int materialID;
    private int atlasPixelSize;
    private Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs;
    private Int2ObjectOpenHashMap<DynamicModelHandle> glyphModels;

    // Internal \\

    public void constructor(
            String name,
            int gpuHandle,
            int materialID,
            int atlasPixelSize,
            Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs,
            Int2ObjectOpenHashMap<DynamicModelHandle> glyphModels) {
        this.name = name;
        this.gpuHandle = gpuHandle;
        this.materialID = materialID;
        this.atlasPixelSize = atlasPixelSize;
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