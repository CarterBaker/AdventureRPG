package editor.runtime.editor;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class MenuTargetFboSystem extends SystemPackage {

    /*
     * Clones the UI render target for this window's GL context, registers it
     * with MenuManager so menus bound to this window composite correctly, and
     * pushes the FBO to the blit queue each frame so it reaches the screen.
     * Guarded by hasCompositeRect() — no blit fires before the compositor
     * assigns a real screen region to this window.
     */

    // Internal
    private FboManager fboManager;
    private MenuManager menuManager;
    private FboRenderSystem fboRenderSystem;

    // Render Target
    private FboInstance uiFbo;

    // Internal \\

    @Override
    protected void get() {
        this.fboManager = get(FboManager.class);
        this.menuManager = get(MenuManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
    }

    @Override
    protected void awake() {
        this.uiFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, context.getWindow());
        menuManager.setMenuTargetFbo(context.getWindow(), uiFbo);
    }

    // Render \\

    @Override
    protected void render() {
        if (!context.getWindow().hasCompositeRect())
            return;
        fboRenderSystem.pushFbo(uiFbo, RuntimeSetting.LAYER_UI, context.getWindow());
    }

    // Accessible \\

    public FboInstance getUiFbo() {
        return uiFbo;
    }
}