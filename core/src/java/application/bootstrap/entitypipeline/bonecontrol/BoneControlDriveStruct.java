package application.bootstrap.entitypipeline.bonecontrol;

import engine.root.StructPackage;

public class BoneControlDriveStruct extends StructPackage {

    /*
     * One bone slider a control drives: the slider by name, whether the
     * control pushes it up its range (increase) or down it as the control
     * rises, and the keys of the curve it follows, each a share along the
     * control's range and the share of the slider's range reached there,
     * at most two, in ascending order of where they stand. With no keys the
     * slider follows the control straight across its range; a key at either
     * end takes that end's place.
     */

    // Identity
    private final String sliderName;

    // Curve
    private final boolean increase;
    private final float[] keys;

    // Constructor \\

    public BoneControlDriveStruct(String sliderName, boolean increase, float[] keys) {

        // Identity
        this.sliderName = sliderName;

        // Curve
        this.increase = increase;
        this.keys = keys;
    }

    // Accessible \\

    public String getSliderName() {
        return sliderName;
    }

    public boolean isIncrease() {
        return increase;
    }

    // Each key's place along the control and the share reached there, one pair after another
    public float[] getKeys() {
        return keys;
    }
}
