package editor.runtime;

import application.bootstrap.menupipeline.menueventsmanager.menus.EditorBranch;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class EditorMenuSystem extends SystemPackage {

    /*
     * Opens the main editor menu against the main window when the editor
     * context awakens. Runs once — no per-frame update needed.
     *
     * NOTE: calls windowManager.getMainWindow() — verify the method name
     * matches your WindowManager API.
     */

    // Internal
    private EditorBranch editorBranch;
    private WindowManager windowManager;
    private MenuManager menuManager;
    private FboManager fboManager;

    // Internal \\

    @Override
    protected void get() {
        this.editorBranch = get(EditorBranch.class);
        this.windowManager = get(WindowManager.class);
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {
        menuManager.setMenuTargetFbo(fboManager.getFbo(RuntimeSetting.FBO_UI));
        editorBranch.openEditorMenu(windowManager.getMainWindow());
    }
}
