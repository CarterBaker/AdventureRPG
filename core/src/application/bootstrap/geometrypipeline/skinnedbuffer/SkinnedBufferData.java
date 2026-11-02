package application.bootstrap.geometrypipeline.skinnedbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.DataPackage;
import engine.root.EngineSetting;

public class SkinnedBufferData extends DataPackage {

    /*
     * Mutable state for one instanced skinned draw: GPU handles, the rigged
     * mesh, per-instance model rows and palette rows, and the realloc flag. A
     * palette row holds the mesh's bones (SKINNED_BONE_TEXELS_PER_BONE texels
     * each), then the appearance texels, then the mesh's morph weights four
     * to a texel. Every instance is resubmitted each frame, so it uploads
     * whenever it has instances.
     */

    // GPU Handles
    private int instanceVBO;
    private int paletteTexture;

    // Mesh / Rig
    private final MeshHandle meshHandle;
    private final RigHandle rigHandle;
    private final int boneCapacity;

    // Palette Layout — texel offsets inside one row, and its width
    private final int appearanceBase;
    private final int morphBase;
    private int morphCount;
    private int rowTexels;

    // Instance Data — one SKINNED_INSTANCE_FLOATS model row per instance
    private float[] instanceData;

    // Instance Data — one palette row of rowTexels texels per instance
    private float[] paletteData;

    // Counts
    private int instanceCount;
    private int maxInstances;

    // Realloc
    private boolean needsGpuRealloc;
    private boolean needsPaletteRealloc;

    // Constructor \\

    public SkinnedBufferData(MeshHandle meshHandle) {

        // Mesh / Rig
        this.meshHandle = meshHandle;
        this.rigHandle = meshHandle.getRigHandle();
        this.boneCapacity = rigHandle.getBoneCount();

        // Palette Layout
        this.appearanceBase = boneCapacity * EngineSetting.SKINNED_BONE_TEXELS_PER_BONE;
        this.morphBase = appearanceBase + SkinnedAppearanceStruct.getTexelCount();
        resolveRowTexels();

        // Counts
        this.maxInstances = EngineSetting.SKINNED_INSTANCE_INITIAL_CAPACITY;
        this.instanceCount = 0;

        // Instance Data
        this.instanceData = new float[maxInstances * EngineSetting.SKINNED_INSTANCE_FLOATS];
        this.paletteData = new float[maxInstances * getRowFloats()];

        // Realloc
        this.needsGpuRealloc = false;
    }

    // Layout \\

    // Whether the mesh still holds the morphs the row was laid out for; an editor can add or remove them
    public boolean matchesMesh() {
        return morphCount == meshHandle.getMorphCount();
    }

    // Lays the row out again for the mesh's morphs, its storage reallocated empty
    public void relayout() {

        resolveRowTexels();
        this.paletteData = new float[maxInstances * getRowFloats()];
        this.needsPaletteRealloc = true;
    }

    private void resolveRowTexels() {
        this.morphCount = meshHandle.getMorphCount();
        this.rowTexels = morphBase + (morphCount + EngineSetting.SKINNED_MORPH_WEIGHTS_PER_TEXEL - 1)
                / EngineSetting.SKINNED_MORPH_WEIGHTS_PER_TEXEL;
    }

    // Accessible \\

    public int getInstanceVBO() {
        return instanceVBO;
    }

    public void setInstanceVBO(int instanceVBO) {
        this.instanceVBO = instanceVBO;
    }

    public int getPaletteTexture() {
        return paletteTexture;
    }

    public void setPaletteTexture(int paletteTexture) {
        this.paletteTexture = paletteTexture;
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

    public int getAppearanceBase() {
        return appearanceBase;
    }

    public int getMorphBase() {
        return morphBase;
    }

    public int getRowTexels() {
        return rowTexels;
    }

    public int getRowFloats() {
        return rowTexels * EngineSetting.COLOR_CHANNEL_COUNT;
    }

    public float[] getInstanceData() {
        return instanceData;
    }

    public void setInstanceData(float[] instanceData) {
        this.instanceData = instanceData;
    }

    public float[] getPaletteData() {
        return paletteData;
    }

    public void setPaletteData(float[] paletteData) {
        this.paletteData = paletteData;
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

    public boolean isNeedsPaletteRealloc() {
        return needsPaletteRealloc;
    }

    public void setNeedsPaletteRealloc(boolean needsPaletteRealloc) {
        this.needsPaletteRealloc = needsPaletteRealloc;
    }
}
