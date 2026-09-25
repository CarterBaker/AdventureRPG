package application.bootstrap.animationpipeline.animation;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.DataPackage;

public class AnimationClipData extends DataPackage {

    /*
     * Immutable clip definition. boneTracks is indexed identically to the
     * rig it was built against — boneTracks[boneIndex] is null for any bone
     * with no authored motion in this clip. duration is derived from the
     * latest keyframe time across every track at build time, never authored
     * directly, so it can never drift out of sync with the data it describes.
     * blendDuration is how long an entity cross-fades into this clip from
     * whatever pose it held when the clip was chosen.
     */

    // Identity
    private final String clipName;
    private final RigHandle rigHandle;

    // Timing
    private final float duration;
    private final boolean looping;
    private final float blendDuration;

    // Tracks — indexed by bone index against rigHandle
    private final BoneTrackStruct[] boneTracks;

    // Constructor \\

    public AnimationClipData(
            String clipName,
            RigHandle rigHandle,
            float duration,
            boolean looping,
            float blendDuration,
            BoneTrackStruct[] boneTracks) {

        // Identity
        this.clipName = clipName;
        this.rigHandle = rigHandle;

        // Timing
        this.duration = duration;
        this.looping = looping;
        this.blendDuration = blendDuration;

        // Tracks
        this.boneTracks = boneTracks;
    }

    // Accessible \\

    public String getClipName() {
        return clipName;
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public float getDuration() {
        return duration;
    }

    public boolean isLooping() {
        return looping;
    }

    public float getBlendDuration() {
        return blendDuration;
    }

    public BoneTrackStruct getBoneTrack(int boneIndex) {
        return boneTracks[boneIndex];
    }

    public boolean hasBoneTrack(int boneIndex) {
        return boneTracks[boneIndex] != null;
    }

    public int getBoneTrackCount() {
        return boneTracks.length;
    }
}