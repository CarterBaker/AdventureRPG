package application.bootstrap.entitypipeline.feature;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import engine.root.DataPackage;

public class FeatureData extends DataPackage {

    /*
     * Immutable definition of one selectable modelled part: the slot it is
     * worn in, the rigged mesh drawn for it and the material it is drawn
     * with, null to take the body's. Owned by FeatureHandle in
     * FeatureManager's palette for the engine lifetime.
     */

    // Identity
    private final String featureName;
    private final short featureID;
    private final FeatureSlot featureSlot;

    // Content
    private final MeshHandle meshHandle;
    private final MaterialInstance material;

    // Constructor \\

    public FeatureData(
            String featureName,
            short featureID,
            FeatureSlot featureSlot,
            MeshHandle meshHandle,
            MaterialInstance material) {

        // Identity
        this.featureName = featureName;
        this.featureID = featureID;
        this.featureSlot = featureSlot;

        // Content
        this.meshHandle = meshHandle;
        this.material = material;
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

    public boolean hasMaterial() {
        return material != null;
    }

    public MaterialInstance getMaterial() {
        return material;
    }
}
