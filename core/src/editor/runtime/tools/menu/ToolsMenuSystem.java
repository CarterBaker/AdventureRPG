package editor.runtime.tools.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import editor.runtime.tools.ToolsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ToolsMenuSystem extends SystemPackage {

    /*
     * Opens the Tools menu: under the name of what the tools target works on,
     * one row per tool it takes, the tool in use highlighted and every row
     * carrying its tool's name; and under that tool's name, one row per
     * option both it and the canvas have, its value between buttons carrying
     * the option's name, then, for a tool that strokes, the brush it strokes
     * with between the buttons that step through the brushes. Everything is
     * rebuilt only when the tools or the brushes move.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ToolManager toolManager;
    private BrushManager brushManager;

    // Menus
    private MenuInstance toolsMenu;

    // Rows
    private ObjectArrayList<ElementInstance> toolElements;
    private ObjectArrayList<ElementInstance> optionElements;

    // Status
    private int shownToolRevision;
    private int shownBrushRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.toolElements = new ObjectArrayList<>();
        this.optionElements = new ObjectArrayList<>();

        // Status
        this.shownToolRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownBrushRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.toolManager = get(ToolManager.class);
        this.brushManager = get(BrushManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolsMenu = menuManager.openMenu(ToolsSetting.MENU_TOOLS, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (toolManager.getRevision() == shownToolRevision && brushManager.getRevision() == shownBrushRevision)
            return;

        shownToolRevision = toolManager.getRevision();
        shownBrushRevision = brushManager.getRevision();

        ToolCanvas canvas = toolManager.getCanvas();
        EditorTool tool = canvas != null ? toolManager.getTool(canvas) : null;

        clear(toolElements, ToolsSetting.ENTRY_TOOLS);
        clear(optionElements, ToolsSetting.ENTRY_OPTIONS);
        setEntryText(
                ToolsSetting.ENTRY_CANVAS_TITLE,
                canvas != null ? canvas.getLabel() : ToolsSetting.TITLE_NO_CANVAS);
        setEntryText(ToolsSetting.ENTRY_TOOL_TITLE, tool != null
                ? tool.getLabel() + ToolsSetting.TITLE_OPTIONS_SUFFIX
                : ToolsSetting.TITLE_NO_OPTIONS);

        if (canvas == null)
            return;

        refreshTools(canvas, tool);
        refreshOptions(canvas, tool);
    }

    // Tools \\

    private void refreshTools(ToolCanvas canvas, EditorTool selected) {

        EditorTool[] tools = canvas.getTools();

        for (int i = 0; i < tools.length; i++)
            toolElements.add(injectTool(tools[i], tools[i] == selected));
    }

    private ElementInstance injectTool(EditorTool tool, boolean selected) {

        String template = selected
                ? ToolsSetting.TEMPLATE_TOOL_ROW_SELECTED
                : ToolsSetting.TEMPLATE_TOOL_ROW;

        return menuManager.inject(
                toolsMenu, ToolsSetting.ENTRY_TOOLS, template,
                element -> {
                    element.setActionArgOverride(ArpgUtility.toEnumName(tool));
                    setChildText(element, ToolsSetting.ELEMENT_TOOL_LABEL, tool.getLabel());
                });
    }

    // Options \\

    // Every option both the tool and the canvas have, then the brush for a tool that strokes with one
    private void refreshOptions(ToolCanvas canvas, EditorTool tool) {

        ToolOption[] options = tool.getOptions();

        for (int i = 0; i < options.length; i++)
            if (canvas.findOption(options[i]) != null)
                optionElements.add(injectOption(options[i], toolManager.getOption(canvas, options[i])));

        if (tool.isStroke() && canvas.takesBrush())
            optionElements.add(menuManager.inject(
                    toolsMenu, ToolsSetting.ENTRY_OPTIONS, ToolsSetting.TEMPLATE_BRUSH_ROW,
                    element -> setChildText(
                            element,
                            ToolsSetting.ELEMENT_BRUSH_LABEL,
                            brushManager.getActiveBrush().getDisplayName())));
    }

    private ElementInstance injectOption(ToolOption option, float value) {

        String arg = ArpgUtility.toEnumName(option);

        return menuManager.inject(
                toolsMenu, ToolsSetting.ENTRY_OPTIONS, ToolsSetting.TEMPLATE_OPTION_ROW,
                element -> {
                    setChildText(element, ToolsSetting.ELEMENT_OPTION_LABEL, option.getLabel());
                    setChildText(element, ToolsSetting.ELEMENT_OPTION_VALUE, formatValue(value));
                    setChildArg(element, ToolsSetting.ELEMENT_OPTION_LOWER, arg);
                    setChildArg(element, ToolsSetting.ELEMENT_OPTION_RAISE, arg);
                });
    }

    // Whole numbers as they are, fractions to the precision their steps move them by
    private String formatValue(float value) {

        float precision = EditorSetting.TOOL_OPTION_PRECISION;

        return value == Math.round(value)
                ? Integer.toString(Math.round(value))
                : Float.toString(Math.round(value * precision) / precision);
    }

    // Utility \\

    private void clear(ObjectArrayList<ElementInstance> elements, int entry) {

        for (int i = 0; i < elements.size(); i++)
            menuManager.eject(toolsMenu, entry, elements.get(i));

        elements.clear();
    }

    private void setEntryText(int entry, String text) {

        ElementInstance label = toolsMenu.getEntryPoint(entry);

        if (label != null)
            label.setFontText(text);
    }

    private void setChildText(ElementInstance element, String childID, String text) {

        ElementInstance child = element.findChildById(childID);

        if (child != null)
            child.setFontText(text);
    }

    private void setChildArg(ElementInstance element, String childID, String arg) {

        ElementInstance child = element.findChildById(childID);

        if (child != null)
            child.setActionArgOverride(arg);
    }
}
