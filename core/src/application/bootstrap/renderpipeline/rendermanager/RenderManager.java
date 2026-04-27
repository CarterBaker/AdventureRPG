package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.cameramanager.CameraManager;
import application.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboManager;
import application.bootstrap.renderpipeline.fborendermanager.FboRenderManager;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderManager extends ManagerPackage {

    private CameraManager cameraManager;
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private UBOManager uboManager;
    private FboManager fboManager;
    private FboRenderManager fboRenderManager;

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
        this.fboRenderManager = get(FboRenderManager.class);
    }

    public void draw() {
        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();
        Object[] elements = windows.elements();
        int count = windows.size();

        if (count == 0) {
            windowManager.endRenderWindow();
            internal.windowPlatform.restoreMainContext();
            return;
        }

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

        fboManager.resizeWindowRelative(window.getWidth(), window.getHeight());
        renderSystem.drawToMappedTargets(window);

        fboRenderManager.pushBlits();
        drawFinal(window);
    }

    public void draw(FboInstance target) {
        WindowInstance window = resolveDefaultWindow();
        if (window == null)
            return;

        uboManager.bindBuffersForCurrentContext();
        playerManager.pushPlayerPositionForWindow(window.getWindowID());
        cameraManager.pushCamera(window);
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

    public void pushRenderCall(ModelInstance modelInstance, FboInstance fbo, int depth) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, null, resolveDefaultWindow());
    }

    public void pushRenderCall(ModelInstance modelInstance, FboInstance fbo, int depth, MaskStruct mask) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, mask, resolveDefaultWindow());
    }

    public void pushRenderCall(ModelInstance modelInstance, FboInstance fbo, int depth, WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, null, window);
    }

    public void pushRenderCall(ModelInstance modelInstance, FboInstance fbo, int depth, MaskStruct mask,
            WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, fbo, depth, mask, window);
    }

    public void pushScreenCall(ModelInstance modelInstance) {
        renderSystem.pushScreenCall(modelInstance, null, resolveDefaultWindow());
    }

    public void pushScreenCall(ModelInstance modelInstance, MaskStruct mask) {
        renderSystem.pushScreenCall(modelInstance, mask, resolveDefaultWindow());
    }

    public void pushScreenCall(ModelInstance modelInstance, WindowInstance window) {
        renderSystem.pushScreenCall(modelInstance, null, window);
    }

    public void pushScreenCall(ModelInstance modelInstance, MaskStruct mask, WindowInstance window) {
        renderSystem.pushScreenCall(modelInstance, mask, window);
    }

    private WindowInstance resolveDefaultWindow() {
        WindowInstance renderWindow = windowManager.getRenderWindow();
        if (renderWindow != null)
            return renderWindow;

        WindowInstance contextWindow = windowManager.getContextWindow();
        if (contextWindow != null)
            return contextWindow;

        WindowInstance activeWindow = windowManager.getActiveWindow();
        if (activeWindow != null)
            return activeWindow;

        return windowManager.getMainWindow();
    }

    public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer) {
        renderSystem.pushCompositeCall(material, buffer, resolveDefaultWindow());
    }

    public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window) {
        renderSystem.pushCompositeCall(material, buffer, window);
    }

    public void removeWindowResources(WindowInstance window) {
        renderSystem.removeWindowResources(window);
    }
}
