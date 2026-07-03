package application.bootstrap.animationpipeline.animation;

import engine.root.StructPackage;

public class BoneTrackStruct extends StructPackage {

    /*
     * Immutable ordered keyframe array for a single bone within one clip.
     * Bones with no authored motion in a clip simply have no entry in
     * AnimationClipData.boneTracks — there is no BoneTrackStruct for them,
     * and pose evaluation falls back to the rig's bind pose for that bone.
     */

    // Keyframes
    private final AnimationKeyframeStruct[] keyframes;

    // Constructor \\

    public BoneTrackStruct(AnimationKeyframeStruct[] keyframes) {

        // Keyframes
        this.keyframes = keyframes;
    }

    // Accessible \\

    public AnimationKeyframeStruct[] getKeyframes() {
        return keyframes;
    }

    public int getKeyframeCount() {
        return keyframes.length;
    }

    public AnimationKeyframeStruct getKeyframe(int index) {
        return keyframes[index];
    }
}