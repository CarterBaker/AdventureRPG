package com.internal.bootstrap.shaderpipeline.sprite;

import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.core.engine.InstancePackage;

/*
 * A live sprite instance handed to external systems via SpriteManager.cloneSprite().
 * Owns its own ModelInstance with independent material and uniform state.
 * The GPU texture handle is shared with the originating SpriteHandle and is
 * never owned or disposed by this instance.
 */
public class SpriteInstance extends InstancePackage {

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