package com.internal.bootstrap.renderpipeline;

import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;
import com.internal.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.core.engine.PipelinePackage;

public class RenderPipeline extends PipelinePackage {

    private CameraManager cameraManager;
    private RenderSystem renderSystem;
    private CompositeRenderSystem compositeRenderSystem;

    @Override
    protected void create() {
        this.cameraManager = create(CameraManager.class);
        this.renderSystem = create(RenderSystem.class);
        this.compositeRenderSystem = create(CompositeRenderSystem.class);
    }

    // TODO: Not happy with this here
    public void draw() {
        renderSystem.draw();
        compositeRenderSystem.draw();
    }
}