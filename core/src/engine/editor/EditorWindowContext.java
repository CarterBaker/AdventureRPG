package engine.editor;

import engine.root.ContextPackage;

public class EditorWindowContext extends ContextPackage {

    /*
     * Context paired with every editor OS window — the main window and every
     * secondary window alike. Each one runs the exact same EditorMenuSystem,
     * so every window gets the same background, dock canvas, and toolbar with
     * no per-window branching. Editor chrome must never pin the cursor.
     */

    // Internal \\

    @Override
    protected void create() {
        create(EditorMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
