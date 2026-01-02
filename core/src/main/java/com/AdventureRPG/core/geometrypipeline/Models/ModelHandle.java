package com.AdventureRPG.core.geometrypipeline.Models;

import com.AdventureRPG.core.engine.HandlePackage;
import com.AdventureRPG.core.shaderpipeline.materials.Material;

public class ModelHandle extends HandlePackage {

    // Internal
    private int vaoHandle;
    private int vertStride;

    private int vboHandle;
    private int vertCount;

    private int iboHandle;
    private int indexCount;

    private Material material;

    // Internal \\

    public void constructor(
            int vaoHandle,
            int vertStride,

            int vboHandle,
            int vertCount,

            int iboHandle,
            int indexCount,

            Material material) {

        // Internal
        this.vaoHandle = vaoHandle;
        this.vertStride = vertStride;

        this.vboHandle = vboHandle;
        this.vertCount = vertCount;

        this.iboHandle = iboHandle;
        this.indexCount = indexCount;

        this.material = material;
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

    // Material
    public Material getMaterial() {
        return material;
    }
}
