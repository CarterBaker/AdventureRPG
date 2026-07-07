package application.bootstrap.weatherpipeline.cloudvolumemanager;

import engine.root.ManagerPackage;

public class CloudVolumeManager extends ManagerPackage {

    /*
     * Owns the volumetric cloud box for the engine lifetime. The mesh
     * itself is built once, at bootstrap, by CloudVolumeMeshBranch — the
     * box's shape never changes, only its per-frame world position and the
     * per-direction weather/wind data pushed into its material's uniforms.
     *
     * CloudVolumeSampleBranch (Stage 5b) and CloudVolumeSystem (Stage 5c)
     * attach to this manager next.
     */

    // Branches
    private CloudVolumeMeshBranch meshBranch;

    // Base \\

    @Override
    protected void create() {
        this.meshBranch = create(CloudVolumeMeshBranch.class);
    }

    // Accessible \\

    CloudVolumeMeshBranch getMeshBranch() {
        return meshBranch;
    }
}