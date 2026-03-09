package com.internal.bootstrap.shaderpipeline.sprite;

import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.core.engine.HandlePackage;

/*
 * Persistent GPU sprite resource owned exclusively by SpriteManager.
 * Never handed to external systems — callers receive a SpriteInstance
 * via SpriteManager.cloneSprite(). Holds the canonical ModelInstance
 * used as the clone template.
 */
public class SpriteHandle extends HandlePackage {

    private String name;
    private int gpuHandle;
    private int width;
    private int height;
    private ModelInstance modelInstance;

    // Nine-slice border: left, bottom, right, top — zero means no slicing
    private float borderLeft;
    private float borderBottom;
    private float borderRight;
    private float borderTop;

    public void constructor(
            String name, int gpuHandle,
            int width, int height,
            ModelInstance modelInstance,
            float borderLeft, float borderBottom,
            float borderRight, float borderTop) {
        this.name = name;
        this.gpuHandle = gpuHandle;
        this.width = width;
        this.height = height;
        this.modelInstance = modelInstance;
        this.borderLeft = borderLeft;
        this.borderBottom = borderBottom;
        this.borderRight = borderRight;
        this.borderTop = borderTop;
    }

    public boolean hasSlice() {
        return borderLeft != 0 || borderBottom != 0
                || borderRight != 0 || borderTop != 0;
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

    public String getName() {
        return name;
    }

    public int getGPUHandle() {
        return gpuHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}