package application.bootstrap.entitypipeline.featuremanager;

import com.google.gson.JsonObject;
import java.io.File;

import application.bootstrap.entitypipeline.feature.FeatureData;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.root.BuilderPackage;
import engine.util.io.JsonUtility;
import engine.util.registry.RegistryUtility;

class FeatureBuilder extends BuilderPackage {

    /*
     * Parses one feature JSON file into a FeatureData and wraps it in a
     * FeatureHandle. "slot" decides what the file must declare: mesh slots
     * a rigged "mesh", texture slots a "texture" tile, and the head also a
     * "face" tile its front face is mapped to. Compatibility with any one
     * character's rig and texture array is checked later, by AppearanceData,
     * when a feature is actually worn. Bootstrap-only.
     */

    // Internal
    private MeshManager meshManager;
    private TextureManager textureManager;

    // Base \\

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Build \\

    FeatureHandle build(File file, String featureName) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        short featureID = RegistryUtility.toShortID(featureName);
        FeatureSlot featureSlot = parseSlot(json, file);

        MeshHandle meshHandle = featureSlot.isMeshSlot() ? parseMesh(json, file) : null;
        TextureHandle textureHandle = featureSlot.isMeshSlot() ? null : parseTexture(json, "texture");
        TextureHandle faceTextureHandle = featureSlot.isFaceSlot() ? parseTexture(json, "face") : null;

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

    private FeatureSlot parseSlot(JsonObject json, File file) {

        String slotName = JsonUtility.validateString(json, "slot");

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            if (featureSlot.name().equalsIgnoreCase(slotName))
                return featureSlot;

        return throwException("Unknown feature slot \"" + slotName + "\" in file: " + file.getName());
    }

    private MeshHandle parseMesh(JsonObject json, File file) {

        String meshName = JsonUtility.validateString(json, "mesh");
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshName(meshName);

        if (!meshHandle.hasRig())
            throwException("Feature mesh \"" + meshName
                    + "\" has no rig — mesh features are skinned onto the character's rig. File: " + file.getName());

        return meshHandle;
    }

    private TextureHandle parseTexture(JsonObject json, String key) {
        return textureManager.getTextureHandleFromTextureName(JsonUtility.validateString(json, key));
    }
}
