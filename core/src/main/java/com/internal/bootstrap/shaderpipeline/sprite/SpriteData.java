package com.internal.bootstrap.shaderpipeline.sprite;

import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.core.engine.DataPackage;

public class SpriteData extends DataPackage {

    /*
     * Complete sprite record. Identity and GPU texture handle are shared
     * references — never cloned. ModelInstance and sliceData are per-instance
     * state — populated on clone. sliceData is null on a SpriteHandle.
     */

    // Identity — shared, never copied
    private final String name;
    private final int gpuHandle;
    private final int width;
    private final int height;
    private final float borderLeft;
    private final float borderBottom;
    private final float borderRight;
    private final float borderTop;

    // Per-instance state
    private final ModelInstance modelInstance;
    private final UBOInstance sliceData;

    // Constructor — handle \\

    public SpriteData(
            String name,
            int gpuHandle,
            int width,
            int height,
            float borderLeft,
            float borderBottom,
            float borderRight,
            float borderTop,
            ModelInstance modelInstance) {

        this.name = name;
        this.gpuHandle = gpuHandle;
        this.width = width;
        this.height = height;
        this.borderLeft = borderLeft;
        this.borderBottom = borderBottom;
        this.borderRight = borderRight;
        this.borderTop = borderTop;
        this.modelInstance = modelInstance;
        this.sliceData = null;
    }

    // Constructor — instance \\

    public SpriteData(SpriteData source, ModelInstance modelInstance, UBOInstance sliceData) {

        this.name = source.name;
        this.gpuHandle = source.gpuHandle;
        this.width = source.width;
        this.height = source.height;
        this.borderLeft = source.borderLeft;
        this.borderBottom = source.borderBottom;
        this.borderRight = source.borderRight;
        this.borderTop = source.borderTop;
        this.modelInstance = modelInstance;
        this.sliceData = sliceData;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int getGpuHandle() {
        return gpuHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getBorderLeft() {
        return borderLeft;
    }

    public float getBorderBottom() {
        return borderBottom;
    }

    public float getBorderRight() {
        return borderRight;
    }

    public float getBorderTop() {
        return borderTop;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public UBOInstance getSliceData() {
        return sliceData;
    }

    public boolean hasSlice() {
        return borderLeft != 0 || borderBottom != 0
                || borderRight != 0 || borderTop != 0;
    }
}