package application.bootstrap.renderpipeline.entityrendersystem;

import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import engine.root.SystemPackage;

public class EntityRenderSystem extends SystemPackage {

    /*
     * Global, once-per-frame reset for the shared skinned instance buffers.
     * Runs during the RENDER phase's global pass — before any window/context
     * gets to push its own player into those buffers (contexts render after
     * global systems each frame, per EnginePackage.internalRender()) — so
     * every buffer starts this frame empty exactly once, no matter how many
     * windows are open.
     *
     * Actually pushing a character into those buffers is done per-window,
     * by PlayerRenderSystem living inside that window's own RuntimeContext —
     * it targets that window's own live world FBO (from WorldSystem), which
     * is the FBO instance actually queued for compositing to screen. This
     * system intentionally does NOT push draw calls itself anymore: the FBO
     * FboManager.getFbo(name) hands back is the global, uncloned instance —
     * never the per-window clone WorldSystem creates and the rest of the
     * pipeline actually reads from — so nothing pushed against it would ever
     * reach the screen.
     */

    private RenderManager renderManager;

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
    }

    @Override
    protected void render() {
        renderManager.clearSkinnedBuffers();
    }
}