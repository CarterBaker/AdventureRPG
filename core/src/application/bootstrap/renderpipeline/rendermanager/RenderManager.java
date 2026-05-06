package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.cameramanager.CameraManager;
import application.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.ContextPackage;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderManager extends ManagerPackage {

    /*
     * Drives the draw phase. Two-pass loop over all windows registered in
     * WindowManager:
     *
     * Pass 1 — logical windows (tabs, no native handle): make their composite
     * target's GL context current, push their own camera to the UBO, render
     * their content, push FBOs onto the OS window's blit queue. No buffer swap.
     *
     * Pass 2 — OS windows (native handle): make context current, sync size,
     * push camera, render content, flush blit queue (which now includes any
     * logical window FBOs from pass 1), swap buffers.
     *
     * Both passes call the same draw(window) — no special-casing per window type.
     *
     * Screen pass is split around the compositor:
     * order 0 — drawn before FBO composite (game UI, menus)
     * order 1 — drawn after FBO composite (dock chrome, always on top)
     */

    private CameraManager cameraManager;
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private UBOManager uboManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;

    private RenderSystem renderSystem;

    @Override
    protected void create() {
        create(CompositeRenderSystem.class);
        this.renderSystem = create(RenderSystem.class);
    }

    @Override
    protected void get() {
        this.cameraManager = get(CameraManager.class);
        this.windowManager = get(WindowManager.class);
        this.playerManager = get(PlayerManager.class);
        this.uboManager = get(UBOManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
    }

    // Draw \\

    public void draw() {

        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();
        Object[] elements = windows.elements();
        int count = windows.size();

        if (count == 0) {
            windowManager.endRenderWindow();
            internal.windowPlatform.restoreMainContext();
            return;
        }

        // Pass 1 — logical windows: render with own camera, queue FBOs onto OS window
        // blit queues
        for (int i = 0; i < count; i++) {
            WindowInstance window = (WindowInstance) elements[i];
            if (window.hasNativeHandle())
                continue;
            internal.windowPlatform.makeContextCurrent(window.getGLWindow());
            windowManager.beginRenderWindow(window);
            draw(window);
        }

        // Pass 2 — OS windows: pushBlits picks up logical window FBOs, then swap
        // buffers
        for (int i = 0; i < count; i++) {
            WindowInstance window = (WindowInstance) elements[i];
            if (!window.hasNativeHandle())
                continue;
            internal.windowPlatform.makeContextCurrent(window);
            internal.windowPlatform.syncWindowSize(window);
            windowManager.beginRenderWindow(window);
            draw(window);
            internal.windowPlatform.swapBuffers(window);
        }

        windowManager.endRenderWindow();
        internal.windowPlatform.restoreMainContext();
    }

    public void draw(WindowInstance window) {
        uboManager.bindBuffersForCurrentContext();
        playerManager.pushPlayerPositionForWindow(window.getWindowID());
        cameraManager.pushCamera(window);
        fboManager.resizeWindowRelative(window, window.getWidth(), window.getHeight());
        renderSystem.drawToMappedTargets(window);
        fboRenderSystem.pushBlits(window);
        drawFinal(window);
    }

    public void draw(FboInstance target) {
        WindowInstance window = resolveDefaultWindow();
        if (window == null)
            return;

        uboManager.bindBuffersForCurrentContext();
        playerManager.pushPlayerPositionForWindow(window.getWindowID());
        cameraManager.pushCamera(window);
        fboManager.resizeWindowRelative(window, window.getWidth(), window.getHeight());
        renderSystem.drawToMappedTargets(window);
        fboRenderSystem.pushBlits(window);
        renderSystem.drawToTarget(window, target);
    }

    public void drawFinal(WindowInstance window) {
        renderSystem.drawToTarget(window, null);
    }

    public void drawFinal() {
        WindowInstance window = resolveDefaultWindow();
        if (window != null)
            drawFinal(window);
    }

    // Render Calls \\

    public void pushRenderCall(ModelInstance modelInstance, FboInstance fbo, int depth) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, null, resolveDefaultWindow());
    }

    public void pushRenderCall(ModelInstance modelInstance, FboInstance fbo, int depth, MaskStruct mask) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, mask, resolveDefaultWindow());
    }

    public void pushRenderCall(ModelInstance modelInstance, FboInstance fbo, int depth, WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, null, window);
    }

    public void pushRenderCall(
            ModelInstance modelInstance,
            FboInstance fbo,
            int depth,
            MaskStruct mask,
            WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, mask, window);
    }

    public void pushScreenCall(ModelInstance modelInstance) {
        renderSystem.pushScreenCall(modelInstance, null, resolveDefaultWindow(), 0);
    }

    public void pushScreenCall(ModelInstance modelInstance, WindowInstance window) {
        renderSystem.pushScreenCall(modelInstance, null, window, 0);
    }

    public void pushScreenCall(ModelInstance modelInstance, MaskStruct mask) {
        renderSystem.pushScreenCall(modelInstance, mask, resolveDefaultWindow(), 0);
    }

    public void pushScreenCall(ModelInstance modelInstance, MaskStruct mask, WindowInstance window) {
        renderSystem.pushScreenCall(modelInstance, mask, window, 0);
    }

    public void pushScreenCall(ModelInstance modelInstance, int order) {
        renderSystem.pushScreenCall(modelInstance, null, resolveDefaultWindow(), order);
    }

    public void pushScreenCall(ModelInstance modelInstance, WindowInstance window, int order) {
        renderSystem.pushScreenCall(modelInstance, null, window, order);
    }

    public void pushScreenCall(ModelInstance modelInstance, MaskStruct mask, int order) {
        renderSystem.pushScreenCall(modelInstance, mask, resolveDefaultWindow(), order);
    }

    public void pushScreenCall(ModelInstance modelInstance, MaskStruct mask, WindowInstance window, int order) {
        renderSystem.pushScreenCall(modelInstance, mask, window, order);
    }

    public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, FboInstance fbo) {
        renderSystem.pushCompositeCall(material, buffer, fbo, resolveDefaultWindow());
    }

    public void pushCompositeCall(
            MaterialInstance material,
            CompositeBufferInstance buffer,
            FboInstance fbo,
            WindowInstance window) {
        renderSystem.pushCompositeCall(material, buffer, fbo, window);
    }

    public void removeWindowResources(WindowInstance window) {
        renderSystem.removeWindowResources(window);
        fboRenderSystem.removeWindowResources(window);
    }

    // Internal \\

    private WindowInstance resolveDefaultWindow() {

        WindowInstance renderWindow = windowManager.getRenderWindow();
        if (renderWindow != null)
            return renderWindow.getGLWindow();

        WindowInstance contextWindow = windowManager.getContextWindow();
        if (contextWindow != null)
            return contextWindow.getGLWindow();

        WindowInstance activeWindow = windowManager.getActiveWindow();
        if (activeWindow != null)
            return activeWindow;

        return windowManager.getMainWindow();
    }
}