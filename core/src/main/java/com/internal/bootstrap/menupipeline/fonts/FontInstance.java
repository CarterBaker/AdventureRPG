package com.internal.bootstrap.menupipeline.fonts;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.FontGeometryBranch;
import com.internal.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.core.engine.InstancePackage;

/*
 * Live instance of a font owned by an ElementInstance. Owns the merged
 * DynamicModelHandle (rebuilt on setText) and the GPU ModelInstance
 * (created at menu open, released at menu close). Color defaults to white.
 */
public class FontInstance extends InstancePackage {

    private FontHandle handle;
    private DynamicModelHandle mergedModel;
    private ModelInstance modelInstance;

    private float colorR = 1f;
    private float colorG = 1f;
    private float colorB = 1f;
    private float colorA = 1f;

    private boolean dirty;

    // Constructor \\

    public void constructor(FontHandle handle, DynamicModelHandle mergedModel) {
        this.handle = handle;
        this.mergedModel = mergedModel;
        this.dirty = false;
    }

    // Text \\

    public void setText(String text) {

        mergedModel.clear();

        if (text == null || text.isEmpty()) {
            dirty = true;
            return;
        }

        float cursorX = 0f;
        float cursorY = 0f;

        for (int i = 0; i < text.length(); i++) {
            int codepoint = text.codePointAt(i);

            if (codepoint == ' ') {
                GlyphMetricStruct space = handle.getGlyph(codepoint);
                if (space != null)
                    cursorX += space.advance;
                else
                    cursorX += handle.getAtlasPixelSize() * 0.25f;
                continue;
            }

            DynamicModelHandle glyphModel = handle.getGlyphModel(codepoint);
            GlyphMetricStruct metric = handle.getGlyph(codepoint);

            if (glyphModel == null || metric == null)
                continue;

            float offsetX = cursorX + metric.bearingX;
            float offsetY = cursorY - (metric.height - metric.bearingY);

            mergedModel.mergeWithOffset(
                    glyphModel,
                    new int[] { FontGeometryBranch.OFFSET_INDEX_X,
                            FontGeometryBranch.OFFSET_INDEX_Y },
                    new float[] { offsetX, offsetY });

            cursorX += metric.advance;
        }

        dirty = true;
    }

    // Color \\

    public void setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
    }

    public float getColorR() {
        return colorR;
    }

    public float getColorG() {
        return colorG;
    }

    public float getColorB() {
        return colorB;
    }

    public float getColorA() {
        return colorA;
    }

    // Model \\

    public void setModelInstance(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
        this.dirty = false;
    }

    public void clearModelInstance() {
        this.modelInstance = null;
    }

    public boolean hasModel() {
        return modelInstance != null;
    }

    public boolean isDirty() {
        return dirty;
    }

    // Accessors \\

    public FontHandle getHandle() {
        return handle;
    }

    public DynamicModelHandle getMergedModel() {
        return mergedModel;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}