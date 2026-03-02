package com.internal.bootstrap.geometrypipeline.vao;

import com.internal.core.engine.StructPackage;

public class VAOStruct extends StructPackage {

    public final int attributeHandle;
    public final int vertStride;
    public final int[] attrSizes;

    public VAOStruct(int attributeHandle, int[] attrSizes) {
        this.attributeHandle = attributeHandle;
        this.attrSizes = attrSizes;
        int stride = 0;
        for (int size : attrSizes)
            stride += size;
        this.vertStride = stride;
    }
}