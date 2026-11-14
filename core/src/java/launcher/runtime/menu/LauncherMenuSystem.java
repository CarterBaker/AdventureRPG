package launcher.runtime.menu;

import engine.root.SystemPackage;
import launcher.runtime.menueventsmanager.menus.LauncherMenuBranch;

public class LauncherMenuSystem extends SystemPackage {

    /*
     * Opens the launcher's menus at startup, as MainMenuSystem opens the
     * game's main menu.
     */

    // Internal
    private LauncherMenuBranch launcherMenuBranch;

    // Internal \\

    @Override
    protected void get() {
        this.launcherMenuBranch = get(LauncherMenuBranch.class);
    }

    @Override
    protected void awake() {
        launcherMenuBranch.openMenu(context.getWindow());
    }
}
