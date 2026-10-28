package editor.runtime.menueventsmanager.menus;

import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushMode;
import editor.bootstrap.brushpipeline.util.BrushShape;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgUtility;

public class ToolBranch extends BranchPackage {

    /*
     * Menu event handlers for the Tools and Brushes panels, and the shared
     * tool keys. Tools and the brush size work on the canvas the tools
     * target; brush presets, shapes, modes and numbers work on the preset in
     * use, which every canvas strokes with. Tool rows carry their tool's
     * name, preset rows their preset's, shape and mode buttons their shape's
     * or mode's, and number steppers their field's.
     */

    // Internal
    private ToolManager toolManager;
    private BrushManager brushManager;

    // Base \\

    @Override
    protected void get() {
        this.toolManager = get(ToolManager.class);
        this.brushManager = get(BrushManager.class);
    }

    // Tools \\

    public void selectTool(String toolName) {
        toolManager.selectTool(ArpgUtility.toEnum(toolName, EditorTool.class));
    }

    public void shrinkBrush() {
        toolManager.resizeBrush(-1);
    }

    public void growBrush() {
        toolManager.resizeBrush(1);
    }

    // Brushes \\

    public void selectBrush(String brushName) {
        brushManager.selectBrush(brushName);
    }

    public void previousBrush() {
        brushManager.cycleBrush(-1);
    }

    public void nextBrush() {
        brushManager.cycleBrush(1);
    }

    public void editBrush() {
        brushManager.editActiveBrush();
    }

    public void selectBrushShape(String shapeName) {
        brushManager.setShape(ArpgUtility.toEnum(shapeName, BrushShape.class));
    }

    public void selectBrushMode(String modeName) {
        brushManager.setMode(ArpgUtility.toEnum(modeName, BrushMode.class));
    }

    public void lowerBrushValue(String fieldName) {
        brushManager.stepValue(ArpgUtility.toEnum(fieldName, BrushField.class), -1);
    }

    public void raiseBrushValue(String fieldName) {
        brushManager.stepValue(ArpgUtility.toEnum(fieldName, BrushField.class), 1);
    }
}
