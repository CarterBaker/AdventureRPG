package com.internal.bootstrap.geometrypipeline.vbo;

import com.internal.core.engine.StructPackage;

public class VBOStruct extends StructPackage {

    public final int vertexHandle;
    public final int vertexCount;

    public VBOStruct(int vertexHandle, int vertexCount) {
        this.vertexHandle = vertexHandle;
        this.vertexCount = vertexCount;
    }
}