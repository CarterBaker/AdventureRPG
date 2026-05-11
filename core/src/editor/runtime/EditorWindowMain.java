package editor.runtime;

import engine.root.ContextPackage;

public class EditorWindowMain extends ContextPackage {

    /*
     * Main editor window context. Owns the persistent editor menu system for
     * the platform-hosted main window.
     */

    // Internal \\
    @Override
    protected void create() {
        create(EditorMenuSystem.class);
    }
}
