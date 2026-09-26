package application.bootstrap.entitypipeline.feature;

public enum FeatureSlot {

    /*
     * Swappable parts of a character's appearance. Mesh slots (head, hair,
     * nose) are drawn as their own rigged mesh on the character's rig; texture
     * slots (eyes, brows, mouth) are overlays painted onto the head's face
     * tile in the character shader. Only the head also carries a face tile,
     * and only the head is required — every other slot may be left empty.
     */

    HEAD(true, true, true),
    HAIR(true, false, false),
    NOSE(true, false, false),
    EYES(false, false, false),
    BROWS(false, false, false),
    MOUTH(false, false, false);

    // Values
    public static final FeatureSlot[] VALUES = values();

    // Internal
    private final boolean meshSlot;
    private final boolean required;
    private final boolean faceSlot;

    // Constructor \\

    FeatureSlot(boolean meshSlot, boolean required, boolean faceSlot) {
        this.meshSlot = meshSlot;
        this.required = required;
        this.faceSlot = faceSlot;
    }

    // Accessible \\

    public boolean isMeshSlot() {
        return meshSlot;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isFaceSlot() {
        return faceSlot;
    }
}
