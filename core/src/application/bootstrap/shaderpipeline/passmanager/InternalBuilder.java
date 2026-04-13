package application.bootstrap.shaderpipeline.passmanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.pass.PassData;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import engine.root.BuilderPackage;
import engine.util.JsonUtility;
import engine.util.RegistryUtility;

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