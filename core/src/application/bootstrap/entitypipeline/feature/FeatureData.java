package application.bootstrap.entitypipeline.feature;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.root.DataPackage;

public class FeatureData extends DataPackage {

    /*
     * Immutable definition of one selectable modelled part: the slot it is
     * worn in and the rigged mesh drawn for it. Owned by FeatureHandle in
     * FeatureManager's palette for the engine lifetime.
     */

    // Identity
    private final String featureName;
    private final short featureID;
    private final FeatureSlot featureSlot;

    // Content
    private final MeshHandle meshHandle;

    // Constructor \\

    public FeatureData(
            String featureName,
            short featureID,
            FeatureSlot featureSlot,
            MeshHandle meshHandle) {

        // Identity
        this.featureName = featureName;
        this.featureID = featureID;
        this.featureSlot = featureSlot;

        // Content
        this.meshHandle = meshHandle;
    }

    // Accessible \\

    public String getFeatureName() {
        return featureName;
    }

    public short getFeatureID() {
        return featureID;
    }

    public FeatureSlot getFeatureSlot() {
        return featureSlot;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }
}
