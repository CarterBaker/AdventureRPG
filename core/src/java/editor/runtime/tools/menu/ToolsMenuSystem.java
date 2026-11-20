package editor.runtime.tools.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
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
     * the target window named after it in parentheses,
     * one row per tool it takes, the tool in use highlighted and every row
     * carrying its tool's name, then each group of actions it lists under its
     * title, every action row carrying the action's key; and under that
     * tool's name, one row per option both it and the canvas have, laid out
     * for its kind: a number on a slider between step buttons and in a box
     * that opens for typing, a switch filled while on, and a choice naming
     * the one in use, its drop-down listing every choice under it while open,
     * every part carrying the option's name and every choice its own. The brush a tool strokes with is
     * chosen in the Brushes tab, never here. The rows are rebuilt only when
     * the target or its tool changes; a value that moves, as under a dragged
     * slider, is set on its row in place.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ToolManager toolManager;
    private ActiveWindowManager activeWindowManager;

    // Menus
    private MenuInstance toolsMenu;

    // Rows
    private ObjectArrayList<ElementInstance> toolElements;
    private ObjectArrayList<ElementInstance> optionElements;
    private ObjectArrayList<ToolOption> shownOptions;
    private ObjectArrayList<ElementInstance> choiceElements;

    // Colors
    private MenuColorStruct switchOnColor;
    private MenuColorStruct switchOffColor;

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
        this.choiceElements = new ObjectArrayList<>();

        // Colors
        this.switchOnColor = new MenuColorStruct(ThemeColor.ACCENT, ToolsSetting.SWITCH_ON_ALPHA);
        this.switchOffColor = new MenuColorStruct(ThemeColor.ACCENT, ToolsSetting.SWITCH_OFF_ALPHA);

        // Status
        this.shownToolRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownOptionRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.toolManager = get(ToolManager.class);
        this.activeWindowManager = get(ActiveWindowManager.class);
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
        clear(choiceElements, ToolsSetting.ENTRY_OPTIONS);
        shownOptions.clear();
        setEntryText(
                ToolsSetting.ENTRY_CANVAS_TITLE,
                canvas != null
                        ? activeWindowManager.nameContext(canvas.getLabel(), toolManager.getCanvasWindow())
                        : ToolsSetting.TITLE_NO_CANVAS);
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

    // Every option both the tool and the canvas have, then every canvas option the canvas honours, last
    private void refreshOptions(ToolCanvas canvas, EditorTool tool) {

        ToolOption[] options = tool.getOptions();

        for (int i = 0; i < options.length; i++)
            showOption(canvas, options[i]);

        for (int i = 0; i < ToolOption.VALUES.length; i++)
            if (ToolOption.VALUES[i].isCanvasWide())
                showOption(canvas, ToolOption.VALUES[i]);
    }

    private void showOption(ToolCanvas canvas, ToolOption option) {

        ToolOptionStruct range = canvas.findOption(option);

        if (range == null)
            return;

        float value = toolManager.getOption(canvas, option);
        ElementInstance row = injectOption(option, range);

        optionElements.add(row);
        shownOptions.add(option);
        fillValue(row, range, value);

        if (toolManager.getOpenChoice() == option)
            showChoices(range, Math.round(value));
    }

    private ElementInstance injectOption(ToolOption option, ToolOptionStruct range) {

        String arg = ArpgUtility.toEnumName(option);

        return menuManager.inject(
                toolsMenu, ToolsSetting.ENTRY_OPTIONS, ToolsSetting.TEMPLATE_OPTION_ROWS[range.getKind().ordinal()],
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

    // The open drop-down's rows under its option's row, one per choice, the one in use highlighted
    private void showChoices(ToolOptionStruct range, int selected) {

        for (int i = 0; i < range.getChoiceCount(); i++) {

            String choiceName = range.format(i);
            String template = i == selected
                    ? ToolsSetting.TEMPLATE_CHOICE_ROW_SELECTED
                    : ToolsSetting.TEMPLATE_CHOICE_ROW;

            choiceElements.add(menuManager.inject(
                    toolsMenu, ToolsSetting.ENTRY_OPTIONS, template,
                    element -> {
                        element.setActionArgOverride(choiceName);
                        setChildText(element, ToolsSetting.ELEMENT_CHOICE_LABEL, choiceName);
                    }));
        }
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

        ElementInstance fill = row.findChildById(ToolsSetting.ELEMENT_OPTION_SWITCH_FILL);

        if (fill != null)
            fill.setColorOverride(value > 0f ? switchOnColor : switchOffColor);

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
