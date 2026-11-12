package application.bootstrap.entitypipeline.featuremanager;

import java.io.File;

import application.bootstrap.entitypipeline.feature.FeatureData;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class FeatureBuilder extends BuilderPackage {

    /*
     * Parses one feature ARPG file into a FeatureData and wraps it in a
     * FeatureHandle: the "slot" it is worn in, the rigged "mesh" drawn for it
     * and the optional "material" it is drawn with, its own clone. Whether it fits any one character's rig is checked later, by
     * AppearanceData, when a feature is actually worn. Bootstrap-only.
     */

    // Internal
    private FeatureManager featureManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;

    // Base \\

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    // Build \\

    FeatureHandle build(File file, String featureName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short featureID = featureManager.registerFeatureName(featureName);
        FeatureSlot featureSlot = parseSlot(arpg, file);

        FeatureData featureData = new FeatureData(
                featureName,
                featureID,
                featureSlot,
                parseMesh(arpg, file),
                parseMaterial(arpg));

        FeatureHandle handle = create(FeatureHandle.class);
        handle.constructor(featureData);

        return handle;
    }

    // Parse \\

    private FeatureSlot parseSlot(ArpgObjectStruct arpg, File file) {

        String slotName = ArpgUtility.validateString(arpg, "slot");

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            if (featureSlot.name().equalsIgnoreCase(slotName))
                return featureSlot;

        return throwException("Unknown feature slot \"" + slotName + "\" in file: " + file.getName());
    }

    private MeshHandle parseMesh(ArpgObjectStruct arpg, File file) {

        String meshName = ArpgUtility.validateString(arpg, "mesh");
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshName(meshName);

        if (!meshHandle.hasRig())
            throwException("Feature mesh \"" + meshName
                    + "\" has no rig — mesh features are skinned onto the character's rig. File: " + file.getName());

        return meshHandle;
    }

    private MaterialInstance parseMaterial(ArpgObjectStruct arpg) {

        if (!ArpgUtility.hasString(arpg, "material"))
            return null;

        return materialManager.cloneMaterial(arpg.get("material").getAsString());
    }
}
