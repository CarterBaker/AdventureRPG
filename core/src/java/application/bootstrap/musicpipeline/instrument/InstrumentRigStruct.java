package application.bootstrap.musicpipeline.instrument;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.StructPackage;

public class InstrumentRigStruct extends StructPackage {

    /*
     * Immutable animation of an instrument for one rig: the clip a body holds
     * it with at rest and the clip it plays it with. Both loop and both are
     * made for the rig.
     */

    // Rig
    private final RigHandle rigHandle;

    // Clips
    private final AnimationClipHandle holdClip;
    private final AnimationClipHandle playClip;

    // Constructor \\

    public InstrumentRigStruct(RigHandle rigHandle, AnimationClipHandle holdClip, AnimationClipHandle playClip) {

        // Rig
        this.rigHandle = rigHandle;

        // Clips
        this.holdClip = holdClip;
        this.playClip = playClip;
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public AnimationClipHandle getHoldClip() {
        return holdClip;
    }

    public AnimationClipHandle getPlayClip() {
        return playClip;
    }
}
