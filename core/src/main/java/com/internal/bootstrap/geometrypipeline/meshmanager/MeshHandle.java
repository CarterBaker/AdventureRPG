package com.internal.bootstrap.geometrypipeline.meshmanager;

import com.internal.bootstrap.geometrypipeline.ibomanager.IBOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOHandle;
import com.internal.core.engine.HandlePackage;

public class MeshHandle extends HandlePackage {

    // Internal
    private VAOHandle vaoHandle;
    private VBOHandle vboHandle;
    private IBOHandle iboHandle;

    // Internal \\

    public void constructor(
            VAOHandle vaoHandle,
            VBOHandle vboHandle,
            IBOHandle iboHandle) {

        // Internal
        this.vaoHandle = vaoHandle;
        this.vboHandle = vboHandle;
        this.iboHandle = iboHandle;
    }

    // Accessible \\

    // VAO
    public int getVAOHandle() {
        return vaoHandle.getAttributeHandle();
    }

    public int getVertStride() {
        return vaoHandle.getVertStride();
    }

    // VBO
    public int getVboHandle() {
        return vboHandle.getVertexHandle();
    }

    public int getVertCount() {
        return vboHandle.getVertexCount();
    }

    // IBO
    public int getIboHandle() {
        return iboHandle.getIndexHandle();
    }

    public int getIndexCount() {
        return iboHandle.getIndexCount();
    }
}
