package editor.runtime;

import editor.runtime.dock.EditorDockSystem;
import editor.runtime.frame.EditorFrameSystem;
import editor.runtime.toolbar.EditorToolbarSystem;
import engine.root.ContextPackage;

public class EditorSecondaryWindowContext extends ContextPackage {

    /*
     * Context paired with every secondary editor OS window. Runs the same
     * dock, toolbar and frame as the main window, with a base menu that leaves
     * room for the toolbar and a toolbar that carries only the window's title
     * and frame controls. Editor chrome must never pin the cursor.
     */

    // Internal
    private EditorDockSystem editorDockSystem;
    private EditorToolbarSystem editorToolbarSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorDockSystem = create(EditorDockSystem.class);
        this.editorToolbarSystem = create(EditorToolbarSystem.class);
        create(EditorFrameSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
        editorDockSystem.openBaseMenu(EditorSetting.MENU_EDITOR_SECONDARY);
        editorToolbarSystem.openToolbar(EditorSetting.MENU_EDITOR_TITLE_BAR);
    }
}
