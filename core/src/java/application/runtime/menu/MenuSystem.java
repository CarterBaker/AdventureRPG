package application.runtime.menu;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
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
    private FBOManager fboManager;

    // Render Target
    private FBOInstance uiFbo;

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
    }

    @Override
    protected void awake() {
        this.uiFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, context.getWindow());
        menuManager.setMenuTargetFbo(context.getWindow(), uiFbo);
    }

    // Accessible \\

    public FBOInstance getUiFbo() {
        return uiFbo;
    }
}
