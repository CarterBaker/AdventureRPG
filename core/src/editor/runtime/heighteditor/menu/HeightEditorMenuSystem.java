package editor.runtime.heighteditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.heighteditor.HeightEditorSetting;
import editor.runtime.worldeditor.tool.WorldEditorToolSystem;
import engine.root.SystemPackage;

public class HeightEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Height Editor status line over the canvas; saving, reverting,
     * undoing and redoing are the editor's File and Edit menus'. It names the
     * image, the tool, brush and size the heights are shaped with, with the
     * pixel, biome and height under the pointer, and rewrites only on change.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldEditorManager worldEditorManager;
    private WorldEditorToolSystem worldEditorToolSystem;

    // Menus
    private MenuInstance toolbarMenu;

    // Status
    private String shownStatus;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.worldEditorToolSystem = get(WorldEditorToolSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(HeightEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        String status = worldEditorManager.getStatusText(
                worldEditorToolSystem.getLayer(),
                worldEditorToolSystem.isHovering(),
                worldEditorToolSystem.getHoveredX(),
                worldEditorToolSystem.getHoveredY());

        if (status.equals(shownStatus))
            return;

        shownStatus = status;

        ElementInstance label = toolbarMenu.getEntryPoint(HeightEditorSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(status);
    }
}
