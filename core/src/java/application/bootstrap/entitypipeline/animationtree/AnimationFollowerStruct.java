package application.bootstrap.entitypipeline.animationtree;

import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import engine.root.StructPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class AnimationFollowerStruct extends StructPackage {

    /*
     * The pose of a rig that follows another, as a worn piece's or a hair
     * style's own skeleton follows the body wearing it (the leader pose
     * approach): every bone it shares with its leader stands where the
     * leader's posed bone stands, sized as the leader sizes it, and skins the
     * piece by the follower's own bind; every bone of its own hangs from its
     * parent at its bind place, carried by the leader's sizing of a shared
     * parent, and its spring chains are simulated over the pose in the body's
     * frame, as the leader's are, each running only while the piece weighs
     * onto it. It is laid out for its rig's structure as it stood when made,
     * so a rig whose bones change is followed by a new one. Every array is
     * allocated once, so following never allocates.
     */

    // Rig — and the structure revision the arrays are laid out for
    private final RigHandle rigHandle;
    private final int structureRevision;

    // Pose — per bone, where it stands in the body's model space and what it skins the piece by
    private final Matrix4[] worldMatrices;
    private final Matrix4[] skinningMatrices;

    // Springs — simulated over the pose, stepped by the time each follow hands them
    private final AnimationSpringStruct springs;

    // Scratch
    private final Vector3 positionScratch;
    private final Vector3 unitScale;
    private final Matrix4 localScratch;
    private final Matrix4 scaleScratch;
    private final Matrix4 matrixScratchA;
    private final Matrix4 matrixScratchB;

    // Constructor \\

    public AnimationFollowerStruct(RigHandle rigHandle) {

        int boneCount = rigHandle.getBoneCount();

        // Rig
        this.rigHandle = rigHandle;
        this.structureRevision = rigHandle.getStructureRevision();

        // Pose
        this.worldMatrices = new Matrix4[boneCount];
        this.skinningMatrices = new Matrix4[boneCount];

        for (int i = 0; i < boneCount; i++) {
            worldMatrices[i] = new Matrix4(rigHandle.getBindWorldMatrix(i));
            skinningMatrices[i] = new Matrix4(1f);
        }

        // Springs
        this.springs = new AnimationSpringStruct(rigHandle);

        // Scratch
        this.positionScratch = new Vector3();
        this.unitScale = new Vector3(1f, 1f, 1f);
        this.localScratch = new Matrix4();
        this.scaleScratch = new Matrix4();
        this.matrixScratchA = new Matrix4();
        this.matrixScratchB = new Matrix4();
    }

    // Springs \\

    // How the body stands turned and sized in the world and how far it travelled since the last follow
    public void setFrame(Matrix4 bodyFrame, float travelX, float travelY, float travelZ) {
        springs.setFrame(bodyFrame, travelX, travelY, travelZ);
    }

    // How fast the air moves past the body, in blocks a second along the world's axes
    public void setWind(float x, float y, float z) {
        springs.setWind(x, y, z);
    }

    // Whether a spring chain runs, off for one the piece does not weigh onto
    public void setActive(int chainIndex, boolean active) {
        springs.setActive(chainIndex, active);
    }

    // Every chain started afresh at the next pose, as an editor striking a still pose asks
    public void reset() {
        springs.reset();
    }

    // Follow \\

    // The pose laid over the leader's as it stands, the springs stepped by the time given; none steps them only to
    // place them where the pose holds them
    public void follow(AnimationStateHandle leader, float deltaTime) {

        int sharedCount = rigHandle.getSharedBoneCount();

        for (int i = 0; i < sharedCount; i++) {

            worldMatrices[i].set(leader.getWorldMatrix(i));
            RigMathUtility.setScale(scaleScratch, leader.getBoneProportion(i));
            skinningMatrices[i]
                    .set(worldMatrices[i])
                    .multiply(scaleScratch)
                    .multiply(rigHandle.getBindWorldInverseMatrix(i));
        }

        for (int i = sharedCount; i < rigHandle.getBoneCount(); i++)
            composeOwnBone(leader, i);

        if (!springs.hasChains())
            return;

        springs.capture(worldMatrices);
        springs.step(deltaTime);
        springs.apply(worldMatrices);

        for (int bone : rigHandle.getSpringCarriedBones())
            skinningMatrices[bone].set(worldMatrices[bone]).multiply(rigHandle.getBindWorldInverseMatrix(bone));
    }

    // A bone of the follower's own at its bind place under its parent, carried by the leader's sizing of a parent
    // they share
    private void composeOwnBone(AnimationStateHandle leader, int boneIndex) {

        RigBoneStruct bone = rigHandle.getBone(boneIndex);
        int parentIndex = bone.getParentIndex();

        positionScratch.set(bone.getPosition());

        if (parentIndex < rigHandle.getSharedBoneCount())
            positionScratch.multiply(leader.getBoneProportion(parentIndex));

        RigMathUtility.composeLocal(
                positionScratch, bone.getRotation(), unitScale, localScratch, matrixScratchA, matrixScratchB);
        worldMatrices[boneIndex].set(worldMatrices[parentIndex]).multiply(localScratch);
        skinningMatrices[boneIndex]
                .set(worldMatrices[boneIndex])
                .multiply(rigHandle.getBindWorldInverseMatrix(boneIndex));
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    // True while the follower is laid out for the rig as it stands
    public boolean isCurrent() {
        return structureRevision == rigHandle.getStructureRevision();
    }

    public Matrix4[] getSkinningMatrices() {
        return skinningMatrices;
    }

    public Matrix4 getWorldMatrix(int boneIndex) {
        return worldMatrices[boneIndex];
    }
}
