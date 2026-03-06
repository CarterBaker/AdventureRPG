package com.internal.bootstrap.shaderpipeline.passmanager;

import java.io.File;

import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.pass.PassHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;

/*
 * Constructs PassHandle objects from JSON descriptors during bootstrap.
 * Resolves material and mesh references by name. Released after bootstrap completes.
 */
class InternalBuilder extends BuilderPackage {

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;

    // Base \\

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    // Build \\

    PassHandle build(File file, String passName, int passID) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        int materialID = materialManager.getMaterialIDFromMaterialName(
                JsonUtility.validateString(json, "material"));
        MaterialInstance material = materialManager.cloneMaterial(materialID);
        MeshHandle meshHandle = getMeshHandleFromJson(json);

        PassHandle pass = create(PassHandle.class);
        pass.constructor(passName, passID, material, meshHandle);
        return pass;
    }

    private MeshHandle getMeshHandleFromJson(JsonObject json) {
        String meshName = JsonUtility.getString(json, "mesh", "util/PlanarPass");
        int meshID = meshManager.getMeshHandleIDFromMeshName(meshName);
        return meshManager.getMeshHandleFromMeshHandleID(meshID);
    }
}