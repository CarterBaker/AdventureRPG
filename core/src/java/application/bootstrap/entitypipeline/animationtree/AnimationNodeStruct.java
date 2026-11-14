package application.bootstrap.entitypipeline.animationtree;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import engine.root.StructPackage;

public class AnimationNodeStruct extends StructPackage {

    /*
     * Immutable node of an animation layer — one clip, or a blend space of
     * several placed along a single AnimationParameter at strictly
     * increasing values. Every clip in a node shares one normalized phase,
     * so a walk blending into a run keeps its footfalls, and all of them
     * either loop or play once. rate multiplies playback; rateScale lets a
     * parameter past either end of the blend space speed or slow playback
     * in proportion. blendDuration is the default cross-fade into this node
     * and delay how long its state must hold before the layer enters it.
     */

    // Identity
    private final String nodeName;

    // Blend Space
    private final AnimationParameter parameter;
    private final AnimationClipHandle[] clips;
    private final float[] values;

    // Playback
    private final boolean looping;
    private final float rate;
    private final boolean rateScale;

    // Transition
    private final float blendDuration;
    private final float delay;

    // Constructor \\

    public AnimationNodeStruct(
            String nodeName,
            AnimationParameter parameter,
            AnimationClipHandle[] clips,
            float[] values,
            boolean looping,
            float rate,
            boolean rateScale,
            float blendDuration,
            float delay) {

        // Identity
        this.nodeName = nodeName;

        // Blend Space
        this.parameter = parameter;
        this.clips = clips;
        this.values = values;

        // Playback
        this.looping = looping;
        this.rate = rate;
        this.rateScale = rateScale;

        // Transition
        this.blendDuration = blendDuration;
        this.delay = delay;
    }

    // Accessible \\

    public String getNodeName() {
        return nodeName;
    }

    public boolean hasParameter() {
        return parameter != null;
    }

    public AnimationParameter getParameter() {
        return parameter;
    }

    public int getClipCount() {
        return clips.length;
    }

    public AnimationClipHandle getClip(int index) {
        return clips[index];
    }

    public float getValue(int index) {
        return values[index];
    }

    public boolean isLooping() {
        return looping;
    }

    public float getRate() {
        return rate;
    }

    public boolean isRateScaled() {
        return rateScale;
    }

    public float getBlendDuration() {
        return blendDuration;
    }

    public float getDelay() {
        return delay;
    }
}
