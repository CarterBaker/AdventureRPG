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
import editor.runtime.tools.ToolsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ToolsMenuSystem extends SystemPackage {

    /*
     * Opens the Tools menu: under the name of what the tools target works on,
     * one row per tool it takes, the tool in use highlighted and every row
     * carrying its tool's name, and below them the brush size and the brush
     * preset between their buttons, or a dash where the canvas takes no
     * brush. Everything is rebuilt only when the tools or the brushes move.
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

    // Status
    private int shownToolRevision;
    private int shownBrushRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.toolElements = new ObjectArrayList<>();

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

        refreshTools(canvas);
        setEntryText(ToolsSetting.ENTRY_TITLE, canvas != null ? canvas.getLabel() : ToolsSetting.TITLE_NO_CANVAS);
        setEntryText(ToolsSetting.ENTRY_SIZE, describeSize(canvas));
        setEntryText(ToolsSetting.ENTRY_BRUSH, canvas != null && canvas.takesBrush()
                ? brushManager.getActiveBrush().getDisplayName()
                : ToolsSetting.NO_VALUE);
    }

    // Rows \\

    private void refreshTools(ToolCanvas canvas) {

        for (int i = 0; i < toolElements.size(); i++)
            menuManager.eject(toolsMenu, ToolsSetting.ENTRY_TOOLS, toolElements.get(i));

        toolElements.clear();

        if (canvas == null)
            return;

        EditorTool[] tools = canvas.getTools();
        EditorTool selected = toolManager.getTool(canvas);

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

                    ElementInstance label = element.findChildById(ToolsSetting.ELEMENT_TOOL_LABEL);

                    if (label != null)
                        label.setFontText(tool.getLabel());
                });
    }

    // Status \\

    private String describeSize(ToolCanvas canvas) {

        if (canvas == null || !canvas.takesBrush())
            return ToolsSetting.NO_VALUE;

        return ToolsSetting.SIZE_PREFIX + Math.round(toolManager.getSize(canvas));
    }

    private void setEntryText(int entry, String text) {

        ElementInstance label = toolsMenu.getEntryPoint(entry);

        if (label != null)
            label.setFontText(text);
    }
}
