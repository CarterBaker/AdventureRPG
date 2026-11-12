package application.bootstrap.geometrypipeline.rig;

import java.util.Arrays;

import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class RigData extends DataPackage {

    /*
     * Immutable bone hierarchy for one rig under its registered name, stored
     * parent before child so poses evaluate in one pass, with the IK handles
     * its tools pose limbs and digits through and the spring chains the
     * runtime simulates in place of any key — every bone a chain turns, and
     * the chain each bone belongs to, found once here. It names the bones
     * each body part holds and the clips its editors strike as poses.
     * Bind-pose world matrices and their inverses are baked once per rig
     * whenever it is built.
     */

    // Identity
    private final String rigName;

    // Bones
    private final RigBoneStruct[] bones;
    private final Object2IntOpenHashMap<String> boneName2Index;

    // IK
    private final RigIkChainStruct[] ikChains;

    // Springs — the chains, the chain each bone belongs to, and every bone a chain's turn reaches, parents first
    private final RigSpringChainStruct[] springChains;
    private final int[] bone2SpringChain;
    private final int[] springCarriedBones;

    // Parts — the bones each part names, by RigPart ordinal; the clips struck as editor poses
    private final int[][] part2Bones;
    private final String[] poseNames;

    // Bind Pose — baked
    private final Matrix4[] bindWorldMatrices;
    private final Matrix4[] bindWorldInverseMatrices;

    // Constructor \\

    public RigData(
            String rigName,
            RigBoneStruct[] bones,
            Object2IntOpenHashMap<String> boneName2Index,
            RigIkChainStruct[] ikChains,
            RigSpringChainStruct[] springChains,
            int[][] part2Bones,
            String[] poseNames) {

        // Identity
        this.rigName = rigName;

        // Bones
        this.bones = bones;
        this.boneName2Index = boneName2Index;

        // IK
        this.ikChains = ikChains;

        // Springs
        this.springChains = springChains;
        this.bone2SpringChain = resolveSpringChains(bones.length, springChains);
        this.springCarriedBones = resolveSpringCarried(bones.length, springChains);

        // Parts
        this.part2Bones = part2Bones;
        this.poseNames = poseNames;

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

    // Springs \\

    private static int[] resolveSpringChains(int boneCount, RigSpringChainStruct[] springChains) {

        int[] chains = new int[boneCount];
        Arrays.fill(chains, EngineSetting.INDEX_NOT_FOUND);

        for (int c = 0; c < springChains.length; c++)
            for (int j = 0; j < springChains[c].getBoneCount(); j++)
                chains[springChains[c].getBone(j)] = c;

        return chains;
    }

    // Every bone the turn of some chain's bone carries, in rig order so parents come before children
    private static int[] resolveSpringCarried(int boneCount, RigSpringChainStruct[] springChains) {

        boolean[] carried = new boolean[boneCount];
        int count = 0;

        for (RigSpringChainStruct chain : springChains)
            for (int j = 0; j < chain.getBoneCount(); j++)
                for (int bone : chain.getCarried(j))
                    if (!carried[bone]) {
                        carried[bone] = true;
                        count++;
                    }

        int[] bones = new int[count];
        int next = 0;

        for (int i = 0; i < boneCount; i++)
            if (carried[i])
                bones[next++] = i;

        return bones;
    }

    // Structure \\

    // True when another rig holds the same bones by name, in the same order, under the same parents, and the same
    // spring chains of the same bones, every live pose and simulation being laid out by them
    public boolean hasSameStructure(RigData other) {

        if (other.bones.length != bones.length || other.springChains.length != springChains.length)
            return false;

        for (int i = 0; i < bones.length; i++)
            if (!bones[i].getName().equals(other.bones[i].getName())
                    || bones[i].getParentIndex() != other.bones[i].getParentIndex())
                return false;

        for (int c = 0; c < springChains.length; c++)
            if (!hasSameBones(springChains[c], other.springChains[c]))
                return false;

        return true;
    }

    private static boolean hasSameBones(RigSpringChainStruct a, RigSpringChainStruct b) {

        if (a.getBoneCount() != b.getBoneCount())
            return false;

        for (int j = 0; j < a.getBoneCount(); j++)
            if (a.getBone(j) != b.getBone(j))
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

    public int getSpringChainCount() {
        return springChains.length;
    }

    public RigSpringChainStruct getSpringChain(int chainIndex) {
        return springChains[chainIndex];
    }

    // The spring chain a bone belongs to, INDEX_NOT_FOUND for a bone the runtime never simulates
    public int getSpringChainIndex(int boneIndex) {
        return bone2SpringChain[boneIndex];
    }

    public boolean isSpringBone(int boneIndex) {
        return bone2SpringChain[boneIndex] != EngineSetting.INDEX_NOT_FOUND;
    }

    public int[] getSpringCarriedBones() {
        return springCarriedBones;
    }

    // The IK handle whose end is a bone, null when none ends there
    public RigIkChainStruct findIkChainByEnd(int boneIndex) {

        for (RigIkChainStruct chain : ikChains)
            if (chain.getEndBone() == boneIndex)
                return chain;

        return null;
    }

    // The bones a part names, empty for a part the rig does not name
    public int[] getPartBones(RigPart rigPart) {
        return part2Bones[rigPart.ordinal()];
    }

    public int getPoseCount() {
        return poseNames.length;
    }

    // The clip struck as an editor pose, by its animation name
    public String getPoseName(int poseIndex) {
        return poseNames[poseIndex];
    }
}
