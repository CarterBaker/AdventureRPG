package editor.runtime.editor;

import editor.runtime.editor.menueventsmanager.EditorMenuEventsManager;
import engine.root.ContextPackage;

public class EditorWindowMain extends ContextPackage {

    /*
     * Main editor window context. Owns the persistent editor menu system,
     * tab compositor, and menu event handlers for the platform-hosted main window.
     *
     * alwaysHover is set here so the main editor chrome responds to hover
     * without requiring a prior click — the window is never a game content
     * window and never needs click-to-capture.
     */

    // Internal \\

    @Override
    protected void create() {
        getWindow().setAlwaysHover(true);
        create(EditorMenuSystem.class);
        create(EditorTabCompositorSystem.class);
        create(EditorMenuEventsManager.class);
    }
}