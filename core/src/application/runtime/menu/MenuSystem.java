package application.runtime.menu;

import application.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import engine.root.SystemPackage;
import engine.root.EngineSetting;

public class MenuSystem extends SystemPackage {

    /*
     * Opens the main menu at runtime startup. Passes the context window so
     * the menu is bound to the correct render target regardless of which
     * window this context was paired with.
     */

    // Internal
    private MainMenuBranch mainMenuBranch;
    private MenuManager menuManager;
    private FboManager fboManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.mainMenuBranch = get(MainMenuBranch.class);
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {
        menuManager.setMenuTargetFbo(fboManager.getFbo(EngineSetting.FBO_UI));
        mainMenuBranch.openMenu(context.getWindow());
    }
}
