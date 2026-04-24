package editor.runtime;

import application.bootstrap.menupipeline.menueventsmanager.menus.EditorBranch;
import application.kernel.windowpipeline.windowmanager.WindowManager;
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

    // Internal \\

    @Override
    protected void get() {
        this.editorBranch = get(EditorBranch.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {
        editorBranch.openEditorMenu(windowManager.getMainWindow());
    }
}