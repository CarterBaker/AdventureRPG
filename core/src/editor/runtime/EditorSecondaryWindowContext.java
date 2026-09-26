package editor.runtime;

import engine.root.ContextPackage;

public class EditorSecondaryWindowContext extends ContextPackage {

    /*
     * Context paired with every secondary editor OS window. Runs the same dock
     * as the main window with a base menu whose dock fills the whole window —
     * secondary windows carry no toolbar. Editor chrome must never pin the
     * cursor.
     */

    // Internal
    private EditorDockSystem editorDockSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorDockSystem = create(EditorDockSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
        editorDockSystem.openBaseMenu(EditorSetting.MENU_EDITOR_SECONDARY);
    }
}
