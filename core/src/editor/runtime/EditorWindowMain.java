package editor.runtime;

import editor.bootstrap.docklayoutsystem.DockLayoutSystem;
import editor.runtime.menueventsmanager.EditorMenuEventsManager;
import engine.root.ContextPackage;

public class EditorWindowMain extends ContextPackage {

    /*
     * Main editor window context. Owns the persistent editor menu system,
     * tab compositor, and menu event handlers for the platform-hosted main window.
     */

    // Internal \\

    @Override
    protected void create() {
        create(EditorMenuSystem.class);
        create(EditorTabCompositorSystem.class);
        create(EditorMenuEventsManager.class);
    }
}