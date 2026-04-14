package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.cameramanager.CameraManager;
import application.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderManager extends ManagerPackage {

    /*
     * Coordinates draw passes and routes render call pushes to the correct
     * window queues. Engine calls draw() once per frame; RenderManager owns
     * draw ordering for main + detached windows.
     */

    // Internal
    private CameraManager cameraManager;
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private UBOManager uboManager;

    // Systems
    private RenderSystem renderSystem;

    // Internal \\

    @Override
    protected void create() {
        create(CompositeRenderSystem.class);
        this.renderSystem = create(RenderSystem.class);
    }

    @Override
    protected void get() {

        // Internal
        this.cameraManager = get(CameraManager.class);
        this.windowManager = get(WindowManager.class);
        this.playerManager = get(PlayerManager.class);
        this.uboManager = get(UBOManager.class);
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
        renderSystem.draw(window);
    }

    // Push — no declared window → render window, context window, active, then main
    // \\

    public void pushRenderCall(ModelInstance modelInstance, int depth) {
        renderSystem.pushRenderCall(modelInstance, depth, null, resolveDefaultWindow());
    }

    public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask) {
        renderSystem.pushRenderCall(modelInstance, depth, mask, resolveDefaultWindow());
    }

    // Push — explicit window \\

    public void pushRenderCall(ModelInstance modelInstance, int depth, WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, depth, null, window);
    }

    public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask, WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, depth, mask, window);
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
    // Composite \\

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
