package editor.runtime.tools.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.toolpipeline.tool.ToolActionGroupStruct;
import editor.bootstrap.toolpipeline.tool.ToolActionStruct;
import editor.bootstrap.toolpipeline.tool.ToolOptionStruct;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.slider.SliderUtility;
import editor.runtime.tools.ToolsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ToolsMenuSystem extends SystemPackage {

    /*
     * Opens the Tools menu: under the name of what the tools target works on,
     * one row per tool it takes, the tool in use highlighted and every row
     * carrying its tool's name, then each group of actions it lists under its
     * title, every action row carrying the action's key; and under that
     * tool's name, one row per option both it and the canvas have, its value
     * on a slider between step buttons and in a box that opens for typing,
     * every part carrying the option's name. The brush a tool strokes with is
     * chosen in the Brushes tab, never here. The rows are rebuilt only when
     * the target or its tool changes; a value that moves, as under a dragged
     * slider, is set on its row in place.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ToolManager toolManager;

    // Menus
    private MenuInstance toolsMenu;

    // Rows
    private ObjectArrayList<ElementInstance> toolElements;
    private ObjectArrayList<ElementInstance> optionElements;
    private ObjectArrayList<ToolOption> shownOptions;

    // Status
    private int shownToolRevision;
    private int shownOptionRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.toolElements = new ObjectArrayList<>();
        this.optionElements = new ObjectArrayList<>();
        this.shownOptions = new ObjectArrayList<>();

        // Status
        this.shownToolRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownOptionRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.toolManager = get(ToolManager.class);
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

        if (toolManager.getRevision() != shownToolRevision) {
            refreshRows();
            return;
        }

        if (toolManager.getOptionRevision() != shownOptionRevision)
            refreshValues();
    }

    private void refreshRows() {

        shownToolRevision = toolManager.getRevision();
        shownOptionRevision = toolManager.getOptionRevision();

        ToolCanvas canvas = toolManager.getCanvas();
        EditorTool tool = canvas != null ? toolManager.getTool(canvas) : null;

        clear(toolElements, ToolsSetting.ENTRY_TOOLS);
        clear(optionElements, ToolsSetting.ENTRY_OPTIONS);
        shownOptions.clear();
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

        refreshActions(canvas.getActionGroups());
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

    // Actions \\

    private void refreshActions(ToolActionGroupStruct[] groups) {

        for (int i = 0; i < groups.length; i++) {

            ToolActionGroupStruct group = groups[i];
            ToolActionStruct[] actions = group.getActions();

            toolElements.add(menuManager.inject(
                    toolsMenu, ToolsSetting.ENTRY_TOOLS, ToolsSetting.TEMPLATE_ACTION_GROUP_ROW,
                    element -> setChildText(element, ToolsSetting.ELEMENT_ACTION_GROUP_LABEL, group.getTitle())));

            for (int j = 0; j < actions.length; j++)
                toolElements.add(injectAction(actions[j]));
        }
    }

    private ElementInstance injectAction(ToolActionStruct action) {
        return menuManager.inject(
                toolsMenu, ToolsSetting.ENTRY_TOOLS, ToolsSetting.TEMPLATE_ACTION_ROW,
                element -> {
                    element.setActionArgOverride(action.getKey());
                    setChildText(element, ToolsSetting.ELEMENT_ACTION_LABEL, action.getLabel());
                });
    }

    // Options \\

    // Every option both the tool and the canvas have
    private void refreshOptions(ToolCanvas canvas, EditorTool tool) {

        ToolOption[] options = tool.getOptions();

        for (int i = 0; i < options.length; i++) {

            ToolOptionStruct range = canvas.findOption(options[i]);

            if (range == null)
                continue;

            ElementInstance row = injectOption(options[i]);

            optionElements.add(row);
            shownOptions.add(options[i]);
            fillValue(row, range, toolManager.getOption(canvas, options[i]));
        }
    }

    private ElementInstance injectOption(ToolOption option) {

        String arg = ArpgUtility.toEnumName(option);

        return menuManager.inject(
                toolsMenu, ToolsSetting.ENTRY_OPTIONS, ToolsSetting.TEMPLATE_OPTION_ROW,
                element -> {
                    setChildText(element, ToolsSetting.ELEMENT_OPTION_LABEL, option.getLabel());
                    setChildArg(element, ToolsSetting.ELEMENT_OPTION_LOWER, arg);
                    setChildArg(element, ToolsSetting.ELEMENT_OPTION_RAISE, arg);
                    setChildArg(element, ToolsSetting.ELEMENT_OPTION_VALUE_BUTTON, arg);

                    ElementInstance track = element.findChildById(ToolsSetting.ELEMENT_OPTION_TRACK);

                    if (track != null)
                        track.setOnDragArgOverride(arg);
                });
    }

    // Every option row's value and knob set where its option now stands, nothing listed again
    private void refreshValues() {

        shownOptionRevision = toolManager.getOptionRevision();

        ToolCanvas canvas = toolManager.getCanvas();

        if (canvas == null)
            return;

        for (int i = 0; i < optionElements.size(); i++) {

            ToolOption option = shownOptions.get(i);
            ToolOptionStruct range = canvas.findOption(option);

            if (range != null)
                fillValue(optionElements.get(i), range, toolManager.getOption(canvas, option));
        }
    }

    private void fillValue(ElementInstance row, ToolOptionStruct range, float value) {

        setChildText(row, ToolsSetting.ELEMENT_OPTION_VALUE, range.format(value));

        ElementInstance track = row.findChildById(ToolsSetting.ELEMENT_OPTION_TRACK);

        if (track != null)
            SliderUtility.placeKnob(
                    track,
                    ToolsSetting.ELEMENT_OPTION_KNOB,
                    SliderUtility.resolveStep(range.toFraction(value)));
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
