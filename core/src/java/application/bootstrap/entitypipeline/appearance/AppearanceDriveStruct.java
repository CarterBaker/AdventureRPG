package application.bootstrap.entitypipeline.appearance;

import engine.root.StructPackage;

public class AppearanceDriveStruct extends StructPackage {

    /*
     * One bone slider a control slider drives, resolved against the race's
     * sliders: the slider's index, whether it rises with the control, and
     * the keys of the curve it follows (BoneControlCurveUtility).
     */

    // Target
    private final int sliderIndex;

    // Curve
    private final boolean increase;
    private final float[] keys;

    // Constructor \\

    public AppearanceDriveStruct(int sliderIndex, boolean increase, float[] keys) {

        // Target
        this.sliderIndex = sliderIndex;

        // Curve
        this.increase = increase;
        this.keys = keys;
    }

    // Accessible \\

    public int getSliderIndex() {
        return sliderIndex;
    }

    public boolean isIncrease() {
        return increase;
    }

    public float[] getKeys() {
        return keys;
    }
}
