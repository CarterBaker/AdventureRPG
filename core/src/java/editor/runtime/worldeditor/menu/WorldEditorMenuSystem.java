package editor.runtime.worldeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
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
     * status line names the image, mode, tool and what the layer picked
     * paints or places, with the pixel, biome and height under the pointer,
     * and rewrites only on change, as do the mode the editor follows and the
     * player marker button's label. The layers, tools, brushes and biome
     * palette are shown in their own tabs.
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
        refreshMode();
    }

    // Status \\

    private void refreshStatus() {

        String status = worldEditorManager.getStatusText(
                worldEditorToolSystem.getLayer(),
                worldEditorToolSystem.isHovering(),
                worldEditorToolSystem.getHoveredX(),
                worldEditorToolSystem.getHoveredY());

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
