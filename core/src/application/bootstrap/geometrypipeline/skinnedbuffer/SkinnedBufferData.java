package application.bootstrap.geometrypipeline.skinnedbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.DataPackage;
import engine.root.EngineSetting;

public class SkinnedBufferData extends DataPackage {

    /*
     * Mutable state for one instanced skinned draw: GPU handles, the rigged
     * mesh, per-instance rows and bone palettes, and the realloc flag. Bone
     * capacity matches the mesh's own rig. Every instance is resubmitted each
     * frame, so it uploads whenever it has instances.
     */

    // GPU Handles
    private int instanceVBO;
    private int bonePaletteTexture;

    // Mesh / Rig
    private final MeshHandle meshHandle;
    private final RigHandle rigHandle;
    private final int boneCapacity;

    // Instance Data — one SKINNED_INSTANCE_FLOATS row per instance: the
    // model mat4 followed by the appearance row
    private float[] instanceData;

    // Instance Data — bone palettes, one row of boneCapacity * 3 texels
    // (12 floats) per instance
    private float[] boneMatrixData;

    // Counts
    private int instanceCount;
    private int maxInstances;

    // Realloc
    private boolean needsGpuRealloc;

    // Constructor \\

    public SkinnedBufferData(MeshHandle meshHandle) {

        // Mesh / Rig
        this.meshHandle = meshHandle;
        this.rigHandle = meshHandle.getRigHandle();
        this.boneCapacity = rigHandle.getBoneCount();

        // Counts
        this.maxInstances = EngineSetting.SKINNED_INSTANCE_INITIAL_CAPACITY;
        this.instanceCount = 0;

        // Instance Data
        this.instanceData = new float[maxInstances * EngineSetting.SKINNED_INSTANCE_FLOATS];
        this.boneMatrixData = new float[maxInstances * boneCapacity * EngineSetting.SKINNED_BONE_TEXELS_PER_BONE * 4];

        // Realloc
        this.needsGpuRealloc = false;
    }

    // Accessible \\

    public int getInstanceVBO() {
        return instanceVBO;
    }

    public void setInstanceVBO(int instanceVBO) {
        this.instanceVBO = instanceVBO;
    }

    public int getBonePaletteTexture() {
        return bonePaletteTexture;
    }

    public void setBonePaletteTexture(int bonePaletteTexture) {
        this.bonePaletteTexture = bonePaletteTexture;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public int getBoneCapacity() {
        return boneCapacity;
    }

    public float[] getInstanceData() {
        return instanceData;
    }

    public void setInstanceData(float[] instanceData) {
        this.instanceData = instanceData;
    }

    public float[] getBoneMatrixData() {
        return boneMatrixData;
    }

    public void setBoneMatrixData(float[] boneMatrixData) {
        this.boneMatrixData = boneMatrixData;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public int getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    public boolean isNeedsGpuRealloc() {
        return needsGpuRealloc;
    }

    public void setNeedsGpuRealloc(boolean needsGpuRealloc) {
        this.needsGpuRealloc = needsGpuRealloc;
    }
}