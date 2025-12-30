package com.AdventureRPG.core.geometrypipeline.vbomanager;

import com.AdventureRPG.core.engine.HandlePackage;

public class VBOHandle extends HandlePackage {

    // Internal
    public final int vertexHandle;
    public final int vertexCount;

    public VBOHandle(
            int vertexHandle,
            int vertexCount) {

        // Internal
        this.vertexHandle = vertexHandle;
        this.vertexCount = vertexCount;
    }
}
