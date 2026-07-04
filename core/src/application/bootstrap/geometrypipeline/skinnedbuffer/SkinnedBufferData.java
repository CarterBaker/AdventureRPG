package application.bootstrap.geometrypipeline.skinnedbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.DataPackage;
import engine.root.EngineSetting;

public class SkinnedBufferData extends DataPackage {

    /*
     * Holds all mutable state for one instanced skinned draw — GPU handles,
     * the rigged mesh it draws, CPU-side per-instance model matrices and
     * per-instance bone palettes, and the realloc flag. All mutation is
     * driven by SkinnedBufferInstance. boneCapacity is fixed to this mesh's
     * own rig's exact bone count — not a shared global maximum — so no
     * skinned buffer ever wastes a single float on bones it doesn't have.
     * Unlike CompositeBufferData, there is no cpu/uploaded version pair —
     * every instance here is re-submitted in full every single frame by
     * EntityRenderSystem (an animated pose is never the same twice), so "is
     * this stale" is never a meaningful question; upload happens
     * unconditionally whenever instanceCount > 0.
     */

    // GPU Handles
    private int instanceVBO;
    private int bonePaletteTexture;

    // Mesh / Rig
    private final MeshHandle meshHandle;
    private final RigHandle rigHandle;
    private final int boneCapacity;

    // Instance Data — model matrices, one mat4 (16 floats) per instance
    private float[] instanceModelData;

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
        this.instanceModelData = new float[maxInstances * EngineSetting.SKINNED_INSTANCE_MODEL_FLOATS];
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

    public float[] getInstanceModelData() {
        return instanceModelData;
    }

    public void setInstanceModelData(float[] instanceModelData) {
        this.instanceModelData = instanceModelData;
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