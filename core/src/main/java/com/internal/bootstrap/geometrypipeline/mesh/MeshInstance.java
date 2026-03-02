package com.internal.bootstrap.geometrypipeline.mesh;

import com.internal.bootstrap.geometrypipeline.ibo.IBOInstance;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshStruct;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOInstance;
import com.internal.core.engine.InstancePackage;

/*
 * A fully GPU-resident mesh created at runtime. Owns its VAO, VBO, and IBO
 * instances. Freed via MeshManager.removeMesh() by whoever created it.
 */
public class MeshInstance extends InstancePackage {

    // Internal
    private VAOInstance vaoInstance;
    private VBOInstance vboInstance;
    private IBOInstance iboInstance;
    private MeshStruct meshStruct;

    // Internal \\

    public void constructor(VAOInstance vaoInstance, VBOInstance vboInstance, IBOInstance iboInstance) {
        this.vaoInstance = vaoInstance;
        this.vboInstance = vboInstance;
        this.iboInstance = iboInstance;
        this.meshStruct = new MeshStruct(
                vaoInstance.getVAOStruct(),
                vboInstance.getVBOStruct(),
                iboInstance.getIBOStruct());
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

    public MeshStruct getMeshStruct() {
        return meshStruct;
    }
}