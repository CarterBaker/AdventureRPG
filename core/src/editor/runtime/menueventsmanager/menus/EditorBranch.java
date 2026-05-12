package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.tabmanager.TabManager;
import editor.runtime.EditorWindowSecondary;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class EditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the main editor menu. Delegates tab and window
     * operations to TabManager.
     *
     * The hasTab guards that previously blocked opening a second tab of the
     * same type have been removed. TabManager now generates unique instance
     * titles via a per-class counter ("Preview 1", "Preview 2", etc.) so
     * there is no uniqueness collision to guard against here. Any limit on
     * how many of a given tab type can be open belongs in TabManager, not
     * in branch event handlers.
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
        tabManager.openTab(EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY, EditorWindowSecondary.class);
    }
}