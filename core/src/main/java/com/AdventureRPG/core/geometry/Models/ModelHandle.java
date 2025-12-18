package com.AdventureRPG.core.geometry.Models;

import com.AdventureRPG.core.engine.HandleFrame;
import com.AdventureRPG.core.shaders.materials.Material;

public class ModelHandle extends HandleFrame {

    // Internal
    final int vao;
    final int vertStride;
    final int vbo;
    final int vertCount;
    final int ibo;
    final int indexCount;
    public final Material material;

    public ModelHandle(
            int vao,
            int vertStride,
            int vbo,
            int vertCount,
            int ibo,
            int indexCount,
            Material material) {

        // Internal
        this.vao = vao;
        this.vertStride = vertStride;
        this.vbo = vbo;
        this.vertCount = vertCount;
        this.ibo = ibo;
        this.indexCount = indexCount;
        this.material = material;
    }
}