package editor.runtime.menueventsmanager.menus;

import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolOption;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgUtility;

public class ToolBranch extends BranchPackage {

    /*
     * Menu event handlers for the Tools and Brushes panels, and the shared
     * tool keys. Tools and their options work on the canvas the tools
     * target; the brush chosen is the tip every canvas strokes with. Tool
     * rows carry their tool's name, option buttons their option's and brush
     * rows their brush's.
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

    // Options \\

    public void lowerOption(String optionName) {
        toolManager.stepOption(ArpgUtility.toEnum(optionName, ToolOption.class), -1);
    }

    public void raiseOption(String optionName) {
        toolManager.stepOption(ArpgUtility.toEnum(optionName, ToolOption.class), 1);
    }

    public void shrinkBrush() {
        toolManager.stepOption(ToolOption.SIZE, -1);
    }

    public void growBrush() {
        toolManager.stepOption(ToolOption.SIZE, 1);
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
}
