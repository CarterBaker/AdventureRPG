package com.internal.bootstrap.shaderpipeline.passmanager;

import java.io.File;

import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.pass.PassData;
import com.internal.bootstrap.shaderpipeline.pass.PassHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.RegistryUtility;

/*
 * Constructs PassHandles from JSON descriptors during bootstrap. Resolves
 * material and mesh references by name, clones the material, and builds
 * the PassData and ModelInstance before wrapping in a handle.
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

    PassHandle build(File file, String passName) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        int materialID = materialManager.getMaterialIDFromMaterialName(
                JsonUtility.validateString(json, "material"));
        MaterialInstance material = materialManager.cloneMaterial(materialID);

        MeshHandle meshHandle = getMeshHandleFromJson(json);

        int passID = RegistryUtility.toIntID(passName);

        ModelInstance modelInstance = create(ModelInstance.class);
        modelInstance.constructor(meshHandle.getMeshData(), material);

        PassData data = new PassData(passName, passID, meshHandle, material, modelInstance);
        PassHandle handle = create(PassHandle.class);
        handle.constructor(data);

        return handle;
    }

    private MeshHandle getMeshHandleFromJson(JsonObject json) {
        String meshName = JsonUtility.getString(json, "mesh", "util/PlanarPass");
        return meshManager.getMeshHandleFromMeshName(meshName);
    }
}