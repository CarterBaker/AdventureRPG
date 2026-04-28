package application.runtime.menu;

import application.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class MenuSystem extends SystemPackage {

    /*
     * Opens the main menu at runtime startup and binds the UI render target
     * so menus composite into the correct FBO regardless of window context.
     */

    // Internal
    private MainMenuBranch mainMenuBranch;
    private MenuManager menuManager;
    private FboManager fboManager;

    // Render Target
    private FboInstance uiFbo;

    @Override
    protected void get() {

        // Internal
        this.mainMenuBranch = get(MainMenuBranch.class);
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {
        this.uiFbo = fboManager.getFbo(EngineSetting.FBO_UI);
        menuManager.setMenuTargetFbo(uiFbo);
        mainMenuBranch.openMenu(context.getWindow());
    }
}