package com.internal.bootstrap.geometrypipeline.mesh;

import com.internal.bootstrap.geometrypipeline.ibo.IBOInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vbo.VBOInstance;
import com.internal.core.engine.InstancePackage;

public class MeshInstance extends InstancePackage {

    /*
     * A fully GPU-resident mesh created at runtime. Owns its VAO template,
     * VBO instance, and IBO instance. Freed via MeshManager.removeMesh() by
     * whoever created it. Stores the VAOHandle template — actual VAO instances
     * are created per-window at render time.
     */

    // Internal
    private VAOHandle vaoHandle;
    private VBOInstance vboInstance;
    private IBOInstance iboInstance;
    private MeshData meshData;

    // Constructor \\

    public void constructor(
            VAOHandle vaoHandle,
            VBOInstance vboInstance,
            IBOInstance iboInstance) {

        // Internal
        this.vaoHandle = vaoHandle;
        this.vboInstance = vboInstance;
        this.iboInstance = iboInstance;
        this.meshData = new MeshData(
                vaoHandle,
                vboInstance.getVBOData(),
                iboInstance.getIBOData());
    }

    // Accessible \\

    public VAOHandle getVAOHandle() {
        return vaoHandle;
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
}