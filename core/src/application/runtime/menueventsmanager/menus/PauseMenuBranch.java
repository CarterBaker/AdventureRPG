package application.runtime.menueventsmanager.menus;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.savepipeline.savemanager.SaveManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;
import engine.settings.KeyBindings;

public class PauseMenuBranch extends BranchPackage {

    /*
     * Runs the in-game pause menu for this context's window. Pause opens it
     * while a player is in the world and no other menu holds input; Pause or
     * Continue closes it and play resumes. Options opens the settings menu
     * over it, which returns to it on close. Quit ends the play session —
     * the character is saved and released through SaveManager — and brings
     * the main menu back up over the world, with the player and camera left
     * where they stood. Pause only closes the menu once it has been on show a
     * whole frame, so the key that backs out of Settings never also resumes
     * play, whichever branch reads it first.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private PlayerManager playerManager;
    private SaveManager saveManager;
    private MainMenuBranch mainMenuBranch;

    // State
    private MenuInstance pauseMenu;
    private boolean closeArmed;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.playerManager = get(PlayerManager.class);
        this.saveManager = get(SaveManager.class);
        this.mainMenuBranch = get(MainMenuBranch.class);
    }

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        boolean pausePressed = inputManager.bindingClicked(KeyBindings.PAUSE, window);

        if (pauseMenu == null) {

            if (pausePressed && canPause(window))
                openMenu(window);

            return;
        }

        if (pausePressed && closeArmed) {
            closeMenu();
            return;
        }

        this.closeArmed = pauseMenu.isVisible();
    }

    // Open / Close \\

    private boolean canPause(WindowInstance window) {
        return playerManager.hasPlayerForWindow(window.getWindowID())
                && !window.getMenuListHandle().isInputLocked();
    }

    private void openMenu(WindowInstance window) {
        this.pauseMenu = menuManager.openMenu(RuntimeSetting.MENU_PAUSE, window);
        this.closeArmed = false;
    }

    public void closeMenu() {
        this.pauseMenu = menuManager.closeMenu(pauseMenu);
    }

    // Quit \\

    public void quitToMainMenu(WindowInstance window) {
        closeMenu();
        saveManager.closeCharacter(window);
        mainMenuBranch.openMenu(window);
    }
}
