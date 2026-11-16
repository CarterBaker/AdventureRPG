package application.bootstrap.animationpipeline.animation;

import engine.root.StructPackage;

public class MorphTrackStruct extends StructPackage {

    /*
     * Immutable weight curve of one morph target within a clip, the
     * blendshape curve a facial clip keys: the morph is named, not indexed,
     * since every mesh numbers its morphs its own way, and whoever plays the
     * clip resolves the name against the mesh it weighs. Times are strictly
     * increasing, enforced by AnimationBuilder; the weight is linear between
     * keys and holds past either end.
     */

    // Identity
    private final String morphName;

    // Keys
    private final float[] times;
    private final float[] values;

    // Constructor \\

    public MorphTrackStruct(String morphName, float[] times, float[] values) {

        // Identity
        this.morphName = morphName;

        // Keys
        this.times = times;
        this.values = values;
    }

    // Accessible \\

    public String getMorphName() {
        return morphName;
    }

    public int getKeyCount() {
        return times.length;
    }

    public float getTime(int key) {
        return times[key];
    }

    public float getValue(int key) {
        return values[key];
    }

    public float getEndTime() {
        return times[times.length - 1];
    }
}
