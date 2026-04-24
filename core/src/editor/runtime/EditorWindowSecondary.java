package editor.runtime;

import engine.root.ContextPackage;

public class EditorWindowSecondary extends ContextPackage {

    /*
     * Minimal context bound to each secondary editor window. No runtime
     * systems are required here — EditorBranch opens the secondary editor
     * menu directly against the WindowInstance returned by
     * WindowManager.openWindow(), so the context itself stays empty.
     *
     * Add systems here if secondary windows ever need their own update loop
     * (e.g. a free cam, independent input handling, etc.).
     */

    @Override
    protected void create() {
        // Intentionally empty — secondary windows are menu-only for now
    }
}