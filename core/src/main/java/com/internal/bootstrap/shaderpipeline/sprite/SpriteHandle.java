package com.internal.bootstrap.shaderpipeline.sprite;

import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.core.engine.HandlePackage;

public class SpriteHandle extends HandlePackage {

    private String name;
    private int gpuHandle;
    private int width;
    private int height;
    private ModelHandle modelHandle;

    public void constructor(
            String name,
            int gpuHandle,
            int width,
            int height,
            ModelHandle modelHandle) {

        this.name = name;
        this.gpuHandle = gpuHandle;
        this.width = width;
        this.height = height;
        this.modelHandle = modelHandle;
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

    public ModelHandle getModelHandle() {
        return modelHandle;
    }
}