package editor.bootstrap.animationeditorpipeline.util;

import editor.runtime.EditorSetting;

public enum AnimationEditorChannel {

    /*
     * The three channels a keyframe carries for its bone, each an x, y and z
     * offset from the bind pose: rotation in degrees, position in blocks,
     * and scale as a multiplier. Each knows its field in a clip's ARPG, the
     * value it holds when not written, and the step its panel rows take.
     */

    ROTATION(
            EditorSetting.ANIMATION_EDITOR_FIELD_ROTATION,
            EditorSetting.ANIMATION_EDITOR_LABEL_ROTATION,
            0f,
            EditorSetting.ANIMATION_EDITOR_ROTATION_STEP),
    POSITION(
            EditorSetting.ANIMATION_EDITOR_FIELD_POSITION,
            EditorSetting.ANIMATION_EDITOR_LABEL_POSITION,
            0f,
            EditorSetting.ANIMATION_EDITOR_POSITION_STEP),
    SCALE(
            EditorSetting.ANIMATION_EDITOR_FIELD_SCALE,
            EditorSetting.ANIMATION_EDITOR_LABEL_SCALE,
            1f,
            EditorSetting.ANIMATION_EDITOR_SCALE_STEP);

    // Values
    public static final AnimationEditorChannel[] VALUES = values();

    // Internal
    private final String field;
    private final String label;
    private final float defaultValue;
    private final float step;

    // Constructor \\

    AnimationEditorChannel(String field, String label, float defaultValue, float step) {
        this.field = field;
        this.label = label;
        this.defaultValue = defaultValue;
        this.step = step;
    }

    // Pose \\

    // Where one axis of this channel sits in a bone's flat pose of every channel's x, y and z
    public int toPoseIndex(int axis) {
        return ordinal() * EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length + axis;
    }

    public static int getPoseLength() {
        return VALUES.length * EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length;
    }

    // Accessible \\

    public String getField() {
        return field;
    }

    public String getLabel() {
        return label;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public float getStep() {
        return step;
    }

    public static AnimationEditorChannel fromField(String field) {

        for (AnimationEditorChannel channel : VALUES)
            if (channel.field.equals(field))
                return channel;

        return null;
    }
}
