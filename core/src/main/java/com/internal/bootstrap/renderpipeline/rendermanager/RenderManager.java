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

public class RenderManager extends ManagerPackage {

    /*
     * Coordinates draw passes and routes render call pushes to the correct
     * window's RenderQueueHandle. Push calls with no window parameter default
     * to the main window — engine-level systems that do not declare a target
     * always render to main. Push calls with an explicit window route there.
     * Active window is never touched here — that is an input concern only.
     * draw(WindowInstance) flushes a specific window's queue under its GL
     * context — called by the engine draw() loop for main, and by each
     * detached WindowInstance.render() callback for itself.
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