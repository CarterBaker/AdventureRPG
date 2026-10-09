package application.bootstrap.shaderpipeline.passmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.pass.PassData;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class PassBuilder extends BuilderPackage {

    /*
     * Constructs PassHandles from ARPG descriptors during bootstrap. Resolves
     * material and mesh references by name, clones the material, and builds
     * the PassData and ModelInstance before wrapping in a handle.
     */

    // Internal
    private PassManager passManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;

    // Base \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    // Build \\

    PassHandle build(File file, String passName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        int materialID = materialManager.getMaterialIDFromMaterialName(
                ArpgUtility.validateString(arpg, "material"));
        MaterialInstance material = materialManager.cloneMaterial(materialID);

        MeshHandle meshHandle = getMeshHandleFromArpg(arpg);

        int passID = passManager.registerPassName(passName);

        ModelInstance modelInstance = create(ModelInstance.class);
        modelInstance.constructor(meshHandle.getMeshData(), material);

        PassData data = new PassData(passName, passID, meshHandle, material, modelInstance);
        PassHandle handle = create(PassHandle.class);
        handle.constructor(data);

        return handle;
    }

    private MeshHandle getMeshHandleFromArpg(ArpgObjectStruct arpg) {
        String meshName = ArpgUtility.getString(arpg, "mesh", "util/PlanarPass");
        return meshManager.getMeshHandleFromMeshName(meshName);
    }
}