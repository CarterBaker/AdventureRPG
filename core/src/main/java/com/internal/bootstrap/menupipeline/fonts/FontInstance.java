package com.internal.bootstrap.menupipeline.fonts;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.FontGeometryBranch;
import com.internal.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.core.engine.InstancePackage;
import com.internal.core.util.mathematics.vectors.Vector4;

public class FontInstance extends InstancePackage {

    private FontHandle handle;
    private DynamicModelHandle mergedModel;
    private ModelInstance modelInstance;
    private final Vector4 color = new Vector4(1f, 1f, 1f, 1f);
    private float textWidth;
    private float textHeight;
    private boolean dirty;

    // Constructor \\

    public void constructor(FontHandle handle, DynamicModelHandle mergedModel) {
        this.handle = handle;
        this.mergedModel = mergedModel;
        this.color.set(1f, 1f, 1f, 1f);
        this.textWidth = 0f;
        this.textHeight = 0f;
        this.dirty = false;
    }

    // Text \\

    public void setText(String text) {
        mergedModel.clear();
        textWidth = 0f;
        textHeight = 0f;
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
            float offsetY = cursorY + (metric.bearingY - metric.height);
            mergedModel.mergeWithOffset(
                    glyphModel,
                    new int[] { FontGeometryBranch.OFFSET_INDEX_X,
                            FontGeometryBranch.OFFSET_INDEX_Y },
                    new float[] { offsetX, offsetY });
            cursorX += metric.advance;
            if (metric.bearingY > textHeight)
                textHeight = metric.bearingY;
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

    public float getTextWidth() {
        return textWidth;
    }

    public float getTextHeight() {
        return textHeight;
    }
}