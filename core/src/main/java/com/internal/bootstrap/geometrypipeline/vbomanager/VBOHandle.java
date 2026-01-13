package com.internal.bootstrap.geometrypipeline.vbomanager;

import com.internal.core.engine.HandlePackage;

public class VBOHandle extends HandlePackage {

    // Internal
    private int vertexHandle;
    private int vertexCount;

    // Internal \\

    public void constructor(
            int vertexHandle,
            int vertexCount) {

        // Internal
        this.vertexHandle = vertexHandle;
        this.vertexCount = vertexCount;
    }

    // Accessible \\

    public int getVertexHandle() {
        return vertexHandle;
    }

    public int getVertexCount() {
        return vertexCount;
    }
}
