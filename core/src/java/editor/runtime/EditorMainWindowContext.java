package editor.runtime;

import editor.runtime.dock.EditorDockSystem;
import editor.runtime.frame.EditorFrameSystem;
import editor.runtime.hintbar.EditorHintBarSystem;
import editor.runtime.toolbar.EditorToolbarSystem;
import engine.root.ContextPackage;

public class EditorMainWindowContext extends ContextPackage {

    /*
     * Context paired with the main editor window. Runs the same dock, toolbar
     * and frame as every secondary window, with a base menu that leaves room
     * for the toolbar and the hint bar, the toolbar that carries the editor's
     * menus and the hint bar that reads out the active window's keys, which
     * only the main window shows. Editor chrome must never pin the cursor.
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
        create(EditorHintBarSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
        editorDockSystem.openBaseMenu(EditorSetting.MENU_EDITOR_BASE);
        editorToolbarSystem.openToolbar(EditorSetting.MENU_EDITOR_TOOLBAR);
    }
}
