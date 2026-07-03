package application.bootstrap.animationpipeline.animation;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.HandlePackage;

public class AnimationClipHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded animation clip. Registered and owned
     * by AnimationManager for the engine lifetime. Delegates all track
     * lookups through AnimationClipData — the single source every
     * AnimationStateHandle samples from during pose evaluation.
     */

    // Internal
    private AnimationClipData clipData;

    // Constructor \\

    public void constructor(AnimationClipData clipData) {

        // Internal
        this.clipData = clipData;
    }

    // Accessible \\

    public AnimationClipData getClipData() {
        return clipData;
    }

    public String getClipName() {
        return clipData.getClipName();
    }

    public RigHandle getRigHandle() {
        return clipData.getRigHandle();
    }

    public float getDuration() {
        return clipData.getDuration();
    }

    public boolean isLooping() {
        return clipData.isLooping();
    }

    public BoneTrackStruct getBoneTrack(int boneIndex) {
        return clipData.getBoneTrack(boneIndex);
    }

    public boolean hasBoneTrack(int boneIndex) {
        return clipData.hasBoneTrack(boneIndex);
    }
}