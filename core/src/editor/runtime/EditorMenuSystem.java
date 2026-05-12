package editor.runtime;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class EditorMenuSystem extends SystemPackage {

    /*
     * Opens the main editor menu against the main window when the editor
     * context awakens and exposes it for canvas-based compositing downstream.
     */

    // Internal
    private WindowManager windowManager;
    private MenuManager menuManager;
    private FboManager fboManager;

    // Menu
    private MenuInstance editorMenu;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.windowManager = get(WindowManager.class);
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {
        menuManager.setMenuTargetFbo(windowManager.getMainWindow(), fboManager.getFbo(RuntimeSetting.FBO_UI));
        this.editorMenu = menuManager.openMenu(EngineSetting.MENU_EDITOR_MAIN, windowManager.getMainWindow());
    }

    // Accessible \\

    public MenuInstance getEditorMenu() {
        return editorMenu;
    }
}