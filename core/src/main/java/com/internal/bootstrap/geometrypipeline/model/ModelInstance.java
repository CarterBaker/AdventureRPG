package com.internal.bootstrap.geometrypipeline.model;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshStruct;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.core.engine.InstancePackage;

/*
 * Runtime model handed to external systems by ModelManager. Stores render
 * integers via MeshStruct and a MaterialInstance with independent uniform and
 * UBO state. Carries no ownership over GPU resources.
 */
public class ModelInstance extends InstancePackage {

    // Internal
    private MeshStruct meshStruct;
    private MaterialInstance material;

    // Internal \\

    public void constructor(MeshStruct meshStruct, MaterialInstance material) {
        this.meshStruct = meshStruct;
        this.material = material;
    }

    // Accessible \\

    public MeshStruct getMeshStruct() {
        return meshStruct;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public int getVAO() {
        return meshStruct.vaoStruct.attributeHandle;
    }

    public int getVertStride() {
        return meshStruct.vaoStruct.vertStride;
    }

    public int getVBO() {
        return meshStruct.vboStruct.vertexHandle;
    }

    public int getVertCount() {
        return meshStruct.vboStruct.vertexCount;
    }

    public int getIBO() {
        return meshStruct.iboStruct.indexHandle;
    }

    public int getIndexCount() {
        return meshStruct.iboStruct.indexCount;
    }
}