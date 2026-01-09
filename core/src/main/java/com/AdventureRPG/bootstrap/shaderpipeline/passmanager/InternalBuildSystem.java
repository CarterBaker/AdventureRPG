package com.AdventureRPG.bootstrap.shaderpipeline.passmanager;

import java.io.File;

import com.AdventureRPG.bootstrap.geometrypipeline.modelmanager.MeshHandle;
import com.AdventureRPG.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.AdventureRPG.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.JsonUtility;
import com.google.gson.JsonObject;

class InternalBuildSystem extends SystemPackage {

    // Internal
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private MeshHandle processingTriangle;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    // Pass Management \\

    void assignMeshData() {

        // Internal
        int meshID = modelManager.getMeshHandleIDFromMeshName("util/PlanarPass");
        this.processingTriangle = modelManager.getMeshHandleFromMeshHandleID(meshID);
    }

    PassHandle buildPass(File root, File file, int passID) {

        String passName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        JsonObject json = JsonUtility.loadJsonObject(file);

        // Get material
        MaterialHandle material = materialManager.getMaterialFromMaterialID(getPassID(json));

        // Get mesh dynamically from JSON, fallback to default
        MeshHandle meshHandle = getMeshHandleFromJson(json);

        // Build PassHandle
        PassHandle processingPassHandle = create(PassHandle.class);
        processingPassHandle.constructor(
                passName,
                passID,
                material,
                meshHandle);

        return processingPassHandle;
    }

    private int getPassID(JsonObject json) {
        String passName = JsonUtility.validateString(json, "material");
        return materialManager.getMaterialIDFromMaterialName(passName);
    }

    private MeshHandle getMeshHandleFromJson(JsonObject json) {
        String meshName = "util/PlanarPass"; // default fallback
        if (json.has("mesh") && !json.get("mesh").isJsonNull()) {
            meshName = json.get("mesh").getAsString();
        }

        int meshID = modelManager.getMeshHandleIDFromMeshName(meshName);
        return modelManager.getMeshHandleFromMeshHandleID(meshID);
    }
}
