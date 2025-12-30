package com.AdventureRPG.core.geometrypipeline.Models;

import com.AdventureRPG.core.engine.HandlePackage;
import com.AdventureRPG.core.shaderpipeline.materials.Material;

public class ModelHandle extends HandlePackage {

    // Internal
    public final int vao;
    public final int vertStride;
    public final int vbo;
    public final int vertCount;
    public final int ibo;
    public final int indexCount;
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