package application.runtime.menu;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class MenuSystem extends SystemPackage {

    /*
     * Binds the UI render target at runtime startup so menus composite into
     * the correct FBO regardless of window context. Opening the main menu is
     * MainMenuSystem's job, so contexts without one still render menus.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;

    // Render Target
    private FboInstance uiFbo;

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {
        this.uiFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, context.getWindow());
        menuManager.setMenuTargetFbo(context.getWindow(), uiFbo);
    }

    // Accessible \\

    public FboInstance getUiFbo() {
        return uiFbo;
    }
}
