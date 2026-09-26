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
     * Runs the pause menu for this context's window. Opens on Pause while
     * playing, closes on Pause or Continue, opens Settings over itself, and on
     * Quit saves and releases the character and returns to the main menu. Pause
     * closes it only after it has shown a full frame.
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
