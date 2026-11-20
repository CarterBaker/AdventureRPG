package editor.bootstrap.charactereditorpipeline.util;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;

public enum CharacterBoneDisplay {

    /*
     * How the character viewports draw a rig's bones, as Blender's armature
     * display offers them: Octahedral, the tapered solid widest near its
     * joint; Stick, a thin bar of fixed width; B-Bone, a box as wide as the
     * bone's own size; Envelope, a bar between spheres at each joint as wide
     * as the bone's size reaches; Wire, the thinnest line; and Hidden, none.
     * A bone runs from its joint to each child's, and a bone with no child,
     * like the root, is a sphere at its joint. Its width is one formula for
     * every style: a fixed width, a share of its length and a share of its
     * size; the marker shape is the viewport's (CHARACTER_VIEW_MARKER_*).
     */

    OCTAHEDRAL(
            EditorSetting.BONE_DISPLAY_LABEL_OCTAHEDRAL,
            EditorSetting.CHARACTER_VIEW_MARKER_OCTAHEDRAL,
            0f,
            EditorSetting.BONE_DISPLAY_OCTAHEDRAL_LENGTH_SHARE,
            0f,
            false),
    STICK(
            EditorSetting.BONE_DISPLAY_LABEL_STICK,
            EditorSetting.CHARACTER_VIEW_MARKER_CUBE,
            EditorSetting.BONE_DISPLAY_STICK_WIDTH,
            0f,
            0f,
            false),
    BBONE(
            EditorSetting.BONE_DISPLAY_LABEL_BBONE,
            EditorSetting.CHARACTER_VIEW_MARKER_CUBE,
            0f,
            0f,
            EditorSetting.BONE_DISPLAY_BBONE_SIZE_SHARE,
            false),
    ENVELOPE(
            EditorSetting.BONE_DISPLAY_LABEL_ENVELOPE,
            EditorSetting.CHARACTER_VIEW_MARKER_CUBE,
            EditorSetting.BONE_DISPLAY_WIRE_WIDTH,
            0f,
            0f,
            true),
    WIRE(
            EditorSetting.BONE_DISPLAY_LABEL_WIRE,
            EditorSetting.CHARACTER_VIEW_MARKER_CUBE,
            EditorSetting.BONE_DISPLAY_WIRE_WIDTH,
            0f,
            0f,
            false),
    HIDDEN(
            EditorSetting.BONE_DISPLAY_LABEL_HIDDEN,
            EngineSetting.INDEX_NOT_FOUND,
            0f,
            0f,
            0f,
            false);

    // Values
    public static final CharacterBoneDisplay[] VALUES = values();

    // Internal
    private final String label;
    private final int segmentShape;
    private final float fixedWidth;
    private final float lengthShare;
    private final float sizeShare;
    private final boolean envelopes;

    // Constructor \\

    CharacterBoneDisplay(
            String label,
            int segmentShape,
            float fixedWidth,
            float lengthShare,
            float sizeShare,
            boolean envelopes) {
        this.label = label;
        this.segmentShape = segmentShape;
        this.fixedWidth = fixedWidth;
        this.lengthShare = lengthShare;
        this.sizeShare = sizeShare;
        this.envelopes = envelopes;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // The marker shape a bone's segment is drawn with; none while the bones are hidden
    public int getSegmentShape() {
        return segmentShape;
    }

    public boolean drawsBones() {
        return segmentShape != EngineSetting.INDEX_NOT_FOUND;
    }

    // Whether every joint is a sphere as wide as its bone's size reaches, as an envelope is
    public boolean drawsEnvelopes() {
        return envelopes;
    }

    // How wide a bone of a length and a size is drawn
    public float resolveWidth(float length, float size) {
        return fixedWidth + lengthShare * length + sizeShare * size;
    }

    // The style after or before this one, round the list
    public CharacterBoneDisplay step(int direction) {
        return VALUES[Math.floorMod(ordinal() + direction, VALUES.length)];
    }
}
