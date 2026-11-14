package application.runtime.menu;

import application.runtime.menueventsmanager.menus.MainMenuBranch;
import engine.root.SystemPackage;

public class MainMenuSystem extends SystemPackage {

    /*
     * Opens the main menu at runtime startup. Kept apart from MenuSystem so a
     * context can bind its UI render target without showing the main menu —
     * dev mode creates MenuSystem alone and drops straight into the world.
     */

    // Internal
    private MainMenuBranch mainMenuBranch;

    // Internal \\

    @Override
    protected void get() {
        this.mainMenuBranch = get(MainMenuBranch.class);
    }

    @Override
    protected void awake() {
        mainMenuBranch.openMenu(context.getWindow());
    }
}