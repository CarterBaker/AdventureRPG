package application.bootstrap.menupipeline.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeContext;
import editor.runtime.EditorWindowSecondary;
import engine.root.BranchPackage;

public class EditorBranch extends BranchPackage {

    /*
     * Handles editor menu lifecycle and toolbar button actions.
     *
     * openEditorMenu() — called by EditorMenuSystem on awake, opens the
     * main editor menu against the main window.
     *
     * toggleTestingDropdown() — $parent click handler on the Testing toolbar
     * button. Reads entry point 0 (testing_dropdown) from the live MenuInstance
     * and flips its visibility.
     *
     * openSecondaryWindow() — Runnable click handler on the Open Window button.
     * Spawns a new OS window bound to EditorWindowSecondary.
     *
     * openPreview() — Runnable click handler on the Open Preview button.
     * Spawns a new OS window bound to RuntimeContext, running the full
     * game runtime in a separate window.
     */

    // Internal
    private MenuManager menuManager;
    private WindowManager windowManager;

    // State
    private MenuInstance editorMenu;

    // Internal \\

    @Override
    protected void get() {

        this.menuManager = get(MenuManager.class);
        this.windowManager = get(WindowManager.class);
    }

    // Open / Close \\

    public void openEditorMenu(WindowInstance window) {

        if (editorMenu != null)
            return;

        editorMenu = menuManager.openMenu("EditorWindow/Main", window);
    }

    public void closeEditorMenu() {

        if (editorMenu == null)
            return;

        editorMenu = menuManager.closeMenu(editorMenu);
    }

    // Toolbar Actions \\

    public void toggleTestingDropdown(MenuInstance parent) {

        ElementInstance dropdown = parent.getEntryPoint(0);

        if (dropdown == null)
            return;

        dropdown.toggleExpanded();
    }

    public void openSecondaryWindow() {

        WindowInstance window = windowManager.openWindow(
                "Secondary Editor",
                EditorWindowSecondary.class);

        menuManager.openMenu("EditorWindow/Secondary", window);
    }

    public void openPreview() {
        windowManager.openWindow("Preview", RuntimeContext.class);
    }
}