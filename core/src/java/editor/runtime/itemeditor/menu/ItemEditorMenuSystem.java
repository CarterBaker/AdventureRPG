package editor.runtime.itemeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.runtime.itemeditor.ItemEditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class ItemEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Item Editor's status bar over the viewport. When the editor's
     * revision or the tools' moves it rewrites the status line, which names
     * the item, the part the tools lay, its material and the tool in use. The
     * parts themselves are the Layers tab's.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ItemEditorManager itemEditorManager;
    private ToolManager toolManager;

    // Menus
    private MenuInstance toolbarMenu;

    // Status
    private int shownRevision;
    private int shownToolRevision;

    // Base \\

    @Override
    protected void create() {

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownToolRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.itemEditorManager = get(ItemEditorManager.class);
        this.toolManager = get(ToolManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(ItemEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (itemEditorManager.getRevision() == shownRevision && toolManager.getRevision() == shownToolRevision)
            return;

        shownRevision = itemEditorManager.getRevision();
        shownToolRevision = toolManager.getRevision();

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(ItemEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(itemEditorManager.getStatusText());
    }
}
