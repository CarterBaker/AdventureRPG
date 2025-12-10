package com.AdventureRPG.core.geometrypipeline.vaomanager;

import com.AdventureRPG.core.kernel.HandleFrame;

public class VAOHandle extends HandleFrame {

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