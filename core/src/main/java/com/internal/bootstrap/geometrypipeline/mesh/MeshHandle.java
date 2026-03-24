package com.internal.bootstrap.geometrypipeline.mesh;

import com.internal.bootstrap.geometrypipeline.ibo.IBOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vbo.VBOHandle;
import com.internal.core.engine.HandlePackage;

public class MeshHandle extends HandlePackage {

    /*
     * A fully GPU-resident static mesh assembled from JSON at bootstrap. Owned
     * exclusively by MeshManager for the engine lifetime. External systems receive
     * a ModelInstance built from this handle's MeshData — never the handle itself.
     * Stores the VAOHandle template — actual VAO instances are created per-window
     * at render time.
     */

    // Internal
    private VAOHandle vaoHandle;
    private VBOHandle vboHandle;
    private IBOHandle iboHandle;
    private MeshData meshData;

    // Constructor \\

    public void constructor(
            VAOHandle vaoHandle,
            VBOHandle vboHandle,
            IBOHandle iboHandle) {

        // Internal
        this.vaoHandle = vaoHandle;
        this.vboHandle = vboHandle;
        this.iboHandle = iboHandle;
        this.meshData = new MeshData(
                vaoHandle,
                vboHandle.getVBOData(),
                iboHandle.getIBOData());
    }

    // Accessible \\

    public VAOHandle getVAOHandle() {
        return vaoHandle;
    }

    public VBOHandle getVBOHandle() {
        return vboHandle;
    }

    public IBOHandle getIBOHandle() {
        return iboHandle;
    }

    public MeshData getMeshData() {
        return meshData;
    }
}