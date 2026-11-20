package editor.bootstrap.recorderpipeline.recording;

import engine.util.mathematics.curves.CurveEnds;
import engine.util.mathematics.curves.CurveStruct;

public class RecordingCurveStruct extends CurveStruct {

    /*
     * One property's curve in a take: the engine's curve of keys and handles
     * (CurveStruct), running on along its first and last spans as a recorded
     * take does, or as an animation clip's ends say for a bone's, named by
     * the property it carries and, for a mast, sail or door, the part it
     * moves. Its revision moves on every change, so what
     * was packed of it is known to still hold.
     */

    // Curve
    private final RecorderCurveProperty property;
    private final int part;

    // Constructor \\

    public RecordingCurveStruct(RecorderCurveProperty property, int part) {
        this(property, part, CurveEnds.OPEN, 0.0);
    }

    public RecordingCurveStruct(RecorderCurveProperty property, int part, CurveEnds ends, double restValue) {

        super(ends, restValue);

        // Curve
        this.property = property;
        this.part = part;
    }

    // Accessible \\

    public RecorderCurveProperty getProperty() {
        return property;
    }

    public int getPart() {
        return part;
    }
}
