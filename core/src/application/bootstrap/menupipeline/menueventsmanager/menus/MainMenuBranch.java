package application.bootstrap.menupipeline.menueventsmanager.menus;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.core.engine.BranchPackage;
import application.core.kernel.window.WindowInstance;

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
