package com.internal.bootstrap.geometrypipeline.mesh;

import com.internal.bootstrap.geometrypipeline.ibo.IBOData;
import com.internal.bootstrap.geometrypipeline.vao.VAOData;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vbo.VBOData;
import com.internal.core.engine.DataPackage;

public class MeshData extends DataPackage {

    /*
     * Flat aggregation of VAO, VBO, and IBO data for one GPU-resident mesh.
     * Provides direct convenience accessors for all render-critical handles
     * and counts without requiring callers to reach through each sub-data object.
     * Stores the VAOHandle template — actual VAO instances are resolved per-window
     * at render time.
     */

    // Internal
    private final VAOHandle vaoHandle;
    private final VBOData vboData;
    private final IBOData iboData;

    // Constructor \\

    public MeshData(VAOHandle vaoHandle, VBOData vboData, IBOData iboData) {

        // Internal
        this.vaoHandle = vaoHandle;
        this.vboData = vboData;
        this.iboData = iboData;
    }

    // Accessible \\

    public VAOHandle getVAOHandle() {
        return vaoHandle;
    }

    public VBOData getVBOData() {
        return vboData;
    }

    public IBOData getIBOData() {
        return iboData;
    }

    public int getVertStride() {
        return vaoHandle.getVAOData().getVertStride();
    }

    public int getVertexHandle() {
        return vboData.getVertexHandle();
    }

    public int getVertexCount() {
        return vboData.getVertexCount();
    }

    public int getIndexHandle() {
        return iboData.getIndexHandle();
    }

    public int getIndexCount() {
        return iboData.getIndexCount();
    }
}