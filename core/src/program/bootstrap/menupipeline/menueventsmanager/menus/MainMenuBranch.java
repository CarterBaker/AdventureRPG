package program.bootstrap.menupipeline.menueventsmanager.menus;

import program.bootstrap.menupipeline.menu.MenuInstance;
import program.bootstrap.menupipeline.menumanager.MenuManager;
import program.bootstrap.renderpipeline.window.WindowInstance;
import program.core.engine.BranchPackage;

public class MainMenuBranch extends BranchPackage {

    /*
     * Handles open and close actions for the main menu. Holds the active
     * MenuInstance so the same instance is reused across open/close cycles.
     * WindowInstance is passed by the caller so the menu is bound to the
     * correct window.
     */

    // Internal
    private MenuManager menuManager;

    // State
    private MenuInstance mainMenu;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
    }

    // Accessible \\

    public MenuInstance openMenu(WindowInstance window) {

        if (mainMenu == null)
            mainMenu = menuManager.openMenu("MainMenu/Main", window);

        return mainMenu;
    }

    public MenuInstance closeMenu() {
        mainMenu = menuManager.closeMenu(mainMenu);
        return mainMenu;
    }
}