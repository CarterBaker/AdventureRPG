package editor.runtime;

import engine.root.ContextPackage;

public class EditorWindowMain extends ContextPackage {

    /*
     * Editor runtime entry point. Creates and owns all editor systems.
     * Paired with the main window by EditorEngine.
     *
     * PreviewSystem has been removed. Secondary editor windows are now
     * opened via the Testing dropdown in the editor toolbar (EditorBranch),
     * which replaces the old OPEN_PREVIEW keybind entirely.
     */

    // Editor
    private EditorMenuSystem editorMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorMenuSystem = create(EditorMenuSystem.class);
    }
}