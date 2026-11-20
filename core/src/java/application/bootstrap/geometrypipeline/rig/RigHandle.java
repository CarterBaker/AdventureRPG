package application.bootstrap.geometrypipeline.rig;

import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBoneMaskStruct;
import engine.root.HandlePackage;
import engine.util.mathematics.matrices.Matrix4;

public class RigHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded rig template. Registered and owned
     * by RigManager for the engine lifetime. Delegates all bone lookups
     * through RigData — used as the shared bind-pose source every
     * AnimationStateHandle evaluates its runtime pose against. Its data is
     * replaced in place when the rig is edited: the bones it was loaded with
     * always kept by name, parent and place, bones after them free to come,
     * go and change, each such change counted in the structure revision so
     * whatever is laid out by the bones lays itself out again.
     */

    // Internal
    private RigData rigData;

    // Structure — how many bones the rig was loaded with, and how many times its bones have changed since
    private int loadedBoneCount;
    private int structureRevision;

    // Constructor \\

    public void constructor(RigData rigData) {

        // Internal
        this.rigData = rigData;

        // Structure
        this.loadedBoneCount = rigData.getBoneCount();
        this.structureRevision = 0;
    }

    // Update \\

    // Takes new data in place, counting a change of structure
    public void replace(RigData rigData) {

        if (!rigData.hasSameStructure(this.rigData))
            this.structureRevision++;

        this.rigData = rigData;
    }

    // Accessible \\

    public RigData getRigData() {
        return rigData;
    }

    public int getLoadedBoneCount() {
        return loadedBoneCount;
    }

    public int getStructureRevision() {
        return structureRevision;
    }

    // Whether a bone was added since the rig was loaded, so nothing loaded from a file names it
    public boolean isAddedBone(int boneIndex) {
        return boneIndex >= loadedBoneCount;
    }

    public String getRigName() {
        return rigData.getRigName();
    }

    public int getBoneCount() {
        return rigData.getBoneCount();
    }

    public RigBoneStruct getBone(int boneIndex) {
        return rigData.getBone(boneIndex);
    }

    public boolean hasBone(String boneName) {
        return rigData.hasBone(boneName);
    }

    public int getBoneIndex(String boneName) {
        return rigData.getBoneIndex(boneName);
    }

    public Matrix4 getBindWorldMatrix(int boneIndex) {
        return rigData.getBindWorldMatrix(boneIndex);
    }

    public Matrix4 getBindWorldInverseMatrix(int boneIndex) {
        return rigData.getBindWorldInverseMatrix(boneIndex);
    }

    public int getIkChainCount() {
        return rigData.getIkChainCount();
    }

    public RigIkChainStruct getIkChain(int chainIndex) {
        return rigData.getIkChain(chainIndex);
    }

    public RigIkChainStruct findIkChainByEnd(int boneIndex) {
        return rigData.findIkChainByEnd(boneIndex);
    }

    public int getSpringChainCount() {
        return rigData.getSpringChainCount();
    }

    public RigSpringChainStruct getSpringChain(int chainIndex) {
        return rigData.getSpringChain(chainIndex);
    }

    public int getSpringChainIndex(int boneIndex) {
        return rigData.getSpringChainIndex(boneIndex);
    }

    public boolean isSpringBone(int boneIndex) {
        return rigData.isSpringBone(boneIndex);
    }

    public int[] getSpringCarriedBones() {
        return rigData.getSpringCarriedBones();
    }

    // Every bone a spring chain simulates, as a bone mask
    public SkinnedBoneMaskStruct getSpringBoneMask() {
        return rigData.getSpringBoneMask();
    }

    public int getColliderCount() {
        return rigData.getColliderCount();
    }

    public RigColliderStruct getCollider(int colliderIndex) {
        return rigData.getCollider(colliderIndex);
    }

    public int getPartCount() {
        return rigData.getPartCount();
    }

    public RigPartStruct getPart(int partIndex) {
        return rigData.getPart(partIndex);
    }

    public int getPoseCount() {
        return rigData.getPoseCount();
    }

    public String getPoseName(int poseIndex) {
        return rigData.getPoseName(poseIndex);
    }
}
