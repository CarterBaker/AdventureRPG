package editor.runtime.editor;

import editor.runtime.editor.menueventsmanager.EditorMenuEventsManager;
import engine.root.ContextPackage;

public class EditorWindowMain extends ContextPackage {

    /*
     * Main editor window context. Owns the persistent editor menu system,
     * tab compositor, and menu event handlers for the platform-hosted main window.
     *
     * The main window is focus-independent — the toolbar must remain hoverable
     * and clickable regardless of which tab currently owns focus. Capture is
     * ineligible because editor chrome must never pin the cursor.
     */

    // Internal \\

    @Override
    protected void create() {
        create(EditorMenuSystem.class);
        create(EditorTabCompositorSystem.class);
        create(EditorMenuEventsManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
        getWindow().setFocusIndependent(true);
    }
}