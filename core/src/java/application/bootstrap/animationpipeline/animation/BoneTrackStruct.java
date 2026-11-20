package application.bootstrap.animationpipeline.animation;

import engine.root.StructPackage;
import engine.util.mathematics.curves.CurveStruct;

public class BoneTrackStruct extends StructPackage {

    /*
     * Immutable curves of a single bone within one clip, one slot for each
     * axis of each channel (AnimationChannel.toSlot); a slot with no curve
     * holds its channel's default. Bones with no authored motion in a clip
     * simply have no entry in AnimationClipData.boneTracks, and pose
     * evaluation falls back to the rig's bind pose for that bone.
     */

    // Curves
    private final CurveStruct[] curves;
    private final float endTime;

    // Constructor \\

    public BoneTrackStruct(CurveStruct[] curves, float endTime) {

        // Curves
        this.curves = curves;
        this.endTime = endTime;
    }

    // Accessible \\

    public CurveStruct getCurve(int slot) {
        return curves[slot];
    }

    public boolean hasCurve(int slot) {
        return curves[slot] != null;
    }

    // The latest key across every curve
    public float getEndTime() {
        return endTime;
    }

    // The slot's value at a moment of the clip, its channel's default where it has no curve
    public float sample(int slot, float time) {
        return curves[slot] != null
                ? (float) curves[slot].evaluate(time)
                : AnimationChannel.fromSlot(slot).getDefaultValue();
    }
}
