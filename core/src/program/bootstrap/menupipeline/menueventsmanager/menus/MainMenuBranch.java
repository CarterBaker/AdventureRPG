package program.bootstrap.menupipeline.menueventsmanager.menus;

import program.bootstrap.menupipeline.menu.MenuInstance;
import program.bootstrap.menupipeline.menumanager.MenuManager;
import program.core.engine.BranchPackage;
import program.core.kernel.window.WindowInstance;

public class MainMenuBranch extends BranchPackage {

    /*
     * Handles open and close actions for the main menu. Menus are opened per
     * window/context and close actions are parent-aware so multi-window sessions
     * can close only the clicked menu instance.
     */

    // Internal
    private MenuManager menuManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
    }

    // Accessible \\

    public MenuInstance openMenu(WindowInstance window) {
        return menuManager.openMenu("MainMenu/Main", window);
    }

    public MenuInstance closeMenu(MenuInstance parent) {
        return menuManager.closeMenu(parent);
    }
}
