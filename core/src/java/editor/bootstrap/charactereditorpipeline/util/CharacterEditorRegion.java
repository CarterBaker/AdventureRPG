package editor.bootstrap.charactereditorpipeline.util;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.rig.RigPart;
import editor.runtime.EditorSetting;

public enum CharacterEditorRegion {

    /*
     * What part of a character a Character Editor layer works on, each a
     * group of layers in the Layers tab nested under the region that holds
     * it. Whole is every worn part and bone, the Body; Head is the body where
     * it weighs on the bones the rig names as its head and every bone below
     * them, shown with the hair, eyes and teeth worn on it, and holds the
     * face's controls; Hair is the worn hair alone, its card roots held to
     * the scalp so every stroke combs, puffs, clumps and smooths from the
     * root out, and the spring chains its locks hang on, every bone named
     * with the hair prefix and the head they hang from; Ears, Hands and Feet
     * are the body where it weighs on the bones the rig names for them and
     * every bone below, the fingers and toes; Eyes are the socket and lids on
     * the body where they weigh on the rig's eye bones together with the eyes
     * worn in them, and Mouth the lips, the jaw and the mouth's inside on the
     * body with the teeth and tongue worn there. A region names its label,
     * the region it is listed under, the worn part it holds, the RigPart
     * whose bones it holds on the body with every bone below them, whether
     * its roots are pinned, whether it takes the rig and the face, the prefix
     * naming its own bones, which new bones there take, and the worn parts
     * it shows beside its own. Every creature's part layers work the same
     * way, through the bones its own rig names for each part.
     */

    WHOLE(
            EditorSetting.CHARACTER_REGION_LABEL_WHOLE,
            null,
            null,
            null,
            false,
            true,
            false,
            EditorSetting.CHARACTER_REGION_NO_PREFIX),
    HEAD(
            EditorSetting.CHARACTER_REGION_LABEL_HEAD,
            WHOLE,
            null,
            RigPart.HEAD,
            false,
            true,
            true,
            EditorSetting.CHARACTER_REGION_NO_PREFIX,
            FeatureSlot.HAIR,
            FeatureSlot.EYES,
            FeatureSlot.TEETH),
    HAIR(
            EditorSetting.CHARACTER_REGION_LABEL_HAIR,
            HEAD,
            FeatureSlot.HAIR,
            null,
            true,
            true,
            false,
            EditorSetting.CHARACTER_HAIR_LOCK_PREFIX),
    EARS(
            EditorSetting.CHARACTER_REGION_LABEL_EARS,
            HEAD,
            null,
            RigPart.EARS,
            false,
            true,
            false,
            EditorSetting.CHARACTER_REGION_NO_PREFIX),
    EYES(
            EditorSetting.CHARACTER_REGION_LABEL_EYES,
            HEAD,
            FeatureSlot.EYES,
            RigPart.EYES,
            false,
            true,
            false,
            EditorSetting.CHARACTER_REGION_NO_PREFIX),
    MOUTH(
            EditorSetting.CHARACTER_REGION_LABEL_MOUTH,
            HEAD,
            FeatureSlot.TEETH,
            RigPart.MOUTH,
            false,
            true,
            false,
            EditorSetting.CHARACTER_REGION_NO_PREFIX),
    HANDS(
            EditorSetting.CHARACTER_REGION_LABEL_HANDS,
            WHOLE,
            null,
            RigPart.HANDS,
            false,
            true,
            false,
            EditorSetting.CHARACTER_REGION_NO_PREFIX),
    FEET(
            EditorSetting.CHARACTER_REGION_LABEL_FEET,
            WHOLE,
            null,
            RigPart.FEET,
            false,
            true,
            false,
            EditorSetting.CHARACTER_REGION_NO_PREFIX);

    // Values
    public static final CharacterEditorRegion[] VALUES = values();

    // Internal
    private final String label;
    private final CharacterEditorRegion parent;
    private final FeatureSlot featureSlot;
    private final RigPart rigPart;
    private final boolean rootPinned;
    private final boolean rigged;
    private final boolean facial;
    private final String bonePrefix;
    private final FeatureSlot[] shownFeatures;

    // Constructor \\

    CharacterEditorRegion(
            String label,
            CharacterEditorRegion parent,
            FeatureSlot featureSlot,
            RigPart rigPart,
            boolean rootPinned,
            boolean rigged,
            boolean facial,
            String bonePrefix,
            FeatureSlot... shownFeatures) {

        this.label = label;
        this.parent = parent;
        this.featureSlot = featureSlot;
        this.rigPart = rigPart;
        this.rootPinned = rootPinned;
        this.rigged = rigged;
        this.facial = facial;
        this.bonePrefix = bonePrefix;
        this.shownFeatures = shownFeatures;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // The region this one is listed under in the Layers tab, null for the whole character
    public CharacterEditorRegion getParent() {
        return parent;
    }

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

    // Whether the face's controls are shaped in the region
    public boolean takesFace() {
        return facial;
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

    // Whether a worn part is shown while the region stands alone: its own, or one it shows beside it
    public boolean showsFeature(FeatureSlot slot) {

        if (slot == featureSlot)
            return true;

        for (int i = 0; i < shownFeatures.length; i++)
            if (shownFeatures[i] == slot)
                return true;

        return false;
    }
}
