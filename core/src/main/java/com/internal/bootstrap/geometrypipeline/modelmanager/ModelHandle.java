package com.internal.bootstrap.geometrypipeline.modelmanager;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.core.engine.HandlePackage;

public class ModelHandle extends HandlePackage {

    // Internal
    private MeshHandle meshHandle;
    private MaterialHandle material;

    // Internal \\

    public void constructor(
            MeshHandle meshHandle,
            MaterialHandle material) {

        // Internal
        this.meshHandle = meshHandle;
        this.material = material;
    }

    // Accessible \\

    // Mesh
    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    // Material
    public MaterialHandle getMaterial() {
        return material;
    }

    // VAO
    public int getVAO() {
        return meshHandle.getVAO();
    }

    public int getVertStride() {
        return meshHandle.getVertStride();
    }

    // VBO
    public int getVBO() {
        return meshHandle.getVBO();
    }

    public int getVertCount() {
        return meshHandle.getVertCount();
    }

    // IBO
    public int getIBO() {
        return meshHandle.getIBO();
    }

    public int getIndexCount() {
        return meshHandle.getIndexCount();
    }
}
