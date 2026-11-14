package launcher.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.menueventsmanager.menus.settings.SettingsMenuBranch;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import launcher.bootstrap.launcherpipeline.launchermanager.LauncherManager;
import launcher.runtime.LauncherSetting;

public class LauncherMenuBranch extends BranchPackage {

    /*
     * Runs the launcher's one big board: the game's animated title standing
     * over its top edge, its choices beneath, and its version. Launch saves
     * the settings and closes the launcher so the game starts in the same
     * JVM; Quit closes it alone; Options opens the game's own settings menu
     * over it through SettingsMenuBranch, which shows the board again on
     * Back. Answers whether a point lies on the shown title, for
     * LauncherWindowSystem.
     */

    // Internal
    private MenuManager menuManager;
    private SettingsSystem settingsSystem;
    private LauncherManager launcherManager;
    private SettingsMenuBranch settingsMenuBranch;

    // Menus
    private MenuInstance mainMenu;
    private ElementInstance titleElement;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.settingsSystem = get(SettingsSystem.class);
        this.launcherManager = get(LauncherManager.class);
        this.settingsMenuBranch = get(SettingsMenuBranch.class);
    }

    // Open \\

    public void openMenu(WindowInstance window) {

        this.mainMenu = menuManager.openMenu(LauncherSetting.MENU_MAIN, window);
        this.titleElement = mainMenu.getElements().get(LauncherSetting.ELEMENT_INDEX_TITLE);

        ElementInstance boardElement = mainMenu.getElements().get(LauncherSetting.ELEMENT_INDEX_BOARD);
        boardElement.findChildById(LauncherSetting.ELEMENT_VERSION).setFontText(
                String.format(LauncherSetting.VERSION_FORMAT, EngineSetting.VERSION));
    }

    // Actions \\

    public void launch() {
        settingsSystem.saveSettings();
        launcherManager.launch();
    }

    public void openOptions(MenuInstance menu, WindowInstance window) {
        settingsMenuBranch.openMenu(menu, window);
    }

    public void quit() {
        launcherManager.quit();
    }

    // Accessible \\

    public boolean isOverTitle(float x, float y) {
        return mainMenu.isVisible() && titleElement.containsPoint(x, y);
    }
}
