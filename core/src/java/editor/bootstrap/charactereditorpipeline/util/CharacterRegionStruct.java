package editor.bootstrap.charactereditorpipeline.util;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class CharacterRegionStruct extends StructPackage {

    /*
     * What part of a character a Character Editor layer works on, each a
     * group of layers in the Layers tab nested under the region that holds
     * it. The regions are the rig's own: the whole character, the Body, then
     * one for every part the rig names, the way a rig groups its bones into
     * collections, and the worn hair. A part's region is the body where it
     * weighs on the part's bones and every bone below them, framed on them
     * and sided when every one has a twin across the body; it may hold a worn
     * part beside the body (eyes in their sockets, teeth in the mouth), show
     * others with it (hair on a head) and take the face's controls, as the
     * rig says. Hair is the worn hair alone, its card roots held to the scalp
     * so every stroke combs from the root out, with the spring chains its
     * locks hang on, every bone named with the hair prefix and the head they
     * hang from. A region is listed under the part whose bones its own hang
     * below, the hair under the part that shows it. A region names its key,
     * stable across runs, its label, the region it is listed under, the worn
     * part it holds, the bones it holds on the body, whether its roots are
     * pinned and it is sided, whether it takes the face, the prefix naming
     * its own bones, which new bones there take, and the worn parts it shows
     * beside its own.
     */

    // Whole — the whole character, the first region of every rig
    public static final CharacterRegionStruct WHOLE = new CharacterRegionStruct(
            EngineSetting.RIG_PART_RESERVED_WHOLE,
            EditorSetting.CHARACTER_REGION_LABEL_WHOLE,
            0,
            null,
            new int[0],
            false,
            false,
            false,
            EditorSetting.CHARACTER_REGION_NO_PREFIX,
            new FeatureSlot[0]);

    // Identity
    private final String key;
    private final String label;
    private final int index;

    // Layout
    private CharacterRegionStruct parent;

    // Holds
    private final FeatureSlot featureSlot;
    private final int[] partBones;
    private final boolean rootPinned;
    private final boolean sided;
    private final boolean facial;
    private final String bonePrefix;
    private final FeatureSlot[] shownFeatures;

    // Constructor \\

    public CharacterRegionStruct(
            String key,
            String label,
            int index,
            FeatureSlot featureSlot,
            int[] partBones,
            boolean rootPinned,
            boolean sided,
            boolean facial,
            String bonePrefix,
            FeatureSlot[] shownFeatures) {

        // Identity
        this.key = key;
        this.label = label;
        this.index = index;

        // Holds
        this.featureSlot = featureSlot;
        this.partBones = partBones;
        this.rootPinned = rootPinned;
        this.sided = sided;
        this.facial = facial;
        this.bonePrefix = bonePrefix;
        this.shownFeatures = shownFeatures;
    }

    // Layout \\

    public void setParent(CharacterRegionStruct parent) {
        this.parent = parent;
    }

    // Accessible \\

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    // The region's place in its rig's list, the whole character first
    public int getIndex() {
        return index;
    }

    // The region this one is listed under in the Layers tab, null for the whole character
    public CharacterRegionStruct getParent() {
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
        return partBones.length > 0;
    }

    // The bones of the rig's part the region holds, with every bone below them; none for the hair and the whole
    public int[] getPartBones() {
        return partBones;
    }

    public boolean isRootPinned() {
        return rootPinned;
    }

    // Whether every bone of the region's part has a twin across the body, so it is shown and framed one side at a time
    public boolean isSided() {
        return sided;
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
