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
     * Coordinates a single draw pass. Delegates camera UBO upload to
     * CameraManager then flushes all queued render calls via RenderSystem.
     * Exposes pushRenderCall() and pushCompositeCall() as the public surface
     * for all systems that submit geometry — RenderSystem is package-private.
     * Depth parameter controls pass ordering — lower depth renders first.
     * draw(WindowInstance) overload sets the active window, used for detached
     * windows.
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

        WindowInstance window = windowManager.getActiveWindow();

        cameraManager.pushCamera(window);
        renderSystem.draw();
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