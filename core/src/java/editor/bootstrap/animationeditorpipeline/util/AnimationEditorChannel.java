package editor.bootstrap.animationeditorpipeline.util;

import application.bootstrap.animationpipeline.animation.AnimationChannel;
import editor.runtime.EditorSetting;

public enum AnimationEditorChannel {

    /*
     * The three channels a clip keys for a bone (AnimationChannel), as the
     * animation editor shows them: each with the words its rows show and the
     * step they take. A bone's pose is flat, every channel's x, y and z, in
     * the slots of the clip's own channels.
     */

    ROTATION(AnimationChannel.ROTATION, EditorSetting.ANIMATION_EDITOR_LABEL_ROTATION,
            EditorSetting.ANIMATION_EDITOR_ROTATION_STEP),
    POSITION(AnimationChannel.POSITION, EditorSetting.ANIMATION_EDITOR_LABEL_POSITION,
            EditorSetting.ANIMATION_EDITOR_POSITION_STEP),
    SCALE(AnimationChannel.SCALE, EditorSetting.ANIMATION_EDITOR_LABEL_SCALE,
            EditorSetting.ANIMATION_EDITOR_SCALE_STEP);

    // Values
    public static final AnimationEditorChannel[] VALUES = values();

    // Internal
    private final AnimationChannel channel;
    private final String label;
    private final float step;

    // Constructor \

    AnimationEditorChannel(AnimationChannel channel, String label, float step) {
        this.channel = channel;
        this.label = label;
        this.step = step;
    }

    // Pose \

    // Where one axis of this channel sits in a bone's flat pose of every channel's x, y and z
    public int toPoseIndex(int axis) {
        return channel.toSlot(axis);
    }

    public static int getPoseLength() {
        return AnimationChannel.SLOT_COUNT;
    }

    // Accessible \

    public AnimationChannel getChannel() {
        return channel;
    }

    public String getField() {
        return channel.getField();
    }

    public String getLabel() {
        return label;
    }

    public float getDefaultValue() {
        return channel.getDefaultValue();
    }

    public float getStep() {
        return step;
    }

    public static AnimationEditorChannel fromField(String field) {

        for (AnimationEditorChannel channel : VALUES)
            if (channel.getField().equals(field))
                return channel;

        return null;
    }
}
