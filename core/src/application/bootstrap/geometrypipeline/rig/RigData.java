package application.bootstrap.geometrypipeline.rig;

import engine.root.DataPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class RigData extends DataPackage {

    /*
     * Immutable bone hierarchy for one rig under its registered name, stored
     * parent before child so poses evaluate in one pass, with the IK handles
     * its tools pose limbs and digits through. Bind-pose world matrices and
     * their inverses are baked once per rig whenever it is built.
     */

    // Identity
    private final String rigName;

    // Bones
    private final RigBoneStruct[] bones;
    private final Object2IntOpenHashMap<String> boneName2Index;

    // IK
    private final RigIkChainStruct[] ikChains;

    // Bind Pose — baked
    private final Matrix4[] bindWorldMatrices;
    private final Matrix4[] bindWorldInverseMatrices;

    // Constructor \\

    public RigData(
            String rigName,
            RigBoneStruct[] bones,
            Object2IntOpenHashMap<String> boneName2Index,
            RigIkChainStruct[] ikChains) {

        // Identity
        this.rigName = rigName;

        // Bones
        this.bones = bones;
        this.boneName2Index = boneName2Index;

        // IK
        this.ikChains = ikChains;

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

    // Structure \\

    // True when another rig holds the same bones by name, in the same order, under the same parents
    public boolean hasSameStructure(RigData other) {

        if (other.bones.length != bones.length)
            return false;

        for (int i = 0; i < bones.length; i++)
            if (!bones[i].getName().equals(other.bones[i].getName())
                    || bones[i].getParentIndex() != other.bones[i].getParentIndex())
                return false;

        return true;
    }

    // Accessible \\

    public String getRigName() {
        return rigName;
    }

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

    public int getIkChainCount() {
        return ikChains.length;
    }

    public RigIkChainStruct getIkChain(int chainIndex) {
        return ikChains[chainIndex];
    }

    // The IK handle whose end is a bone, null when none ends there
    public RigIkChainStruct findIkChainByEnd(int boneIndex) {

        for (RigIkChainStruct chain : ikChains)
            if (chain.getEndBone() == boneIndex)
                return chain;

        return null;
    }
}
