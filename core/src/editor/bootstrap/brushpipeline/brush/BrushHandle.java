package editor.bootstrap.brushpipeline.brush;

import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushShape;
import engine.root.HandlePackage;

public class BrushHandle extends HandlePackage {

    /*
     * Persistent reference to one brush. Registered and owned by
     * BrushManager. Delegates all accessors through BrushData, which a live
     * edit of the brush replaces in place, so every tool holding it strokes
     * with the edit at once.
     */

    // Internal
    private BrushData brushData;

    // Constructor \\

    public void constructor(BrushData brushData) {

        // Internal
        this.brushData = brushData;
    }

    // Accessible \\

    public String getBrushName() {
        return brushData.getBrushName();
    }

    public int getBrushID() {
        return brushData.getBrushID();
    }

    public String getDisplayName() {
        return brushData.getDisplayName();
    }

    public BrushShape getShape() {
        return brushData.getShape();
    }

    public float getValue(BrushField field) {
        return brushData.getValue(field);
    }
}
