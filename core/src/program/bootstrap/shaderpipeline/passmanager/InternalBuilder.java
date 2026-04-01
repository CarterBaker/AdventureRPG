package program.bootstrap.shaderpipeline.passmanager;

import java.io.File;

import com.google.gson.JsonObject;
import program.bootstrap.geometrypipeline.mesh.MeshHandle;
import program.bootstrap.geometrypipeline.meshmanager.MeshManager;
import program.bootstrap.geometrypipeline.model.ModelInstance;
import program.bootstrap.shaderpipeline.material.MaterialInstance;
import program.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import program.bootstrap.shaderpipeline.pass.PassData;
import program.bootstrap.shaderpipeline.pass.PassHandle;
import program.core.engine.BuilderPackage;
import program.core.util.JsonUtility;
import program.core.util.RegistryUtility;

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