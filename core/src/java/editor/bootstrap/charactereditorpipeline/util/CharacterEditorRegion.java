package editor.bootstrap.charactereditorpipeline.util;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.rig.RigPart;
import editor.runtime.EditorSetting;

public enum CharacterEditorRegion {

    /*
     * What part of a character a modelling tab works on. Whole is every worn
     * part and bone, the Character Mesh Editor's; Hair is the worn hair alone,
     * its card roots held to the scalp so every stroke combs, puffs, clumps
     * and smooths from the root out, and the spring chains its locks hang on,
     * every bone named with the hair prefix and the head they hang from;
     * Hands and Feet are the body where it weighs on the bones the rig names
     * as its hands or feet and every bone below them, the fingers and toes;
     * Eyes are the socket and lids on the body where they weigh on the rig's
     * eye bones together with the eyes worn in them, and Mouth the lips, the
     * jaw and the mouth's inside on the body with the teeth and tongue worn
     * there. A region names the worn part it holds, the RigPart whose bones
     * it holds on the body with every bone below them, whether its roots are
     * pinned, whether it takes the rig, and the prefix naming its own bones,
     * which new bones there take. Every creature's part editors work the
     * same way, through the bones its own rig names for each part.
     */

    WHOLE(null, null, false, true, EditorSetting.CHARACTER_REGION_NO_PREFIX),
    HAIR(FeatureSlot.HAIR, null, true, true, EditorSetting.CHARACTER_HAIR_LOCK_PREFIX),
    HANDS(null, RigPart.HANDS, false, true, EditorSetting.CHARACTER_REGION_NO_PREFIX),
    FEET(null, RigPart.FEET, false, true, EditorSetting.CHARACTER_REGION_NO_PREFIX),
    EYES(FeatureSlot.EYES, RigPart.EYES, false, true, EditorSetting.CHARACTER_REGION_NO_PREFIX),
    MOUTH(FeatureSlot.TEETH, RigPart.MOUTH, false, true, EditorSetting.CHARACTER_REGION_NO_PREFIX);

    // Values
    public static final CharacterEditorRegion[] VALUES = values();

    // Internal
    private final FeatureSlot featureSlot;
    private final RigPart rigPart;
    private final boolean rootPinned;
    private final boolean rigged;
    private final String bonePrefix;

    // Constructor \\

    CharacterEditorRegion(
            FeatureSlot featureSlot,
            RigPart rigPart,
            boolean rootPinned,
            boolean rigged,
            String bonePrefix) {

        this.featureSlot = featureSlot;
        this.rigPart = rigPart;
        this.rootPinned = rootPinned;
        this.rigged = rigged;
        this.bonePrefix = bonePrefix;
    }

    // Accessible \\

    public boolean isWhole() {
        return this == WHOLE;
    }

    // Whether the region holds a worn feature
    public boolean holdsFeature() {
        return featureSlot != null;
    }

    public FeatureSlot getFeatureSlot() {
        return featureSlot;
    }

    // Whether the region holds the body where it weighs on the bones of a part of the rig
    public boolean holdsBody() {
        return rigPart != null;
    }

    public RigPart getRigPart() {
        return rigPart;
    }

    public boolean isRootPinned() {
        return rootPinned;
    }

    public boolean takesRig() {
        return rigged;
    }

    public boolean hasBonePrefix() {
        return !bonePrefix.isEmpty();
    }

    public String getBonePrefix() {
        return bonePrefix;
    }

    // Whether a bone is the region's own by name: named with its prefix, or the bone its prefixed bones hang from
    public boolean namesBone(String boneName) {
        return hasBonePrefix()
                && (boneName.startsWith(bonePrefix) || boneName.equals(EditorSetting.CHARACTER_REGION_HAIR_FRAME));
    }
}
