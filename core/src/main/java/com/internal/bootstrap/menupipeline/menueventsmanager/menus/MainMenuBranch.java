package com.internal.bootstrap.menupipeline.menueventsmanager.menus;

import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.core.engine.BranchPackage;

public class MainMenuBranch extends BranchPackage {

    /*
     * Handles open and close actions for the main menu. Holds the active
     * MenuInstance so the same instance is reused across open/close cycles.
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

    public MenuInstance openMenu() {

        if (mainMenu == null)
            mainMenu = menuManager.openMenu("MainMenu/Main");

        return mainMenu;
    }

    public MenuInstance closeMenu() {
        mainMenu = menuManager.closeMenu(mainMenu);
        debug();
        return mainMenu;
    }
}