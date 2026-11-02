package application.bootstrap.entitypipeline.feature;

public enum FeatureSlot {

    /*
     * Swappable modelled parts worn over a character's body mesh, each its own
     * rigged mesh on the character's rig. The face is not a slot: it is
     * modelled into the body and shaped by sliders. Every slot may be left
     * empty.
     */

    HAIR;

    // Values
    public static final FeatureSlot[] VALUES = values();
}
