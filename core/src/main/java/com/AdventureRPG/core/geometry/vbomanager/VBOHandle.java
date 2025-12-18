package com.AdventureRPG.core.geometry.vbomanager;

import com.AdventureRPG.core.engine.HandleFrame;

public class VBOHandle extends HandleFrame {

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
