package application.bootstrap.entitypipeline.feature;

public enum FeatureSlot {

    /*
     * Swappable modelled parts worn over a character's body mesh, each its own
     * rigged mesh on the character's rig and drawn with its own material when
     * it names one: the hair, the eyes and the teeth with the tongue. The
     * face around them is modelled into the body and shaped by sliders.
     * Every slot may be left empty, for a creature with no eyes or teeth; a
     * removable slot is one the game's creator also offers the player bare,
     * as hair may be shaved away and eyes may not.
     */

    HAIR(true),
    EYES(false),
    TEETH(false);

    // Values
    public static final FeatureSlot[] VALUES = values();

    // Internal
    private final boolean removable;

    // Constructor \\

    FeatureSlot(boolean removable) {
        this.removable = removable;
    }

    // Accessible \\

    public boolean isRemovable() {
        return removable;
    }
}
