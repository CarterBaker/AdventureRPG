package editor.runtime.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.BranchPackage;

public class EditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the main editor menu. Delegates tab and window
     * operations to TabManager.
     *
     * openPreview() opens a content tab. openSecondaryWindow() opens a bare OS
     * window with no tab — the user can drag tabs into it. Both operations are
     * single-method calls into TabManager; no policy lives here.
     */

    private MenuManager menuManager;
    private TabManager tabManager;

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.tabManager = get(TabManager.class);
    }

    public void toggleTestingDropdown(MenuInstance parent) {

        ElementInstance dropdown = parent.getEntryPoint(0);

        if (dropdown == null)
            return;

        dropdown.toggleExpanded();
    }

    public void openPreview() {
        tabManager.openPreview();
    }

    public void openSecondaryWindow() {
        tabManager.openSecondaryWindow();
    }
}