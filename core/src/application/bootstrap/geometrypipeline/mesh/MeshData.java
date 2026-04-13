package application.bootstrap.geometrypipeline.mesh;

import application.bootstrap.geometrypipeline.ibo.IBOData;
import application.bootstrap.geometrypipeline.vao.VAOData;
import application.bootstrap.geometrypipeline.vbo.VBOData;
import application.core.engine.DataPackage;

public class MeshData extends DataPackage {

    /*
     * Flat aggregation of VAO, VBO, and IBO data for one GPU-resident mesh.
     * Provides direct convenience accessors for all render-critical handles
     * and counts without requiring callers to reach through each sub-data object.
     */

    // Internal
    private final VAOData vaoData;
    private final VBOData vboData;
    private final IBOData iboData;

    // Constructor \\

    public MeshData(VAOData vaoData, VBOData vboData, IBOData iboData) {

        // Internal
        this.vaoData = vaoData;
        this.vboData = vboData;
        this.iboData = iboData;
    }

    // Accessible \\

    public VAOData getVAOData() {
        return vaoData;
    }

    public VBOData getVBOData() {
        return vboData;
    }

    public IBOData getIBOData() {
        return iboData;
    }

    public int getAttributeHandle() {
        return vaoData.getAttributeHandle();
    }

    public int getVertStride() {
        return vaoData.getVertStride();
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