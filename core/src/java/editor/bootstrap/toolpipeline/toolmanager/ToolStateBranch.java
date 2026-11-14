package editor.bootstrap.toolpipeline.toolmanager;

import java.io.File;

import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class ToolStateBranch extends BranchPackage {

    /*
     * Keeps the tools as they were left between sessions, in a user file in
     * the editor's bin directory beside its layouts: the brush in use, and
     * for every canvas the tool chosen and the value of every option it
     * honours, numbers kept to the precision the Tools panel steps them by.
     * A file that is missing or unreadable leaves every default as it is, and
     * anything in it that no longer exists — a brush, a canvas, a tool a
     * canvas no longer takes, an option it no longer honours — is passed
     * over, every value held to the canvas's range.
     */

    // Internal
    private ToolManager toolManager;
    private BrushManager brushManager;

    // File
    private File stateFile;

    // Base \\

    @Override
    protected void create() {

        // File
        this.stateFile = ArpgUtility.resolveFile(
                new File(internal.path, EngineSetting.BIN_DIRECTORY),
                EditorSetting.TOOL_STATE_FILE_NAME);
    }

    @Override
    protected void get() {
        this.toolManager = get(ToolManager.class);
        this.brushManager = get(BrushManager.class);
    }

    // Restore \\

    void restore() {

        ArpgObjectStruct stateArpg = ArpgUtility.tryLoadObject(stateFile);

        if (stateArpg == null)
            return;

        String brushName = ArpgUtility.getString(stateArpg, EditorSetting.TOOL_STATE_KEY_BRUSH, "");

        if (brushManager.hasBrush(brushName))
            brushManager.selectBrush(brushName);

        if (!ArpgUtility.hasObject(stateArpg, EditorSetting.TOOL_STATE_KEY_CANVASES))
            return;

        ArpgObjectStruct canvasesArpg = stateArpg.getAsObject(EditorSetting.TOOL_STATE_KEY_CANVASES);

        for (int i = 0; i < ToolCanvas.VALUES.length; i++) {

            String canvasName = ArpgUtility.toEnumName(ToolCanvas.VALUES[i]);

            if (ArpgUtility.hasObject(canvasesArpg, canvasName))
                restoreCanvas(ToolCanvas.VALUES[i], canvasesArpg.getAsObject(canvasName));
        }
    }

    private void restoreCanvas(ToolCanvas canvas, ArpgObjectStruct canvasArpg) {

        EditorTool tool = findTool(canvas, ArpgUtility.getString(canvasArpg, EditorSetting.TOOL_STATE_KEY_TOOL, ""));

        if (tool != null)
            toolManager.setTool(canvas, tool);

        if (!ArpgUtility.hasObject(canvasArpg, EditorSetting.TOOL_STATE_KEY_OPTIONS))
            return;

        ArpgObjectStruct optionsArpg = canvasArpg.getAsObject(EditorSetting.TOOL_STATE_KEY_OPTIONS);

        for (int i = 0; i < ToolOption.VALUES.length; i++) {

            String optionName = ArpgUtility.toEnumName(ToolOption.VALUES[i]);

            if (ArpgUtility.hasNumber(optionsArpg, optionName))
                toolManager.setOption(canvas, ToolOption.VALUES[i], optionsArpg.get(optionName).getAsFloat());
        }
    }

    // The canvas's tool written under a name, null for one it does not take
    private EditorTool findTool(ToolCanvas canvas, String toolName) {

        EditorTool[] tools = canvas.getTools();

        for (int i = 0; i < tools.length; i++)
            if (ArpgUtility.toEnumName(tools[i]).equals(toolName))
                return tools[i];

        return null;
    }

    // Save \\

    void save() {

        ArpgObjectStruct stateArpg = new ArpgObjectStruct();
        ArpgObjectStruct canvasesArpg = new ArpgObjectStruct();

        stateArpg.addProperty(EditorSetting.TOOL_STATE_KEY_BRUSH, brushManager.getActiveBrush().getBrushName());

        for (int i = 0; i < ToolCanvas.VALUES.length; i++)
            canvasesArpg.add(ArpgUtility.toEnumName(ToolCanvas.VALUES[i]), buildCanvas(ToolCanvas.VALUES[i]));

        stateArpg.add(EditorSetting.TOOL_STATE_KEY_CANVASES, canvasesArpg);
        ArpgUtility.tryWriteObject(stateFile, stateArpg);
    }

    private ArpgObjectStruct buildCanvas(ToolCanvas canvas) {

        ArpgObjectStruct canvasArpg = new ArpgObjectStruct();
        ArpgObjectStruct optionsArpg = new ArpgObjectStruct();
        float precision = EditorSetting.TOOL_OPTION_PRECISION;

        canvasArpg.addProperty(EditorSetting.TOOL_STATE_KEY_TOOL, ArpgUtility.toEnumName(toolManager.getTool(canvas)));

        for (int i = 0; i < ToolOption.VALUES.length; i++) {

            ToolOption option = ToolOption.VALUES[i];

            if (canvas.findOption(option) != null)
                optionsArpg.addProperty(
                        ArpgUtility.toEnumName(option),
                        Math.round(toolManager.getOption(canvas, option) * precision) / (double) precision);
        }

        canvasArpg.add(EditorSetting.TOOL_STATE_KEY_OPTIONS, optionsArpg);

        return canvasArpg;
    }
}
