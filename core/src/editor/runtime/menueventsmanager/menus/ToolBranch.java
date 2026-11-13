package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.toolpipeline.tool.ToolOptionStruct;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import editor.runtime.slider.SliderUtility;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgUtility;

public class ToolBranch extends BranchPackage {

    /*
     * Menu event handlers for the Tools and Brushes panels, and the shared
     * tool keys. Tools and their options work on the canvas the tools
     * target, and an action runs on the target's window; an option is
     * stepped by its buttons, set by its slider at the share of its range
     * under the pointer, or typed into its box. The brush chosen is the tip
     * every canvas strokes with, and choosing one shows it in the Info Panel.
     * Tool rows carry their tool's name, action rows their action's key,
     * option buttons, sliders and boxes their option's name and brush rows
     * their brush's.
     */

    // Internal
    private InputManager inputManager;
    private ToolManager toolManager;
    private BrushManager brushManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
        this.toolManager = get(ToolManager.class);
        this.brushManager = get(BrushManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Tools \\

    public void selectTool(String toolName) {
        toolManager.selectTool(ArpgUtility.toEnum(toolName, EditorTool.class));
    }

    // Actions \\

    public void runAction(String actionKey) {
        toolManager.runAction(actionKey);
    }

    // Options \\

    public void lowerOption(String optionName) {
        toolManager.stepOption(ArpgUtility.toEnum(optionName, ToolOption.class), -1);
    }

    public void raiseOption(String optionName) {
        toolManager.stepOption(ArpgUtility.toEnum(optionName, ToolOption.class), 1);
    }

    // The option set to the share of its range under the pointer, on the nearest step
    public void dragOption(String optionName, WindowInstance window, ElementInstance track) {

        ToolOption option = ArpgUtility.toEnum(optionName, ToolOption.class);
        ToolOptionStruct range = toolManager.findTargetOption(option);

        if (range != null)
            toolManager.setTargetOption(
                    option,
                    range.fromFraction(SliderUtility.resolveFraction(inputManager.getHoverMouseX(window), track)));
    }

    // The option's value opened for typing, any number taken and held to the option's range
    public void editOption(String optionName, WindowInstance window) {

        ToolOption option = ArpgUtility.toEnum(optionName, ToolOption.class);
        ToolOptionStruct range = toolManager.findTargetOption(option);

        if (range == null)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                range.format(toolManager.getOption(toolManager.getCanvas(), option)),
                range::accepts,
                text -> toolManager.setTargetOption(option, range.parse(text)));
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
        brushManager.showActiveBrush();
    }

    public void previousBrush() {
        brushManager.cycleBrush(-1);
        brushManager.showActiveBrush();
    }

    public void nextBrush() {
        brushManager.cycleBrush(1);
        brushManager.showActiveBrush();
    }
}
