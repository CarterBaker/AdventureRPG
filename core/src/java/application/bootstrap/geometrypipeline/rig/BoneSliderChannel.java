package application.bootstrap.geometrypipeline.rig;

import engine.root.EngineSetting;

public enum BoneSliderChannel {

    /*
     * One of the nine ways a bone can be a character slider: its joint moved
     * along an axis in blocks under its parent, turned about an axis in
     * degrees on its bind rotation, or its skin sized along an axis as a
     * fraction. A rig names the channels each bone offers as sliders; a left
     * and right pair offers one slider, the right side taking the mirror of
     * the move across x and of the turns about y and z, as a mirrored pose
     * does. Each kind carries the range a new slider of it starts with.
     */

    MOVE_X(0, 0, -1f),
    MOVE_Y(0, 1, 1f),
    MOVE_Z(0, 2, 1f),
    TURN_X(1, 0, 1f),
    TURN_Y(1, 1, -1f),
    TURN_Z(1, 2, -1f),
    SIZE_X(2, 0, 1f),
    SIZE_Y(2, 1, 1f),
    SIZE_Z(2, 2, 1f);

    // Values
    public static final BoneSliderChannel[] VALUES = values();

    // Kinds
    public static final int KIND_MOVE = 0;
    public static final int KIND_TURN = 1;
    public static final int KIND_SIZE = 2;

    // Internal
    private final int kind;
    private final int axis;
    private final float mirrorSign;

    // Constructor \\

    BoneSliderChannel(int kind, int axis, float mirrorSign) {
        this.kind = kind;
        this.axis = axis;
        this.mirrorSign = mirrorSign;
    }

    // Accessible \\

    // Move, turn or size
    public int getKind() {
        return kind;
    }

    public int getAxis() {
        return axis;
    }

    // What the channel's value is multiplied by on the right side of a mirrored pair
    public float getMirrorSign() {
        return mirrorSign;
    }

    // The bit a bone's channel mask holds this channel in
    public int getBit() {
        return 1 << ordinal();
    }

    // How far either way a new slider of this channel slides, in its own unit
    public float getDefaultRange() {
        return switch (kind) {
            case KIND_MOVE -> EngineSetting.BONE_SLIDER_MOVE_RANGE;
            case KIND_TURN -> EngineSetting.BONE_SLIDER_TURN_RANGE;
            default -> EngineSetting.BONE_SLIDER_SIZE_RANGE;
        };
    }
}
