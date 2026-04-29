package editor.runtime;

import engine.root.ContextPackage;

public class EditorWindowSecondary extends ContextPackage {

    /*
     * Minimal context bound to each secondary editor window. Clones the UI
     * render target for this window's GL context via MenuTargetFboSystem so
     * menus opened against this window composite correctly.
     */

    @Override
    protected void create() {
        create(MenuTargetFboSystem.class);
    }
}
