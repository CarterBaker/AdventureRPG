package application.bootstrap.entitypipeline.featuremanager;

import java.io.File;

import application.bootstrap.entitypipeline.feature.FeatureData;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class FeatureBuilder extends BuilderPackage {

    /*
     * Parses one feature ARPG file into a FeatureData and wraps it in a
     * FeatureHandle. "slot" decides what the file must declare: mesh slots
     * a rigged "mesh", texture slots a "texture" tile, and the head also a
     * "face" tile its front face is mapped to. Compatibility with any one
     * character's rig and texture array is checked later, by AppearanceData,
     * when a feature is actually worn. Bootstrap-only.
     */

    // Internal
    private FeatureManager featureManager;
    private MeshManager meshManager;
    private TextureManager textureManager;

    // Base \\

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
        this.meshManager = get(MeshManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Build \\

    FeatureHandle build(File file, String featureName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short featureID = featureManager.registerFeatureName(featureName);
        FeatureSlot featureSlot = parseSlot(arpg, file);

        MeshHandle meshHandle = featureSlot.isMeshSlot() ? parseMesh(arpg, file) : null;
        TextureHandle textureHandle = featureSlot.isMeshSlot() ? null : parseTexture(arpg, "texture");
        TextureHandle faceTextureHandle = featureSlot.isFaceSlot() ? parseTexture(arpg, "face") : null;

        FeatureData featureData = new FeatureData(
                featureName,
                featureID,
                featureSlot,
                meshHandle,
                textureHandle,
                faceTextureHandle);

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

    private TextureHandle parseTexture(ArpgObjectStruct arpg, String key) {
        return textureManager.getTextureHandleFromTextureName(ArpgUtility.validateString(arpg, key));
    }
}
