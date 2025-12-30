package com.AdventureRPG.core.geometrypipeline.vaomanager;

import com.AdventureRPG.core.engine.HandlePackage;

public class VAOHandle extends HandlePackage {

    // Internal
    public final int attributeHandle;
    public final int vertStride;

    public VAOHandle(
            int attributeHandle,
            int vertStride) {

        // Internal
        this.attributeHandle = attributeHandle;
        this.vertStride = vertStride;
    }
}