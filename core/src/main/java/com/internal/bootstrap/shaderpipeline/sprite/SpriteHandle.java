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

    // Internal
    private String name;
    private int gpuHandle;
    private int width;
    private int height;
    private ModelInstance modelInstance;

    // Internal \\

    public void constructor(
            String name,
            int gpuHandle,
            int width,
            int height,
            ModelInstance modelInstance) {
        this.name = name;
        this.gpuHandle = gpuHandle;
        this.width = width;
        this.height = height;
        this.modelInstance = modelInstance;
    }

    // Accessible \\

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

    public ModelInstance getModelHandle() {
        return modelInstance;
    }
}