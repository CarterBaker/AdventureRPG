package application.bootstrap.geometrypipeline.model;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.core.engine.InstancePackage;

public class ModelInstance extends InstancePackage {

    /*
     * Runtime model handed to external systems by ModelManager. Stores render
     * integers via MeshData and a MaterialInstance with independent uniform and
     * UBO state. Carries no ownership over GPU resources.
     */

    // Internal
    private MeshData meshData;
    private MaterialInstance material;

    // Constructor \\

    public void constructor(MeshData meshData, MaterialInstance material) {

        // Internal
        this.meshData = meshData;
        this.material = material;
    }

    // Accessible \\

    public MeshData getMeshData() {
        return meshData;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public int getVAO() {
        return meshData.getAttributeHandle();
    }

    public int getVertStride() {
        return meshData.getVertStride();
    }

    public int getVBO() {
        return meshData.getVertexHandle();
    }

    public int getVertCount() {
        return meshData.getVertexCount();
    }

    public int getIBO() {
        return meshData.getIndexHandle();
    }

    public int getIndexCount() {
        return meshData.getIndexCount();
    }
}