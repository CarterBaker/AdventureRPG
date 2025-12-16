package com.AdventureRPG.core.geometrypipeline.mesh;

import com.AdventureRPG.core.kernel.HandleFrame;

public class MeshHandle extends HandleFrame {

    // Internal
    public final int vao;
    public final int vertStride;
    public final int vbo;
    public final int vertCount;
    public final int ibo;
    public final int indexCount;

    public MeshHandle(
            int vao,
            int vertStride,
            int vbo,
            int vertCount,
            int ibo,
            int indexCount) {

        // Internal
        this.vao = vao;
        this.vertStride = vertStride;
        this.vbo = vbo;
        this.vertCount = vertCount;
        this.ibo = ibo;
        this.indexCount = indexCount;
    }
}