package application.bootstrap.geometrypipeline.vao;

import application.core.engine.HandlePackage;

public class VAOHandle extends HandlePackage {

    /*
     * Pure layout template. Carries no GPU object — VAOData.attributeHandle is 0.
     * Used exclusively as the source from which VAOInstances are created.
     * Owned by VAOManager for the engine lifetime.
     */

    // Internal
    private VAOData vaoData;

    // Constructor \\

    public void constructor(int[] attrSizes) {

        // Internal
        this.vaoData = new VAOData(0, attrSizes);
    }

    // Accessible \\

    public VAOData getVAOData() {
        return vaoData;
    }
}