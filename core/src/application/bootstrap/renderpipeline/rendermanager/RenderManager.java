package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedAppearanceStruct;
import application.bootstrap.geometrypipeline.skinnedbuffermanager.SkinnedBufferManager;
import application.bootstrap.renderpipeline.cameramanager.CameraManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.render.MaskStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderManager extends ManagerPackage {

    /*
     * Drives the draw phase across all registered windows — logical windows
     * queue into their OS window's blit queue, OS windows flush their full
     * queue and swap buffers. Screen pass order 0 draws before FBO composite,
     * order 1 after.
     */

    private CameraManager cameraManager;
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private UBOManager uboManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private SkinnedBufferManager skinnedBufferManager;

    private RenderSystem renderSystem;

    @Override
    protected void create() {
        create(CompositeRenderSystem.class);
        this.renderSystem = create(RenderSystem.class);
        create(EntityRenderSystem.class);
    }

    @Override
    protected void get() {
        this.cameraManager = get(CameraManager.class);
        this.windowManager = get(WindowManager.class);
        this.playerManager = get(PlayerManager.class);
        this.uboManager = get(UBOManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.skinnedBufferManager = get(SkinnedBufferManager.class);
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

        // Pass 1 — logical windows queue into their OS window; no render work here.

        // Pass 2 — OS windows: correct camera, full queue, composite blits, swap.
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

    public void draw(FBOInstance target) {
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

    public void pushRenderCall(ModelInstance modelInstance, FBOInstance fbo, int depth) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, null, resolveDefaultWindow());
    }

    public void pushRenderCall(ModelInstance modelInstance, FBOInstance fbo, int depth, MaskStruct mask) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, mask, resolveDefaultWindow());
    }

    public void pushRenderCall(ModelInstance modelInstance, FBOInstance fbo, int depth, WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, null, window);
    }

    public void pushRenderCall(
            ModelInstance modelInstance,
            FBOInstance fbo,
            int depth,
            MaskStruct mask,
            WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, mask, window);
    }

    public void pushScreenCall(ModelInstance modelInstance) {
        renderSystem.pushScreenCall(modelInstance, null, resolveDefaultWindow(), EngineSetting.SCREEN_ORDER_BACKGROUND);
    }

    public void pushScreenCall(ModelInstance modelInstance, WindowInstance window) {
        renderSystem.pushScreenCall(modelInstance, null, window, EngineSetting.SCREEN_ORDER_BACKGROUND);
    }

    public void pushScreenCall(ModelInstance modelInstance, MaskStruct mask) {
        renderSystem.pushScreenCall(modelInstance, mask, resolveDefaultWindow(), EngineSetting.SCREEN_ORDER_BACKGROUND);
    }

    public void pushScreenCall(ModelInstance modelInstance, MaskStruct mask, WindowInstance window) {
        renderSystem.pushScreenCall(modelInstance, mask, window, EngineSetting.SCREEN_ORDER_BACKGROUND);
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

    public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, FBOInstance fbo) {
        renderSystem.pushCompositeCall(material, buffer, null, fbo, resolveDefaultWindow());
    }

    public void pushCompositeCall(
            MaterialInstance material,
            CompositeBufferInstance buffer,
            FBOInstance fbo,
            WindowInstance window) {
        renderSystem.pushCompositeCall(material, buffer, null, fbo, window);
    }

    public void pushCompositeCall(
            MaterialInstance material,
            CompositeBufferInstance buffer,
            MaskStruct mask,
            FBOInstance fbo,
            WindowInstance window) {
        renderSystem.pushCompositeCall(material, buffer, mask, fbo, window);
    }

    public void ensureFboRendered(FBOInstance fbo, WindowInstance window) {
        renderSystem.ensureTargetQueued(fbo, window);
    }

    // Skinned Calls \\

    public void clearSkinnedBuffers() {
        skinnedBufferManager.clearAll();
    }

    public void pushSkinnedCall(
            MeshHandle meshHandle,
            MaterialInstance material,
            Matrix4 modelMatrix,
            SkinnedAppearanceStruct appearance,
            Matrix4[] skinningMatrices,
            FBOInstance fbo) {
        renderSystem.pushSkinnedCall(
                meshHandle, material, modelMatrix, appearance, skinningMatrices, fbo, resolveDefaultWindow());
    }

    public void pushSkinnedCall(
            MeshHandle meshHandle,
            MaterialInstance material,
            Matrix4 modelMatrix,
            SkinnedAppearanceStruct appearance,
            Matrix4[] skinningMatrices,
            FBOInstance fbo,
            WindowInstance window) {
        renderSystem.pushSkinnedCall(meshHandle, material, modelMatrix, appearance, skinningMatrices, fbo, window);
    }

    // Window Resources \\

    public void resizeWindowResources(WindowInstance window) {
        fboManager.resizeWindowRelative(window, window.getWidth(), window.getHeight());
    }

    public void migrateWindowResources(WindowInstance window, WindowInstance previousGLWindow) {
        fboManager.migrateWindowFbos(window, previousGLWindow);
    }

    public void removeWindowResources(WindowInstance window) {
        renderSystem.removeWindowResources(window);
        fboRenderSystem.removeWindowResources(window);
        fboManager.releaseWindowFbos(window);
    }

    // Internal \\

    private WindowInstance resolveDefaultWindow() {

        WindowInstance renderWindow = windowManager.getRenderWindow();
        if (renderWindow != null)
            return renderWindow.getGLWindow();

        WindowInstance contextWindow = windowManager.getContextWindow();
        if (contextWindow != null)
            return contextWindow.getGLWindow();

        return windowManager.getMainWindow();
    }
}