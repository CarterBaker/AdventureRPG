package editor.bootstrap.charactereditorpipeline.util;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import editor.runtime.EditorSetting;

public enum CharacterEditorRegion {

    /*
     * What part of a character a modelling tab works on. Whole is every worn
     * part and bone, the Character Mesh Editor's; Hair is the worn hair alone,
     * its card roots held to the scalp so every stroke combs, puffs, clumps
     * and smooths from the root out, and the spring chains its locks hang on,
     * every bone named with the hair prefix and the head they hang from;
     * Hands and Feet are the body where it weighs on the hands or feet and
     * every bone below them, the fingers and toes. A region names the worn
     * part it edits (none keeps the part chosen), the bones it holds with
     * every bone below them, the bone it is framed about, whether it takes
     * the rig, and the prefix naming its own bones, which new bones there
     * take.
     */

    WHOLE(null, EditorSetting.CHARACTER_REGION_NO_BONES, null, false, true, EditorSetting.CHARACTER_REGION_NO_PREFIX),
    HAIR(FeatureSlot.HAIR, EditorSetting.CHARACTER_REGION_NO_BONES, EditorSetting.CHARACTER_REGION_HAIR_FRAME,
            true, true, EditorSetting.CHARACTER_HAIR_LOCK_PREFIX),
    HANDS(null, EditorSetting.CHARACTER_REGION_HAND_BONES, EditorSetting.CHARACTER_REGION_HAND_FRAME, false, true,
            EditorSetting.CHARACTER_REGION_NO_PREFIX),
    FEET(null, EditorSetting.CHARACTER_REGION_FOOT_BONES, EditorSetting.CHARACTER_REGION_FOOT_FRAME, false, true,
            EditorSetting.CHARACTER_REGION_NO_PREFIX);

    // Values
    public static final CharacterEditorRegion[] VALUES = values();

    // Internal
    private final FeatureSlot featureSlot;
    private final String[] rootBones;
    private final String frameBone;
    private final boolean rootPinned;
    private final boolean rigged;
    private final String bonePrefix;

    // Constructor \\

    CharacterEditorRegion(
            FeatureSlot featureSlot,
            String[] rootBones,
            String frameBone,
            boolean rootPinned,
            boolean rigged,
            String bonePrefix) {

        this.featureSlot = featureSlot;
        this.rootBones = rootBones;
        this.frameBone = frameBone;
        this.rootPinned = rootPinned;
        this.rigged = rigged;
        this.bonePrefix = bonePrefix;
    }

    // Accessible \\

    public boolean isWhole() {
        return this == WHOLE;
    }

    // Whether the region is a worn feature rather than the body
    public boolean isFeature() {
        return featureSlot != null;
    }

    public FeatureSlot getFeatureSlot() {
        return featureSlot;
    }

    public boolean isBound() {
        return rootBones.length > 0;
    }

    public String[] getRootBones() {
        return rootBones;
    }

    public String getFrameBone() {
        return frameBone;
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
        return hasBonePrefix() && (boneName.startsWith(bonePrefix) || boneName.equals(frameBone));
    }
}
