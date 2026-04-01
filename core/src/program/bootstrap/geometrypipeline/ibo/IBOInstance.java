package program.bootstrap.geometrypipeline.ibo;

import program.core.engine.InstancePackage;

public class IBOInstance extends InstancePackage {

    /*
     * Runtime index buffer created on demand for dynamic meshes. Holds its
     * own IBOData independently of the manager palette. Released via
     * IBOManager.removeIBOInstance() by whoever created it.
     */

    // Internal
    private IBOData iboData;

    // Constructor \\

    public void constructor(IBOData iboData) {

        // Internal
        this.iboData = iboData;
    }

    // Accessible \\

    public IBOData getIBOData() {
        return iboData;
    }
}