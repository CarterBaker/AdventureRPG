package application.bootstrap.geometrypipeline.skinnedbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.matrices.Matrix4;

public class SkinnedBufferInstance extends InstancePackage {

    /*
     * One instanced skinned draw per rigged mesh. Cleared at the start of each
     * frame's gather, then addInstance() adds each visible entity's model
     * matrix and its palette row — skinning matrices, appearance and morph
     * weights. Nothing persists across frames.
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

    public int getPaletteTexture() {
        return skinnedBufferData.getPaletteTexture();
    }

    public int getAppearanceBase() {
        return skinnedBufferData.getAppearanceBase();
    }

    public int getMorphBase() {
        return skinnedBufferData.getMorphBase();
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

    // Empties the draw for a new frame, laying its palette row out again if its mesh's morphs changed
    public void clear() {

        skinnedBufferData.setInstanceCount(0);

        if (!skinnedBufferData.matchesMesh())
            skinnedBufferData.relayout();
    }

    public int addInstance(
            Matrix4 modelMatrix,
            SkinnedAppearanceStruct appearance,
            Matrix4[] skinningMatrices) {

        int index = skinnedBufferData.getInstanceCount();

        if (index >= skinnedBufferData.getMaxInstances())
            grow();

        System.arraycopy(
                modelMatrix.val, 0,
                skinnedBufferData.getInstanceData(), index * EngineSetting.SKINNED_INSTANCE_FLOATS,
                EngineSetting.SKINNED_INSTANCE_MODEL_FLOATS);
        writePaletteRow(index, appearance, skinningMatrices);

        skinnedBufferData.setInstanceCount(index + 1);

        return index;
    }

    // Write \\

    // The instance's bones, then its appearance, then its morph weights
    private void writePaletteRow(int index, SkinnedAppearanceStruct appearance, Matrix4[] skinningMatrices) {

        float[] paletteData = skinnedBufferData.getPaletteData();
        int base = index * skinnedBufferData.getRowFloats();
        int boneCount = skinningMatrices.length;

        appearance.write(paletteData, base
                + skinnedBufferData.getAppearanceBase() * EngineSetting.COLOR_CHANNEL_COUNT);
        appearance.writeMorphWeights(getMeshHandle(), paletteData, base
                + skinnedBufferData.getMorphBase() * EngineSetting.COLOR_CHANNEL_COUNT);

        for (int bone = 0; bone < boneCount; bone++) {

            Matrix4 m = skinningMatrices[bone];
            int boneBase = base + bone * EngineSetting.SKINNED_BONE_TEXELS_PER_BONE * EngineSetting.COLOR_CHANNEL_COUNT;

            // Row 0
            paletteData[boneBase] = m.getM00();
            paletteData[boneBase + 1] = m.getM01();
            paletteData[boneBase + 2] = m.getM02();
            paletteData[boneBase + 3] = m.getM03();

            // Row 1
            paletteData[boneBase + 4] = m.getM10();
            paletteData[boneBase + 5] = m.getM11();
            paletteData[boneBase + 6] = m.getM12();
            paletteData[boneBase + 7] = m.getM13();

            // Row 2
            paletteData[boneBase + 8] = m.getM20();
            paletteData[boneBase + 9] = m.getM21();
            paletteData[boneBase + 10] = m.getM22();
            paletteData[boneBase + 11] = m.getM23();
        }
    }

    // Growth \\

    private void grow() {

        int newMax = skinnedBufferData.getMaxInstances() * 2;

        float[] grownInstances = new float[newMax * EngineSetting.SKINNED_INSTANCE_FLOATS];
        System.arraycopy(
                skinnedBufferData.getInstanceData(), 0,
                grownInstances, 0,
                skinnedBufferData.getInstanceData().length);

        float[] grownPalette = new float[newMax * skinnedBufferData.getRowFloats()];
        System.arraycopy(
                skinnedBufferData.getPaletteData(), 0,
                grownPalette, 0,
                skinnedBufferData.getPaletteData().length);

        skinnedBufferData.setMaxInstances(newMax);
        skinnedBufferData.setInstanceData(grownInstances);
        skinnedBufferData.setPaletteData(grownPalette);
        skinnedBufferData.setNeedsGpuRealloc(true);
    }

    public void clearNeedsGpuRealloc() {
        skinnedBufferData.setNeedsGpuRealloc(false);
    }
}