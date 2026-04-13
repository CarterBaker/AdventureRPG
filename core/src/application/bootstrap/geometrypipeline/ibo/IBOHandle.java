package application.bootstrap.geometrypipeline.ibo;

import engine.root.HandlePackage;

public class IBOHandle extends HandlePackage {

    /*
     * Persistent shared reference to an index buffer. Registered and owned
     * by IBOManager for the engine lifetime. Never handed out directly —
     * external systems access index data through MeshHandle.
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