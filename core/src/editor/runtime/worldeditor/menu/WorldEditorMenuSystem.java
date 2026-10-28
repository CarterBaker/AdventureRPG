package editor.runtime.worldeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorMode;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import editor.runtime.worldeditor.WorldEditorSetting;
import editor.runtime.worldeditor.render.WorldEditorRenderSystem;
import editor.runtime.worldeditor.tool.WorldEditorToolSystem;
import engine.root.SystemPackage;

public class WorldEditorMenuSystem extends SystemPackage {

    /*
     * Opens the World Editor toolbar and status line over the canvas. The
     * status line names the image, mode, tool and what it paints or places,
     * with the pixel, biome and height under the pointer, and rewrites only
     * on change, as do the mode the editor follows and the layer and player
     * marker buttons' labels. The tools, the brushes and the palette are
     * shown in their own tabs.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldEditorManager worldEditorManager;
    private WorldEditorToolSystem worldEditorToolSystem;
    private WorldEditorRenderSystem worldEditorRenderSystem;

    // Menus
    private MenuInstance toolbarMenu;

    // Status
    private String shownStatus;
    private String shownPlayers;
    private WorldEditorLayer shownLayer;
    private WorldEditorMode shownMode;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.worldEditorToolSystem = get(WorldEditorToolSystem.class);
        this.worldEditorRenderSystem = get(WorldEditorRenderSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(WorldEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        refreshStatus();
        refreshPlayers();
        refreshLayer();
        refreshMode();
    }

    // Status \\

    private void refreshStatus() {

        String status = worldEditorToolSystem.isHovering()
                ? worldEditorManager.getStatusText() + EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR
                        + worldEditorManager.describePixel(
                                worldEditorToolSystem.getHoveredX(), worldEditorToolSystem.getHoveredY())
                : worldEditorManager.getStatusText();

        if (status.equals(shownStatus))
            return;

        shownStatus = status;
        setEntryText(WorldEditorSetting.ENTRY_STATUS, status);
    }

    private void refreshPlayers() {

        String players = worldEditorRenderSystem.isShowingPlayers()
                ? EditorSetting.WORLD_EDITOR_PLAYERS_ON
                : EditorSetting.WORLD_EDITOR_PLAYERS_OFF;

        if (players.equals(shownPlayers))
            return;

        shownPlayers = players;
        setEntryText(WorldEditorSetting.ENTRY_PLAYERS_LABEL, players);
    }

    private void refreshLayer() {

        WorldEditorLayer layer = worldEditorManager.getLayer();

        if (layer == shownLayer)
            return;

        shownLayer = layer;
        setEntryText(WorldEditorSetting.ENTRY_LAYER_LABEL, layer.getLabel());
    }

    private void refreshMode() {

        WorldEditorMode mode = worldEditorManager.getMode();

        if (mode == shownMode)
            return;

        shownMode = mode;
        setEntryText(WorldEditorSetting.ENTRY_MODE_LABEL, mode.getLabel());
    }

    private void setEntryText(int entry, String text) {

        ElementInstance label = toolbarMenu.getEntryPoint(entry);

        if (label != null)
            label.setFontText(text);
    }
}
