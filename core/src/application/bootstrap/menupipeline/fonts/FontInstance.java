package application.bootstrap.menupipeline.fonts;

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
     * and the uploaded ModelInstance pushed to the GPU. Text changes accumulate
     * into the merged model and are uploaded lazily on the next render cycle.
     */

    // Internal
    private FontHandle handle;
    private DynamicModelHandle mergedModel;
    private ModelInstance modelInstance;

    // State
    private final Vector4 color = new Vector4(1f, 1f, 1f, 1f);
    private float fontSize;
    private float textWidth;
    private float textHeight;
    private boolean dirty;

    // Scratch — cached to avoid allocation per glyph per setText call
    private final int[] offsetIndices = new int[] {
            EngineSetting.FONT_DEFAULT_OFFSET_INDEX_X,
            EngineSetting.FONT_DEFAULT_OFFSET_INDEX_Y
    };
    private final float[] offsets = new float[2];

    // Constructor \\

    public void constructor(FontHandle handle, DynamicModelHandle mergedModel) {

        // Internal
        this.handle = handle;
        this.mergedModel = mergedModel;
        this.fontSize = EngineSetting.FONT_DEFAULT_SIZE;
    }

    // Text \\

    public void setText(String text) {

        mergedModel.clear();
        textWidth = 0f;
        textHeight = 0f;
        float scale = fontSize > 0f && handle.getAtlasPixelSize() > 0f
                ? fontSize / handle.getAtlasPixelSize()
                : 1f;

        if (text == null || text.isEmpty()) {
            dirty = true;
            return;
        }

        float cursorX = 0f;
        float cursorY = 0f;

        for (int i = 0; i < text.length();) {

            int codepoint = text.codePointAt(i);
            i += Character.charCount(codepoint);

            if (codepoint == ' ') {
                GlyphMetricStruct space = handle.getGlyph(codepoint);
                cursorX += space != null
                        ? space.advance * scale
                        : handle.getAtlasPixelSize() * 0.25f * scale;
                continue;
            }

            DynamicModelHandle glyphModel = handle.getGlyphModel(codepoint);
            GlyphMetricStruct metric = handle.getGlyph(codepoint);

            if (glyphModel == null || metric == null)
                continue;

            offsets[0] = cursorX + metric.bearingX * scale;
            offsets[1] = cursorY + (metric.bearingY - metric.height) * scale;

            mergedModel.mergeWithOffset(glyphModel, offsetIndices, offsets);

            cursorX += metric.advance * scale;

            float metricHeight = metric.height * scale;

            if (metricHeight > textHeight)
                textHeight = metricHeight;
        }

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

    public float getTextWidth() {
        return textWidth;
    }

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