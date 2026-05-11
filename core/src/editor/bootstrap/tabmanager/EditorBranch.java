package editor.bootstrap.tabmanager;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class EditorBranch extends BranchPackage {

    /*
     * Event routing branch for editor menu actions. JSON on_click reflection
     * calls only the public methods in this class; lifecycle and registry work
     * remain owned by TabManager.
     */

    // Internal
    private TabManager tabManager;
    private MenuManager menuManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.tabManager = get(TabManager.class);
        this.menuManager = get(MenuManager.class);
    }

    // Management \\

    void openSecondaryMenu(WindowInstance window) {
        menuManager.openMenu(EngineSetting.MENU_EDITOR_SECONDARY, window);
    }

    // Accessible \\

    public void toggleTestingDropdown(MenuInstance parent) {

        if (parent == null)
            throwException("Cannot toggle testing dropdown without a parent menu.");

        ElementInstance dropdown = parent.getEntryPoint(EngineSetting.EDITOR_TESTING_DROPDOWN_ENTRY_INDEX);

        if (dropdown == null)
            throwException("Testing dropdown entry point was not found.");

        dropdown.toggleExpanded();
    }

    public void openSecondaryWindow() {
        tabManager.openSecondaryWindow();
    }

    public void openPreview() {
        tabManager.openPreview();
    }
}
