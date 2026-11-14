package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecorderCurveProperty {

    /*
     * Every value a take holds that a curve can carry, each with the words
     * the Keys tab shows for it and the kind of value it is. Positions are
     * read across chunks from where a track began, so a curve of one runs
     * on unbroken. The camera's look and the turn of items and vehicles are
     * kept as yaw, pitch and roll, so each bends on its own. A vehicle has
     * one brace, hoist or opening for each of its masts, sails and doors.
     * The lists say which a camera, a character, an item, a vehicle, the
     * weather and the clock hold, in the order the Keys tab lists them.
     */

    POSITION_X(EditorSetting.RECORDER_CURVE_LABEL_POSITION_X, RecorderCurveKind.DISTANCE),
    POSITION_Y(EditorSetting.RECORDER_CURVE_LABEL_POSITION_Y, RecorderCurveKind.DISTANCE),
    POSITION_Z(EditorSetting.RECORDER_CURVE_LABEL_POSITION_Z, RecorderCurveKind.DISTANCE),
    YAW(EditorSetting.RECORDER_CURVE_LABEL_YAW, RecorderCurveKind.ANGLE),
    PITCH(EditorSetting.RECORDER_CURVE_LABEL_PITCH, RecorderCurveKind.DEGREES),
    ROLL(EditorSetting.RECORDER_CURVE_LABEL_ROLL, RecorderCurveKind.ANGLE),
    SHAKE_YAW(EditorSetting.RECORDER_CURVE_LABEL_SHAKE_YAW, RecorderCurveKind.DEGREES),
    SHAKE_PITCH(EditorSetting.RECORDER_CURVE_LABEL_SHAKE_PITCH, RecorderCurveKind.DEGREES),
    SHAKE_ROLL(EditorSetting.RECORDER_CURVE_LABEL_SHAKE_ROLL, RecorderCurveKind.DEGREES),
    FIELD_OF_VIEW(EditorSetting.RECORDER_CURVE_LABEL_FIELD_OF_VIEW, RecorderCurveKind.DEGREES),
    FACING(EditorSetting.RECORDER_CURVE_LABEL_FACING, RecorderCurveKind.ANGLE),
    TURN_RATE(EditorSetting.RECORDER_CURVE_LABEL_TURN_RATE, RecorderCurveKind.SCALAR),
    LOOK_YAW(EditorSetting.RECORDER_CURVE_LABEL_LOOK_YAW, RecorderCurveKind.ANGLE),
    LOOK_PITCH(EditorSetting.RECORDER_CURVE_LABEL_LOOK_PITCH, RecorderCurveKind.DEGREES),
    GROUND_SPEED(EditorSetting.RECORDER_CURVE_LABEL_GROUND_SPEED, RecorderCurveKind.SCALAR),
    RISE_SPEED(EditorSetting.RECORDER_CURVE_LABEL_RISE_SPEED, RecorderCurveKind.SCALAR),
    GROUND_OFFSET(EditorSetting.RECORDER_CURVE_LABEL_GROUND_OFFSET, RecorderCurveKind.DISTANCE),
    RUDDER(EditorSetting.RECORDER_CURVE_LABEL_RUDDER, RecorderCurveKind.DEGREES),
    BRACE(EditorSetting.RECORDER_CURVE_LABEL_BRACE, RecorderCurveKind.DEGREES),
    HOIST(EditorSetting.RECORDER_CURVE_LABEL_HOIST, RecorderCurveKind.SCALAR),
    DOOR(EditorSetting.RECORDER_CURVE_LABEL_DOOR, RecorderCurveKind.SCALAR),
    DRIFT_X(EditorSetting.RECORDER_CURVE_LABEL_DRIFT_X, RecorderCurveKind.DISTANCE),
    DRIFT_Z(EditorSetting.RECORDER_CURVE_LABEL_DRIFT_Z, RecorderCurveKind.DISTANCE),
    GUST(EditorSetting.RECORDER_CURVE_LABEL_GUST, RecorderCurveKind.SCALAR),
    TIME(EditorSetting.RECORDER_CURVE_LABEL_TIME, RecorderCurveKind.CLOCK);

    // Lists
    public static final RecorderCurveProperty[] CAMERA = {
            POSITION_X, POSITION_Y, POSITION_Z, YAW, PITCH, FIELD_OF_VIEW, SHAKE_YAW, SHAKE_PITCH, SHAKE_ROLL };
    public static final RecorderCurveProperty[] BODY = {
            POSITION_X, POSITION_Y, POSITION_Z, FACING, LOOK_YAW, LOOK_PITCH, GROUND_SPEED, RISE_SPEED, TURN_RATE,
            GROUND_OFFSET };
    public static final RecorderCurveProperty[] PROJECTILE = {
            POSITION_X, POSITION_Y, POSITION_Z, YAW, PITCH, ROLL };
    public static final RecorderCurveProperty[] VEHICLE = {
            POSITION_X, POSITION_Y, POSITION_Z, YAW, PITCH, ROLL, RUDDER };
    public static final RecorderCurveProperty[] WEATHER = { DRIFT_X, DRIFT_Z, GUST };
    public static final RecorderCurveProperty[] CLOCK = { TIME };

    // Internal
    private final String label;
    private final RecorderCurveKind kind;

    // Constructor \\

    RecorderCurveProperty(String label, RecorderCurveKind kind) {
        this.label = label;
        this.kind = kind;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public RecorderCurveKind getKind() {
        return kind;
    }

    public boolean isPosition() {
        return this == POSITION_X || this == POSITION_Y || this == POSITION_Z;
    }
}
