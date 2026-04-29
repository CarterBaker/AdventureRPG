package editor.runtime;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class MenuTargetFboSystem extends SystemPackage {

    /*
     * Clones the UI render target for this window's GL context and registers
     * it with MenuManager so menus bound to this window composite correctly.
     */

    // Internal
    private FboManager fboManager;
    private MenuManager menuManager;

    // Render Target
    private FboInstance uiFbo;

    @Override
    protected void get() {

        // Internal
        this.fboManager = get(FboManager.class);
        this.menuManager = get(MenuManager.class);
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
