package com.internal.bootstrap.geometrypipeline.mesh;

import com.internal.bootstrap.geometrypipeline.ibo.IBOInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOInstance;
import com.internal.core.engine.InstancePackage;

public class MeshInstance extends InstancePackage {

    /*
     * A fully GPU-resident mesh created at runtime. Owns its VAO, VBO, and IBO
     * instances. Freed via MeshManager.removeMesh() by whoever created it.
     */

    // Internal
    private VAOInstance vaoInstance;
    private VBOInstance vboInstance;
    private IBOInstance iboInstance;
    private MeshData meshData;

    // Constructor \\

    public void constructor(
            VAOInstance vaoInstance,
            VBOInstance vboInstance,
            IBOInstance iboInstance) {

        // Internal
        this.vaoInstance = vaoInstance;
        this.vboInstance = vboInstance;
        this.iboInstance = iboInstance;
        this.meshData = new MeshData(
                vaoInstance.getVAOData(),
                vboInstance.getVBOData(),
                iboInstance.getIBOData());
    }

    // Accessible \\

    public VAOInstance getVAOInstance() {
        return vaoInstance;
    }

    public VBOInstance getVBOInstance() {
        return vboInstance;
    }

    public IBOInstance getIBOInstance() {
        return iboInstance;
    }

    public MeshData getMeshData() {
        return meshData;
    }

    public int getAttributeHandle() {
        return meshData.getAttributeHandle();
    }

    public int[] getAttrSizes() {
        return meshData.getVAOData().getAttrSizes();
    }

    public int getVertexHandle() {
        return meshData.getVertexHandle();
    }

    public int getIndexHandle() {
        return meshData.getIndexHandle();
    }

    public int getIndexCount() {
        return meshData.getIndexCount();
    }
}
