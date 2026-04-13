package application.bootstrap.geometrypipeline.vbo;

import application.core.engine.InstancePackage;

public class VBOInstance extends InstancePackage {

    /*
     * Runtime vertex buffer created on demand for dynamic meshes. Holds its
     * own VBOData independently of the manager palette. Released via
     * VBOManager.removeVBOInstance() by whoever created it.
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