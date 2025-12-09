package com.AdventureRPG.core.geometrypipeline.vbomanager;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class VBOHandle {

    // Internal
    public final FloatArrayList vertices;
    public final int vertexCount;

    VBOHandle(
            FloatArrayList vertices,
            int vertexCount) {

        // Internal
        this.vertices = vertices;
        this.vertexCount = vertexCount;
    }
}
