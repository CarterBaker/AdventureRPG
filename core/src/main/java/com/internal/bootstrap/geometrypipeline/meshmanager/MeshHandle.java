package com.internal.bootstrap.geometrypipeline.meshmanager;

import com.internal.core.engine.HandlePackage;

public class MeshHandle extends HandlePackage {

    // Internal
    private int vaoHandle;
    private int vertStride;

    private int vboHandle;
    private int vertCount;

    private int iboHandle;
    private int indexCount;

    // Internal \\

    public void constructor(
            int vaoHandle,
            int vertStride,

            int vboHandle,
            int vertCount,

            int iboHandle,
            int indexCount) {

        // Internal
        this.vaoHandle = vaoHandle;
        this.vertStride = vertStride;

        this.vboHandle = vboHandle;
        this.vertCount = vertCount;

        this.iboHandle = iboHandle;
        this.indexCount = indexCount;
    }

    // Accessible \\

    // VAO
    public int getVaoHandle() {
        return vaoHandle;
    }

    public int getVertStride() {
        return vertStride;
    }

    // VBO
    public int getVboHandle() {
        return vboHandle;
    }

    public int getVertCount() {
        return vertCount;
    }

    // IBO
    public int getIboHandle() {
        return iboHandle;
    }

    public int getIndexCount() {
        return indexCount;
    }
}
