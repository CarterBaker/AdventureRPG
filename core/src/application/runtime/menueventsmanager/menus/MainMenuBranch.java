package application.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.savepipeline.savemanager.SaveManager;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.world.WorldSystem;
import engine.root.BranchPackage;

public class MainMenuBranch extends BranchPackage {

    /*
     * Handles open and close actions for the main menu. Menus are opened per
     * window/context and close actions are parent-aware so multi-window
     * sessions can close only the clicked menu instance. Continue routes
     * through SaveManager to the most recently played character, then closes
     * the clicked menu so play begins; it does nothing until a character save
     * exists, so the menu stays open. New Game opens the character creator,
     * which CharacterCreatorBranch runs. returnToMainMenu() is the one way out
     * of play back to the menu: the character is saved and released and the
     * main menu opens over the world. Opening the menu points this window's
     * grid at the main menu's range and closing it at the play range, so the
     * world behind the menu streams only as far as its own settings allow;
     * the creator streams the play world its preview stands in.
     */

    // Internal
    private MenuManager menuManager;
    private SaveManager saveManager;
    private SettingsSystem settingsSystem;
    private WorldSystem worldSystem;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
        this.saveManager = get(SaveManager.class);
        this.settingsSystem = get(SettingsSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    // Accessible \\

    public MenuInstance openMenu(WindowInstance window) {
        streamMenuWorld();
        return menuManager.openMenu("MainMenu/Main", window);
    }

    public MenuInstance closeMenu(MenuInstance parent) {
        streamPlayWorld();
        return menuManager.closeMenu(parent);
    }

    // World \\

    public void streamMenuWorld() {
        worldSystem.useRange(settingsSystem.getMenuRange());
    }

    public void streamPlayWorld() {
        worldSystem.useRange(settingsSystem.getPlayRange());
    }

    // Game \\

    public void continueGame(MenuInstance menu, WindowInstance window) {
        if (saveManager.continueCharacter(window))
            closeMenu(menu);
    }

    public void returnToMainMenu(WindowInstance window) {
        saveManager.closeCharacter(window);
        openMenu(window);
    }
}
