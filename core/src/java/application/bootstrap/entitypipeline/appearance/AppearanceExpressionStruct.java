package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import engine.root.StructPackage;

public class AppearanceExpressionStruct extends StructPackage {

    /*
     * One expression a race's face plays, by name: the clip that keys its
     * face bones and the weight curves of its morphs, and per body the morph
     * each curve weighs, resolved by name when the race is built, since
     * every body numbers its morphs its own way.
     */

    // Identity
    private final String expressionName;

    // Clip
    private final AnimationClipHandle clip;

    // Morphs — per body, the morph each of the clip's curves weighs
    private final int[][] trackMorphs;

    // Constructor \\

    public AppearanceExpressionStruct(String expressionName, AnimationClipHandle clip, int[][] trackMorphs) {

        // Identity
        this.expressionName = expressionName;

        // Clip
        this.clip = clip;

        // Morphs
        this.trackMorphs = trackMorphs;
    }

    // Accessible \\

    public String getExpressionName() {
        return expressionName;
    }

    public AnimationClipHandle getClip() {
        return clip;
    }

    public int getTrackCount() {
        return clip.getMorphTrackCount();
    }

    public int getTrackMorph(int bodyIndex, int trackIndex) {
        return trackMorphs[bodyIndex][trackIndex];
    }

    // Whether any of the clip's curves weighs a morph by its name
    public boolean playsMorph(String morphName) {

        for (int t = 0; t < clip.getMorphTrackCount(); t++)
            if (clip.getMorphTrack(t).getMorphName().equals(morphName))
                return true;

        return false;
    }
}
