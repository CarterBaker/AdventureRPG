package application.bootstrap.menupipeline.font;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector4;

public class FontInstance extends InstancePackage {

    /*
     * Per-label runtime font state. Holds a reference to the shared FontHandle,
     * a merged DynamicModelHandle assembled from per-glyph origin-space models,
     * and the uploaded ModelInstance pushed to the GPU.
     *
     * All geometry and cursor offsets are kept in raw atlas-pixel units.
     * Font size scaling is applied entirely in the render transform matrix —
     * NOT here. This means setText only needs to run when the string content
     * changes, not on every font-size change, because sizing is handled
     * downstream by the render system.
     */

    // Internal
    private FontHandle handle;
    private DynamicModelHandle mergedModel;
    private ModelInstance modelInstance;

    // State
    private final Vector4 color = new Vector4(1f, 1f, 1f, 1f);
    private float fontSize;
    private float textWidth; // raw atlas-pixel units — scale before use in screen space
    private float textHeight; // raw atlas-pixel units — scale before use in screen space
    private boolean dirty;

    // Scratch — cached to avoid allocation per glyph per setText call
    private final int[] offsetIndices = new int[] {
            EngineSetting.FONT_DEFAULT_OFFSET_INDEX_X,
            EngineSetting.FONT_DEFAULT_OFFSET_INDEX_Y
    };
    private final float[] offsets = new float[2];

    // Constructor \\

    public void constructor(FontHandle handle, DynamicModelHandle mergedModel) {
        this.handle = handle;
        this.mergedModel = mergedModel;
        this.fontSize = EngineSetting.FONT_RASTER_SIZE;
    }

    // Text \\

    public void setText(String text) {

        /*
         * Builds the merged glyph model in raw atlas-pixel units.
         * No font-size scale is applied — the render transform handles that.
         * FONT_LETTER_SPACING_RATIO adds a small per-character gap proportional
         * to the atlas pixel size so glyphs never bleed into each other at any
         * size. Set it to 0 in EngineSetting for pure metric-only spacing.
         */

        mergedModel.clear();
        textWidth = 0f;
        textHeight = 0f;

        if (text == null || text.isEmpty()) {
            dirty = true;
            return;
        }

        float letterSpacing = handle.getAtlasPixelSize() * EngineSetting.FONT_LETTER_SPACING_RATIO;
        float cursorX = 0f;

        for (int i = 0; i < text.length();) {

            int codepoint = text.codePointAt(i);
            i += Character.charCount(codepoint);

            if (codepoint == ' ') {
                GlyphMetricStruct space = handle.getGlyph(codepoint);
                cursorX += (space != null
                        ? space.advance
                        : handle.getAtlasPixelSize() * 0.25f) + letterSpacing;
                continue;
            }

            DynamicModelHandle glyphModel = handle.getGlyphModel(codepoint);
            GlyphMetricStruct metric = handle.getGlyph(codepoint);

            if (glyphModel == null || metric == null)
                continue;

            // Raw atlas-pixel offsets — no size scaling
            offsets[0] = cursorX + metric.bearingX;
            offsets[1] = metric.bearingY - metric.height;

            mergedModel.mergeWithOffset(glyphModel, offsetIndices, offsets);

            cursorX += metric.advance + letterSpacing;

            if (metric.height > textHeight)
                textHeight = metric.height;
        }

        // Strip trailing letter spacing from the last character
        if (cursorX > 0f)
            cursorX -= letterSpacing;

        textWidth = cursorX;
        dirty = true;
    }

    // Color \\

    public void setColor(float r, float g, float b, float a) {
        color.set(r, g, b, a);
        if (modelInstance != null)
            modelInstance.getMaterial().setUniform("u_color", color);
    }

    public Vector4 getColor() {
        return color;
    }

    // Font Size \\

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public float getFontSize() {
        return fontSize;
    }

    // GPU Lifecycle \\

    public void upload(ModelManager modelManager, MaterialManager materialManager) {

        if (!dirty || mergedModel.isEmpty())
            return;

        if (modelInstance != null)
            modelManager.removeMesh(modelInstance);

        MaterialInstance mat = materialManager.cloneMaterial(handle.getMaterialID());
        mat.setUniform("u_fontAtlas", handle.getGPUHandle());
        mat.setUniform("u_color", color);

        modelInstance = modelManager.createModel(
                mergedModel.getVAOHandle(),
                mergedModel.getVertices(),
                mergedModel.getIndices(),
                mat);

        dirty = false;
    }

    public void release(ModelManager modelManager) {
        if (modelInstance == null)
            return;
        modelManager.removeMesh(modelInstance);
        modelInstance = null;
    }

    // Accessible \\

    public FontHandle getHandle() {
        return handle;
    }

    public DynamicModelHandle getMergedModel() {
        return mergedModel;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    /**
     * Raw atlas-pixel width. Multiply by (fontSize / atlasPixelSize) for screen
     * pixels.
     */
    public float getTextWidth() {
        return textWidth;
    }

    /**
     * Raw atlas-pixel height. Multiply by (fontSize / atlasPixelSize) for screen
     * pixels.
     */
    public float getTextHeight() {
        return textHeight;
    }

    public boolean hasModel() {
        return modelInstance != null;
    }

    public boolean isDirty() {
        return dirty;
    }
}