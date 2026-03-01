package com.internal.bootstrap.geometrypipeline.model;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.core.engine.InstancePackage;

/*
 * Runtime model handed to external systems by ModelManager.createModel().
 * Owns a MeshHandle reference and a MaterialInstance with independent uniform
 * and UBO state. Never holds a MaterialHandle — material state is always
 * instance-owned.
 */
public class ModelInstance extends InstancePackage {

    // Internal
    private MeshHandle meshHandle;
    private MaterialInstance material;

    // Internal \\

    public void constructor(
            MeshHandle meshHandle,
            MaterialInstance material) {
        this.meshHandle = meshHandle;
        this.material = material;
    }

    // Accessible \\

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public int getVAO() {
        return meshHandle.getVAO();
    }

    public int getVertStride() {
        return meshHandle.getVertStride();
    }

    public int getVBO() {
        return meshHandle.getVBO();
    }

    public int getVertCount() {
        return meshHandle.getVertCount();
    }

    public int getIBO() {
        return meshHandle.getIBO();
    }

    public int getIndexCount() {
        return meshHandle.getIndexCount();
    }
}