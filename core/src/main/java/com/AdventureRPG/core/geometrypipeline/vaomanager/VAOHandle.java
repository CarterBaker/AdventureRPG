package com.AdventureRPG.core.geometrypipeline.vaomanager;

public class VAOHandle {

    // Internal
    public final int vertStride;
    public final int gpuHandle;

    VAOHandle(
            int vertStride,
            int gpuHandle) {

        // Internal
        this.vertStride = vertStride;
        this.gpuHandle = gpuHandle;
    }
}