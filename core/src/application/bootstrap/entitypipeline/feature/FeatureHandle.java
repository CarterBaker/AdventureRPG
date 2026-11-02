package application.bootstrap.entitypipeline.feature;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.root.HandlePackage;

public class FeatureHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded appearance option. Registered and
     * owned by FeatureManager. Shared by every AppearanceHandle that has it
     * selected — selection state never lives here.
     */

    // Internal
    private FeatureData featureData;

    // Constructor \\

    public void constructor(FeatureData featureData) {

        // Internal
        this.featureData = featureData;
    }

    // Accessible \\

    public FeatureData getFeatureData() {
        return featureData;
    }

    public String getFeatureName() {
        return featureData.getFeatureName();
    }

    public short getFeatureID() {
        return featureData.getFeatureID();
    }

    public FeatureSlot getFeatureSlot() {
        return featureData.getFeatureSlot();
    }

    public MeshHandle getMeshHandle() {
        return featureData.getMeshHandle();
    }
}
