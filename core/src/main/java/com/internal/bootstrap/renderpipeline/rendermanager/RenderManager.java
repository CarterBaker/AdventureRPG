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
     * Coordinates a single draw pass per window. Exposes createRenderQueue()
     * so ContextPackage can allocate its own RenderQueueHandle on awake.
     * pushRenderCall() and pushCompositeCall() are the public surface for all
     * systems that submit geometry — they always write into the active window's
     * context queue. draw() resolves the active window and flushes its queue.
     * draw(WindowInstance) sets the target explicitly — used by detached windows
     * from their own ApplicationListener.render() callback.
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

    // Queue \\

    public RenderQueueHandle createRenderQueue() {
        RenderQueueHandle handle = create(RenderQueueHandle.class);
        handle.constructor();
        return handle;
    }

    // Draw \\

    public void draw() {
        WindowInstance window = windowManager.getActiveWindow();
        cameraManager.pushCamera(window);
        renderSystem.draw(window);
    }

    public void draw(WindowInstance window) {
        windowManager.setActiveWindow(window);
        draw();
    }

    // Push \\

    public void pushRenderCall(ModelInstance modelInstance, int depth) {
        renderSystem.pushRenderCall(modelInstance, depth);
    }

    public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask) {
        renderSystem.pushRenderCall(modelInstance, depth, mask);
    }

    public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer) {
        renderSystem.pushCompositeCall(material, buffer);
    }
}