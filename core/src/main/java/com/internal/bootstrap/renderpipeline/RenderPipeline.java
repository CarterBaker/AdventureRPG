package com.internal.bootstrap.renderpipeline;

import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;
import com.internal.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.core.engine.PipelinePackage;

public class RenderPipeline extends PipelinePackage {

    /*
     * Registers all render pipeline managers and systems in dependency order.
     * Exposes draw() as the single entry point for the engine's render loop.
     */

    // Internal
    private RenderSystem renderSystem;

    // Internal \\

    @Override
    protected void create() {
        create(CameraManager.class);
        create(CompositeRenderSystem.class);
        this.renderSystem = create(RenderSystem.class);
    }

    // Accessible \\

    public void draw() {
        renderSystem.draw();
    }
}