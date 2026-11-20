package application.bootstrap.animationpipeline.animation;

import engine.root.EngineSetting;

public enum AnimationChannel {

    /*
     * The three channels a clip keys for a bone, each an x, y and z offset
     * from the bind pose: rotation in Euler degrees (XYZ order), position in
     * blocks and scale as a multiplier. Each axis of each channel is a curve
     * of its own, held in one slot of a bone track: the channel's place times
     * three, plus the axis. Each knows its field in a clip's ARPG and the
     * value an axis holds when it has no curve.
     */

    ROTATION(EngineSetting.ANIMATION_FIELD_ROTATION, 0f),
    POSITION(EngineSetting.ANIMATION_FIELD_POSITION, 0f),
    SCALE(EngineSetting.ANIMATION_FIELD_SCALE, 1f);

    // Values
    public static final AnimationChannel[] VALUES = values();
    public static final int AXIS_COUNT = EngineSetting.ANIMATION_AXIS_NAMES.length;
    public static final int SLOT_COUNT = VALUES.length * AXIS_COUNT;

    // Internal
    private final String field;
    private final float defaultValue;

    // Constructor \\

    AnimationChannel(String field, float defaultValue) {
        this.field = field;
        this.defaultValue = defaultValue;
    }

    // Slots \\

    public int toSlot(int axis) {
        return ordinal() * AXIS_COUNT + axis;
    }

    public static AnimationChannel fromSlot(int slot) {
        return VALUES[slot / AXIS_COUNT];
    }

    public static int toAxis(int slot) {
        return slot % AXIS_COUNT;
    }

    // Accessible \\

    public String getField() {
        return field;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public static AnimationChannel fromField(String field) {

        for (AnimationChannel channel : VALUES)
            if (channel.field.equals(field))
                return channel;

        return null;
    }
}
