package application.bootstrap.geometrypipeline.vao;

import engine.root.HandlePackage;

public class VAOHandle extends HandlePackage {

    /*
     * Pure layout template. Carries no GPU object — VAOData.attributeHandle is 0.
     * Used exclusively as the source from which VAOInstances are created; a
     * layout is all floats unless it names a type for every attribute.
     * Owned by VAOManager for the engine lifetime.
     */

    // Internal
    private VAOData vaoData;

    // Constructor \\

    public void constructor(int[] attrSizes) {

        // Internal
        this.vaoData = new VAOData(0, attrSizes);
    }

    public void constructor(int[] attrSizes, int[] attrTypes, boolean[] attrNormalized) {

        // Internal
        this.vaoData = new VAOData(0, attrSizes, attrTypes, attrNormalized);
    }

    // Accessible \\

    public VAOData getVAOData() {
        return vaoData;
    }
}
