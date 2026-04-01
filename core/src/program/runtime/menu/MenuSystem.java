package program.runtime.menu;

import program.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch;
import program.core.engine.SystemPackage;

public class MenuSystem extends SystemPackage {

    /*
     * Opens the main menu at runtime startup. Passes the context window so
     * the menu is bound to the correct render target regardless of which
     * window this context was paired with.
     */

    // Internal
    private MainMenuBranch mainMenuBranch;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.mainMenuBranch = get(MainMenuBranch.class);
    }

    @Override
    protected void awake() {
        mainMenuBranch.openMenu(context.getWindow());
    }
}