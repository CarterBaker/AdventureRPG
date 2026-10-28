package editor.bootstrap.brushpipeline.brush;

import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushShape;
import engine.root.DataPackage;

public class BrushData extends DataPackage {

    /*
     * Immutable definition of one brush: its name, which is its file's path,
     * and its registry ID; the name it is listed by; the shape of its tip; and
     * one value per BrushField, indexed by the field's ordinal.
     */

    // Identity
    private final String brushName;
    private final int brushID;
    private final String displayName;

    // Tip
    private final BrushShape shape;
    private final float[] values;

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
        this.shape = shape;
        this.values = values;
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
}
