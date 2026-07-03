package application.bootstrap.geometrypipeline.rig;

import engine.root.DataPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class RigData extends DataPackage {

    /*
     * Immutable bone hierarchy for one rig template. Bones are stored in
     * strict parent-before-child order — index 0 is always the root, and
     * every bone's parentIndex is guaranteed to be less than its own index.
     * This lets pose evaluation walk the array once, front to back, with
     * every parent already resolved before its children are reached.
     *
     * bindWorldMatrices and bindWorldInverseMatrices are baked once here,
     * at load time, from the bones' bind-pose position/rotation — identical
     * for every entity sharing this rig, so this is computed per rig, never
     * per entity. Runtime pose evaluation combines a bone's *current* world
     * matrix with bindWorldInverseMatrices[boneIndex] to produce the
     * skinning matrix fed to the GPU — see AnimationStateHandle.
     */

    // Bones
    private final RigBoneStruct[] bones;
    private final Object2IntOpenHashMap<String> boneName2Index;

    // Bind Pose — baked
    private final Matrix4[] bindWorldMatrices;
    private final Matrix4[] bindWorldInverseMatrices;

    // Constructor \\

    public RigData(RigBoneStruct[] bones, Object2IntOpenHashMap<String> boneName2Index) {

        // Bones
        this.bones = bones;
        this.boneName2Index = boneName2Index;

        // Bind Pose — baked
        this.bindWorldMatrices = new Matrix4[bones.length];
        this.bindWorldInverseMatrices = new Matrix4[bones.length];
        bakeBindPose();
    }

    // Bind Pose \\

    private void bakeBindPose() {

        Vector3 unitScale = new Vector3(1f, 1f, 1f);
        Matrix4 local = new Matrix4();
        Matrix4 scratchA = new Matrix4();
        Matrix4 scratchB = new Matrix4();

        for (int i = 0; i < bones.length; i++) {

            RigBoneStruct bone = bones[i];
            RigMathUtility.composeLocal(bone.getPosition(), bone.getRotation(), unitScale, local, scratchA, scratchB);

            Matrix4 world = bone.isRoot()
                    ? new Matrix4(local)
                    : new Matrix4(bindWorldMatrices[bone.getParentIndex()]).multiply(local);

            bindWorldMatrices[i] = world;
            bindWorldInverseMatrices[i] = new Matrix4(world).inverse();
        }
    }

    // Accessible \\

    public RigBoneStruct[] getBones() {
        return bones;
    }

    public int getBoneCount() {
        return bones.length;
    }

    public RigBoneStruct getBone(int boneIndex) {
        return bones[boneIndex];
    }

    public boolean hasBone(String boneName) {
        return boneName2Index.containsKey(boneName);
    }

    public int getBoneIndex(String boneName) {

        if (!boneName2Index.containsKey(boneName))
            throwException("Bone not found in rig: \"" + boneName + "\"");

        return boneName2Index.getInt(boneName);
    }

    public Matrix4 getBindWorldMatrix(int boneIndex) {
        return bindWorldMatrices[boneIndex];
    }

    public Matrix4 getBindWorldInverseMatrix(int boneIndex) {
        return bindWorldInverseMatrices[boneIndex];
    }
}