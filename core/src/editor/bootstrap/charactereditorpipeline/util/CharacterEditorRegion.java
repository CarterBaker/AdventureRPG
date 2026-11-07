package editor.bootstrap.charactereditorpipeline.util;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import editor.runtime.EditorSetting;

public enum CharacterEditorRegion {

    /*
     * What part of a character a modelling tab works on. Whole is every worn
     * part and bone, the Character Mesh Editor's; Hair is the worn hair alone,
     * its card roots held to the scalp so every stroke combs, puffs, clumps
     * and smooths from the root out; Hands and Feet are the body where it
     * weighs on the hands or feet and every bone below them, the fingers and
     * toes. A region names the worn part it edits (none keeps the part
     * chosen), the bones it holds, the bone it is framed about, and whether
     * it takes the rig.
     */

    WHOLE(null, EditorSetting.CHARACTER_REGION_NO_BONES, null, false, true),
    HAIR(FeatureSlot.HAIR, EditorSetting.CHARACTER_REGION_NO_BONES, EditorSetting.CHARACTER_REGION_HAIR_FRAME,
            true, false),
    HANDS(null, EditorSetting.CHARACTER_REGION_HAND_BONES, EditorSetting.CHARACTER_REGION_HAND_FRAME, false, true),
    FEET(null, EditorSetting.CHARACTER_REGION_FOOT_BONES, EditorSetting.CHARACTER_REGION_FOOT_FRAME, false, true);

    // Values
    public static final CharacterEditorRegion[] VALUES = values();

    // Internal
    private final FeatureSlot featureSlot;
    private final String[] rootBones;
    private final String frameBone;
    private final boolean rootPinned;
    private final boolean rigged;

    // Constructor \\

    CharacterEditorRegion(
            FeatureSlot featureSlot,
            String[] rootBones,
            String frameBone,
            boolean rootPinned,
            boolean rigged) {

        this.featureSlot = featureSlot;
        this.rootBones = rootBones;
        this.frameBone = frameBone;
        this.rootPinned = rootPinned;
        this.rigged = rigged;
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
}
