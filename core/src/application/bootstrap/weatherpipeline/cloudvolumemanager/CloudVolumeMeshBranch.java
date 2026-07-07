package application.bootstrap.weatherpipeline.cloudvolumemanager;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CloudVolumeMeshBranch extends BranchPackage {

    /*
     * Resolves the cloud volume's box model once at awake(). The box itself
     * is fully authored data — see CloudVolumeBox.json — loaded through the
     * same MeshLoader bootstrap scan every other mesh in the project goes
     * through. This branch does no geometry work at all; it just asks for
     * the mesh by name and pairs it with an independent material instance,
     * the same two-call shape ModelManager already exposes for any other
     * real object (MeshHandle + MaterialInstance -> ModelInstance).
     */

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;

    // Built once at awake()
    private ModelInstance cloudVolumeModel;

    // Internal \\

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
    }

    @Override
    protected void awake() {

        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshName(EngineSetting.CLOUD_VOLUME_MESH_NAME);

        int materialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.CLOUD_VOLUME_MATERIAL_NAME);
        MaterialInstance material = materialManager.cloneMaterial(materialID);

        this.cloudVolumeModel = modelManager.createModel(meshHandle, material);
    }

    // Accessible \\

    ModelInstance getCloudVolumeModel() {
        return cloudVolumeModel;
    }
}