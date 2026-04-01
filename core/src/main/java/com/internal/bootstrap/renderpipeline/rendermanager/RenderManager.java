package com.internal.bootstrap.renderpipeline.rendermanager;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;
import com.internal.bootstrap.renderpipeline.util.MaskStruct;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.core.engine.ManagerPackage;
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
    }

    // Draw \\

    public void draw() {

        WindowInstance mainWindow = windowManager.getMainWindow();

        if (mainWindow == null)
            return;

        // Main window — context current via platform callback.
        draw(mainWindow);

        // Detached windows — explicit context/swap ownership via platform.
        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();
        Object[] elements = windows.elements();
        int count = windows.size();

        for (int i = 0; i < count; i++) {
            WindowInstance window = (WindowInstance) elements[i];

            if (window == mainWindow)
                continue;

            if (!window.hasNativeHandle())
                continue;

            internal.windowPlatform.makeContextCurrent(window);
            internal.windowPlatform.syncWindowSize(window);
            draw(window);
            internal.windowPlatform.swapBuffers(window);
        }

        // Restore main for platform post-render assumptions.
        internal.windowPlatform.restoreMainContext();
    }

    public void draw(WindowInstance window) {
        cameraManager.pushCamera(window);
        renderSystem.draw(window);
    }

    // Push — no declared window → main window \\

    public void pushRenderCall(ModelInstance modelInstance, int depth) {
        renderSystem.pushRenderCall(modelInstance, depth, null, windowManager.getMainWindow());
    }

    public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask) {
        renderSystem.pushRenderCall(modelInstance, depth, mask, windowManager.getMainWindow());
    }

    // Push — explicit window \\

    public void pushRenderCall(ModelInstance modelInstance, int depth, WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, depth, null, window);
    }

    public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask, WindowInstance window) {
        renderSystem.pushRenderCall(modelInstance, depth, mask, window);
    }

    // Composite \\

    public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer) {
        renderSystem.pushCompositeCall(material, buffer, windowManager.getMainWindow());
    }

    public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window) {
        renderSystem.pushCompositeCall(material, buffer, window);
    }

    public void removeWindowResources(WindowInstance window) {
        renderSystem.removeWindowResources(window);
    }
}