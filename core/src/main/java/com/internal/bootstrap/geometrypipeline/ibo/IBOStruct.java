package com.internal.bootstrap.geometrypipeline.ibo;

import com.internal.core.engine.StructPackage;

public class IBOStruct extends StructPackage {

    public final int indexHandle;
    public final int indexCount;

    public IBOStruct(int indexHandle, int indexCount) {
        this.indexHandle = indexHandle;
        this.indexCount = indexCount;
    }
}