package editor.runtime.faceeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.runtime.faceeditor.FaceEditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class FaceEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Face Editor toolbar and its status bar over the viewport, and
     * rewrites the status line when the editor's revision moves. The editor's
     * panels are the Face Outliner and Face Properties docker windows.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private FaceEditorManager faceEditorManager;

    // Menus
    private MenuInstance toolbarMenu;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.faceEditorManager = get(FaceEditorManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(FaceEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (faceEditorManager.getRevision() == shownRevision)
            return;

        shownRevision = faceEditorManager.getRevision();

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(FaceEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(faceEditorManager.getStatusText());
    }
}
