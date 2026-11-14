package editor.bootstrap.brushpipeline.brush;

import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushShape;
import engine.root.DataPackage;

public class BrushData extends DataPackage {

    /*
     * Immutable definition of one brush: its name, which is its file's path,
     * and its registry ID; the name it is listed by; the shape of its tip;
     * one value per BrushField, indexed by the field's ordinal; and the turn
     * and squash its angle and ratio make, worked out once so a stamp only
     * multiplies.
     */

    // Identity
    private final String brushName;
    private final int brushID;
    private final String displayName;

    // Tip
    private final BrushShape shape;
    private final float[] values;
    private final float angleCos;
    private final float angleSin;
    private final float inverseRatio;

    // Constructor \\

    public BrushData(
            String brushName,
            int brushID,
            String displayName,
            BrushShape shape,
            float[] values) {

        // Identity
        this.brushName = brushName;
        this.brushID = brushID;
        this.displayName = displayName;

        // Tip
        double angle = Math.toRadians(values[BrushField.ANGLE.ordinal()]);

        this.shape = shape;
        this.values = values;
        this.angleCos = (float) Math.cos(angle);
        this.angleSin = (float) Math.sin(angle);
        this.inverseRatio = 1f / values[BrushField.RATIO.ordinal()];
    }

    // Accessible \\

    public String getBrushName() {
        return brushName;
    }

    public int getBrushID() {
        return brushID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BrushShape getShape() {
        return shape;
    }

    public float getValue(BrushField field) {
        return values[field.ordinal()];
    }

    public float getAngleCos() {
        return angleCos;
    }

    public float getAngleSin() {
        return angleSin;
    }

    public float getInverseRatio() {
        return inverseRatio;
    }
}
