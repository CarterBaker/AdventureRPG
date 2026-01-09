package com.AdventureRPG.bootstrap.geometrypipeline.vaomanager;

import com.AdventureRPG.core.engine.HandlePackage;

public class VAOHandle extends HandlePackage {

    // Internal
    private int attributeHandle;
    private int vertStride;

    // Internal \\

    public void constructor(
            int attributeHandle,
            int vertStride) {

        // Internal
        this.attributeHandle = attributeHandle;
        this.vertStride = vertStride;
    }

    // Accessible \\

    public int getAttributeHandle() {
        return attributeHandle;
    }

    public int getVertStride() {
        return vertStride;
    }
}