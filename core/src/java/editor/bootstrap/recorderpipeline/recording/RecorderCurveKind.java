package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecorderCurveKind {

    /*
     * How a curve's values read: a distance in blocks, an angle in degrees
     * that turns round and is played the short way, degrees that never turn
     * round, a plain amount, or the world's clock in milliseconds from its
     * epoch. Each kind knows how near a curve fitted to a take must keep to
     * what was recorded, and the least span of values a graph of it shows.
     */

    DISTANCE(EditorSetting.RECORDER_CURVE_DISTANCE_TOLERANCE, EditorSetting.RECORDER_CURVE_DISTANCE_SPAN),
    ANGLE(EditorSetting.RECORDER_CURVE_ANGLE_TOLERANCE, EditorSetting.RECORDER_CURVE_ANGLE_SPAN),
    DEGREES(EditorSetting.RECORDER_CURVE_ANGLE_TOLERANCE, EditorSetting.RECORDER_CURVE_ANGLE_SPAN),
    SCALAR(EditorSetting.RECORDER_CURVE_SCALAR_TOLERANCE, EditorSetting.RECORDER_CURVE_SCALAR_SPAN),
    CLOCK(EditorSetting.RECORDER_CURVE_CLOCK_TOLERANCE, EditorSetting.RECORDER_CURVE_CLOCK_SPAN);

    // Internal
    private final double tolerance;
    private final double minimumSpan;

    // Constructor \\

    RecorderCurveKind(double tolerance, double minimumSpan) {
        this.tolerance = tolerance;
        this.minimumSpan = minimumSpan;
    }

    // Accessible \\

    public double getTolerance() {
        return tolerance;
    }

    public double getMinimumSpan() {
        return minimumSpan;
    }

    // True for an angle that turns round, fitted unwrapped and played the short way
    public boolean isWrapping() {
        return this == ANGLE;
    }
}
