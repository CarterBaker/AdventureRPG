package application.bootstrap.geometrypipeline.skinnedbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.matrices.Matrix4;

public class SkinnedBufferInstance extends InstancePackage {

    /*
     * Runtime instanced skinned draw unit. One SkinnedBufferInstance exists
     * per distinct rigged MeshHandle — every entity sharing that mesh and
     * rig is submitted into the same instance, drawn in a single
     * glDrawElementsInstanced call. clear() is called once at the start of
     * each frame's gather pass; addInstance() is then called once per
     * visible entity using this mesh. There is no updateInstance() or
     * removeInstance() — unlike CompositeBufferInstance, nothing here
     * persists across frames, since an animated pose is never stable
     * between two frames. SkinnedBufferManager owns GPU upload and drawing.
     */

    // Internal
    private SkinnedBufferData skinnedBufferData;

    // Constructor \\

    public void constructor(SkinnedBufferData skinnedBufferData) {

        // Internal
        this.skinnedBufferData = skinnedBufferData;
    }

    // Accessible \\

    public SkinnedBufferData getSkinnedBufferData() {
        return skinnedBufferData;
    }

    public MeshHandle getMeshHandle() {
        return skinnedBufferData.getMeshHandle();
    }

    public RigHandle getRigHandle() {
        return skinnedBufferData.getRigHandle();
    }

    public int getInstanceVBO() {
        return skinnedBufferData.getInstanceVBO();
    }

    public int getBonePaletteTexture() {
        return skinnedBufferData.getBonePaletteTexture();
    }

    public int getInstanceCount() {
        return skinnedBufferData.getInstanceCount();
    }

    public int getMaxInstances() {
        return skinnedBufferData.getMaxInstances();
    }

    public int getBoneCapacity() {
        return skinnedBufferData.getBoneCapacity();
    }

    public boolean needsGpuRealloc() {
        return skinnedBufferData.isNeedsGpuRealloc();
    }

    public boolean isEmpty() {
        return skinnedBufferData.getInstanceCount() == 0;
    }

    // Frame Lifecycle \\

    public void clear() {
        skinnedBufferData.setInstanceCount(0);
    }

    public int addInstance(Matrix4 modelMatrix, Matrix4[] skinningMatrices) {

        int index = skinnedBufferData.getInstanceCount();

        if (index >= skinnedBufferData.getMaxInstances())
            grow();

        writeModelMatrix(index, modelMatrix);
        writeBoneMatrices(index, skinningMatrices);

        skinnedBufferData.setInstanceCount(index + 1);

        return index;
    }

    // Write \\

    private void writeModelMatrix(int index, Matrix4 modelMatrix) {

        float[] instanceModelData = skinnedBufferData.getInstanceModelData();
        int base = index * EngineSetting.SKINNED_INSTANCE_MODEL_FLOATS;

        System.arraycopy(modelMatrix.val, 0, instanceModelData, base, EngineSetting.SKINNED_INSTANCE_MODEL_FLOATS);
    }

    private void writeBoneMatrices(int index, Matrix4[] skinningMatrices) {

        float[] boneMatrixData = skinnedBufferData.getBoneMatrixData();
        int floatsPerInstance = skinnedBufferData.getBoneCapacity()
                * EngineSetting.SKINNED_BONE_TEXELS_PER_BONE * 4;
        int base = index * floatsPerInstance;
        int boneCount = skinningMatrices.length;

        for (int bone = 0; bone < boneCount; bone++) {

            Matrix4 m = skinningMatrices[bone];
            int boneBase = base + bone * EngineSetting.SKINNED_BONE_TEXELS_PER_BONE * 4;

            // Row 0
            boneMatrixData[boneBase] = m.getM00();
            boneMatrixData[boneBase + 1] = m.getM01();
            boneMatrixData[boneBase + 2] = m.getM02();
            boneMatrixData[boneBase + 3] = m.getM03();

            // Row 1
            boneMatrixData[boneBase + 4] = m.getM10();
            boneMatrixData[boneBase + 5] = m.getM11();
            boneMatrixData[boneBase + 6] = m.getM12();
            boneMatrixData[boneBase + 7] = m.getM13();

            // Row 2
            boneMatrixData[boneBase + 8] = m.getM20();
            boneMatrixData[boneBase + 9] = m.getM21();
            boneMatrixData[boneBase + 10] = m.getM22();
            boneMatrixData[boneBase + 11] = m.getM23();
        }
    }

    // Growth \\

    private void grow() {

        int newMax = skinnedBufferData.getMaxInstances() * 2;

        float[] grownModel = new float[newMax * EngineSetting.SKINNED_INSTANCE_MODEL_FLOATS];
        System.arraycopy(
                skinnedBufferData.getInstanceModelData(), 0,
                grownModel, 0,
                skinnedBufferData.getInstanceModelData().length);

        int boneFloatsPerInstance = skinnedBufferData.getBoneCapacity()
                * EngineSetting.SKINNED_BONE_TEXELS_PER_BONE * 4;
        float[] grownBones = new float[newMax * boneFloatsPerInstance];
        System.arraycopy(
                skinnedBufferData.getBoneMatrixData(), 0,
                grownBones, 0,
                skinnedBufferData.getBoneMatrixData().length);

        skinnedBufferData.setMaxInstances(newMax);
        skinnedBufferData.setInstanceModelData(grownModel);
        skinnedBufferData.setBoneMatrixData(grownBones);
        skinnedBufferData.setNeedsGpuRealloc(true);
    }

    public void clearNeedsGpuRealloc() {
        skinnedBufferData.setNeedsGpuRealloc(false);
    }
}