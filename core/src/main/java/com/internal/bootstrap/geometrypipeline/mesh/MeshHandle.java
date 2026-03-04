package com.internal.bootstrap.geometrypipeline.mesh;

import com.internal.bootstrap.geometrypipeline.ibo.IBOHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshStruct;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOHandle;
import com.internal.core.engine.HandlePackage;

/*
 * A fully GPU-resident static mesh assembled from JSON at bootstrap. Owned
 * exclusively by MeshManager for the engine lifetime. External systems receive
 * a ModelInstance built from this handle's MeshStruct — never the handle itself.
 */
public class MeshHandle extends HandlePackage {

    // Internal
    private VAOInstance vaoInstance;
    private VBOHandle vboHandle;
    private IBOHandle iboHandle;
    private MeshStruct meshStruct;

    // Internal \\

    public void constructor(VAOInstance vaoInstance, VBOHandle vboHandle, IBOHandle iboHandle) {
        this.vaoInstance = vaoInstance;
        this.vboHandle = vboHandle;
        this.iboHandle = iboHandle;
        this.meshStruct = new MeshStruct(
                vaoInstance.getVAOStruct(),
                vboHandle.getVBOStruct(),
                iboHandle.getIBOStruct());
    }

    // Accessible \\

    public VAOInstance getVAOInstance() {
        return vaoInstance;
    }

    public VBOHandle getVBOHandle() {
        return vboHandle;
    }

    public IBOHandle getIBOHandle() {
        return iboHandle;
    }

    public MeshStruct getMeshStruct() {
        return meshStruct;
    }
}