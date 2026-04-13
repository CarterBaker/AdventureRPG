package application.bootstrap.geometrypipeline.vao;

import application.core.engine.InstancePackage;

public class VAOInstance extends InstancePackage {

    /*
     * A live GPU vertex array object created from a VAOHandle template.
     * Holds a fully initialized VAOData with a real GL handle. Owned by
     * whoever created it — released via VAOManager.removeVAOInstance().
     */

    // Internal
    private VAOData vaoData;

    // Constructor \\

    public void constructor(VAOData vaoData) {

        // Internal
        this.vaoData = vaoData;
    }

    // Accessible \\

    public VAOData getVAOData() {
        return vaoData;
    }
}