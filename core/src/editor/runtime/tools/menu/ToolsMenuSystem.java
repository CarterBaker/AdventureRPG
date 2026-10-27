package editor.runtime.tools.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorTool;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import editor.runtime.tools.ToolsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ToolsMenuSystem extends SystemPackage {

    /*
     * Opens the Tools menu: one row per World Editor tool under its mode's
     * header, the tool in use highlighted and every row carrying its tool's
     * name, and the brush size between its buttons. The rows are rebuilt only
     * when the tool in use changes, and the size label only when it does.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldEditorManager worldEditorManager;

    // Menus
    private MenuInstance toolsMenu;

    // Rows
    private WorldEditorTool[] tools;
    private ObjectArrayList<ElementInstance> toolElements;
    private WorldEditorTool shownTool;

    // Status
    private int shownRadius;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.tools = WorldEditorTool.values();
        this.toolElements = new ObjectArrayList<>();

        // Status
        this.shownRadius = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
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

        refreshRadius();

        if (worldEditorManager.getTool() == shownTool)
            return;

        shownTool = worldEditorManager.getTool();
        refreshTools();
    }

    // Status \\

    private void refreshRadius() {

        int radius = worldEditorManager.getBrushRadius();

        if (radius == shownRadius)
            return;

        shownRadius = radius;

        ElementInstance label = toolsMenu.getEntryPoint(ToolsSetting.ENTRY_RADIUS);

        if (label != null)
            label.setFontText(EditorSetting.WORLD_EDITOR_STATUS_RADIUS + radius);
    }

    // Rows \\

    private void refreshTools() {

        for (int i = 0; i < toolElements.size(); i++)
            menuManager.eject(toolsMenu, resolveEntry(tools[i]), toolElements.get(i));

        toolElements.clear();

        for (int i = 0; i < tools.length; i++)
            toolElements.add(injectTool(tools[i]));
    }

    private ElementInstance injectTool(WorldEditorTool tool) {

        String template = tool == shownTool
                ? ToolsSetting.TEMPLATE_TOOL_ROW_SELECTED
                : ToolsSetting.TEMPLATE_TOOL_ROW;

        return menuManager.inject(
                toolsMenu, resolveEntry(tool), template,
                element -> {
                    element.setActionArgOverride(ArpgUtility.toEnumName(tool));

                    ElementInstance label = element.findChildById(ToolsSetting.ELEMENT_TOOL_LABEL);

                    if (label != null)
                        label.setFontText(tool.getLabel());
                });
    }

    // Utility \\

    private int resolveEntry(WorldEditorTool tool) {
        return ToolsSetting.ENTRY_MODE_TOOLS[tool.getMode().ordinal()];
    }
}
