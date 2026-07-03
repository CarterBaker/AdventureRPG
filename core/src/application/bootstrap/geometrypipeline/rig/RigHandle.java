package application.bootstrap.geometrypipeline.rig;

import engine.root.HandlePackage;

public class RigHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded rig template. Registered and owned
     * by RigManager for the engine lifetime. Delegates all bone lookups
     * through RigData — used as the shared bind-pose source every
     * AnimationStateHandle evaluates its runtime pose against.
     */

    // Internal
    private RigData rigData;

    // Constructor \\

    public void constructor(RigData rigData) {

        // Internal
        this.rigData = rigData;
    }

    // Accessible \\

    public RigData getRigData() {
        return rigData;
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
}