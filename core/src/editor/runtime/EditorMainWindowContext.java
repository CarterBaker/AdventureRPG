package editor.runtime;

import editor.runtime.EditorDockSystem;
import engine.root.ContextPackage;

public class EditorMainWindowContext extends ContextPackage {

    /*
     * Context paired with the main editor window. Runs the same dock as every
     * secondary window, with a base menu that leaves room for the toolbar, plus
     * the toolbar itself — which only the main window carries. Editor chrome
     * must never pin the cursor.
     */

    // Internal
    private EditorDockSystem editorDockSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorDockSystem = create(EditorDockSystem.class);
        create(EditorToolbarSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
        editorDockSystem.openBaseMenu(EditorSetting.MENU_EDITOR_BASE);
    }
}
