package com.internal.bootstrap.geometrypipeline.vaomanager;

import com.internal.core.engine.HandlePackage;

public class VAOHandle extends HandlePackage {

    // Internal
    private int attributeHandle;
    private int vertStride;
    private int[] attrSizes;

    // Internal \\

    public void constructor(
            int attributeHandle,
            int[] attrSizes) {

        this.attributeHandle = attributeHandle;
        this.attrSizes = attrSizes;

        // Derive stride from sum of attribute sizes
        int stride = 0;
        for (int size : attrSizes)
            stride += size;
        this.vertStride = stride;
    }

    // Accessible \\

    public int getAttributeHandle() {
        return attributeHandle;
    }

    public int getVertStride() {
        return vertStride;
    }

    public int[] getAttrSizes() {
        return attrSizes;
    }
}