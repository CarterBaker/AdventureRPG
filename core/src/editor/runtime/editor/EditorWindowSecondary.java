package editor.runtime.editor;

import engine.root.ContextPackage;

public class EditorWindowSecondary extends ContextPackage {

    /*
     * Secondary OS window context. Mirrors EditorWindowMain minus the toolbar —
     * opens the Base menu and drives dock rect and content rect sync via the
     * same systems the main window uses, just scoped to this window.
     *
     * All tab drag code paths are fully shared — TabDragManager and
     * DockLayoutSystem are OS-window-agnostic. Tabs land here via
     * TabManager.openSecondaryWindowForTab() which registers the BSP tree
     * before this context's awake() runs.
     *
     * capture and focusIndependent mirror the main window — editor chrome must
     * never pin the cursor and must remain hoverable regardless of focus state.
     */

    // Internal \\

    @Override
    protected void create() {
        create(SecondaryMenuSystem.class);
        create(SecondaryTabCompositorSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}