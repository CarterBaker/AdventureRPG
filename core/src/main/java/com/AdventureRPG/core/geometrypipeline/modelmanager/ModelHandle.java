package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.kernel.HandleFrame;

public class ModelHandle extends HandleFrame {

    // Internal
    final int vao;
    final int vertStride;
    final int vbo;
    final int vertCount;
    final int ibo;
    final int indexCount;
    public final int shaderHandle;

    public ModelHandle(
            int vao,
            int vertStride,
            int vbo,
            int vertCount,
            int ibo,
            int indexCount,
            int shaderHandle) {

        // Internal
        this.vao = vao;
        this.vertStride = vertStride;
        this.vbo = vbo;
        this.vertCount = vertCount;
        this.ibo = ibo;
        this.indexCount = indexCount;
        this.shaderHandle = shaderHandle;
    }
}