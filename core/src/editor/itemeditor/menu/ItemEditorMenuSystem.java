package editor.itemeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.itemeditor.ItemEditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class ItemEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Item Editor toolbar over the viewport and rewrites its status
     * line when the editor's revision moves.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;
    private ItemEditorManager itemEditorManager;

    // Menus
    private MenuInstance toolbarMenu;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
        this.itemEditorManager = get(ItemEditorManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(ItemEditorSetting.MENU_TOOLBAR, window);
    }

    @Override
    protected void dispose() {

        menuManager.closeMenu(toolbarMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Update \\

    @Override
    protected void update() {

        if (itemEditorManager.getRevision() == shownRevision)
            return;

        shownRevision = itemEditorManager.getRevision();

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(ItemEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(itemEditorManager.getStatusText());
    }
}
