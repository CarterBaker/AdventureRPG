package editor.bootstrap.brushpipeline.brushmanager;

import editor.bootstrap.brushpipeline.brush.BrushData;
import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushMode;
import editor.bootstrap.brushpipeline.util.BrushShape;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class BrushBuilder extends BuilderPackage {

    /*
     * Parses one brush preset's ARPG object into BrushData, from its file at
     * boot or from the Info Panel's live copy while it is edited. Every field
     * is optional: an empty or missing display name falls back to the
     * preset's name, the shape to round, the mode to paint, and each number
     * to its field's default, held to the field's range.
     */

    // Internal
    private BrushManager brushManager;

    // Base \\

    @Override
    protected void get() {
        this.brushManager = get(BrushManager.class);
    }

    // Build \\

    BrushData build(String brushName, ArpgObjectStruct brushArpg) {

        float[] values = new float[BrushField.VALUES.length];

        for (int i = 0; i < BrushField.VALUES.length; i++) {

            BrushField field = BrushField.VALUES[i];

            values[i] = field.clamp(ArpgUtility.getFloat(brushArpg, field.getKey(), field.getDefaultValue()));
        }

        String displayName = ArpgUtility.getString(brushArpg, "display_name", "");

        return new BrushData(
                brushName,
                brushManager.registerBrushName(brushName),
                displayName.isEmpty() ? brushName : displayName,
                ArpgUtility.getEnum(brushArpg, "shape", BrushShape.class, BrushShape.ROUND),
                ArpgUtility.getEnum(brushArpg, "mode", BrushMode.class, BrushMode.PAINT),
                values);
    }
}
