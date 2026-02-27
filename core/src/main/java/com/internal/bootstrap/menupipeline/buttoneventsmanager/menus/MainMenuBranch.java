package com.internal.bootstrap.menupipeline.buttoneventsmanager.menus;

import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.core.engine.BranchPackage;

public class MainMenuBranch extends BranchPackage {

    private MenuManager menuManager;
    private MenuInstance mainMenu;

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    public MenuInstance openMenu() {

        if (mainMenu == null)
            mainMenu = menuManager.openMenu("MainMenu/Main");

        return mainMenu;
    }

    public MenuInstance closeMenu() {
        mainMenu = menuManager.closeMenu(mainMenu);
        return mainMenu;
    }
}
