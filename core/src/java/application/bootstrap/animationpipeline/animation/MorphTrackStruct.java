package application.bootstrap.animationpipeline.animation;

import engine.root.StructPackage;
import engine.util.mathematics.curves.CurveStruct;

public class MorphTrackStruct extends StructPackage {

    /*
     * Immutable weight curve of one morph target within a clip, the
     * blendshape curve a facial clip keys: the morph is named, not indexed,
     * since every mesh numbers its morphs its own way, and whoever plays the
     * clip resolves the name against the mesh it weighs. Its keys are a curve
     * like any bone axis's (keys in strictly increasing time, each with its
     * mode and handles, enforced by AnimationBuilder), holding its first and
     * last value past either end.
     */

    // Identity
    private final String morphName;

    // Curve
    private final CurveStruct curve;

    // Constructor \\

    public MorphTrackStruct(String morphName, CurveStruct curve) {

        // Identity
        this.morphName = morphName;

        // Curve
        this.curve = curve;
    }

    // Accessible \\

    public String getMorphName() {
        return morphName;
    }

    public CurveStruct getCurve() {
        return curve;
    }

    public float getEndTime() {
        return curve.getEndTime();
    }
}
