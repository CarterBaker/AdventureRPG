package com.internal.bootstrap.geometrypipeline.vbo;

import com.internal.core.engine.HandlePackage;

public class VBOHandle extends HandlePackage {

    /*
     * Persistent shared reference to a vertex buffer. Registered and owned
     * by VBOManager for the engine lifetime. External systems access vertex
     * data through MeshHandle.
     */

    // Internal
    private VBOData vboData;

    // Constructor \\

    public void constructor(VBOData vboData) {

        // Internal
        this.vboData = vboData;
    }

    // Accessible \\

    public VBOData getVBOData() {
        return vboData;
    }
}