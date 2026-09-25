package application.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.savepipeline.savemanager.SaveManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.BranchPackage;

public class MainMenuBranch extends BranchPackage {

    /*
     * Handles open and close actions for the main menu. Menus are opened per
     * window/context and close actions are parent-aware so multi-window sessions
     * can close only the clicked menu instance. New Game and Continue route
     * through SaveManager — a fresh character or the most recently played one —
     * then close the clicked menu so play begins. Continue does nothing until a
     * character save exists, so the menu stays open.
     */

    // Internal
    private MenuManager menuManager;
    private SaveManager saveManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
        this.saveManager = get(SaveManager.class);
    }

    // Accessible \\

    public MenuInstance openMenu(WindowInstance window) {
        return menuManager.openMenu("MainMenu/Main", window);
    }

    public MenuInstance closeMenu(MenuInstance parent) {
        return menuManager.closeMenu(parent);
    }

    // Game \\

    public void newGame(MenuInstance menu, WindowInstance window) {
        saveManager.newCharacter(window);
        closeMenu(menu);
    }

    public void continueGame(MenuInstance menu, WindowInstance window) {
        if (saveManager.continueCharacter(window))
            closeMenu(menu);
    }
}