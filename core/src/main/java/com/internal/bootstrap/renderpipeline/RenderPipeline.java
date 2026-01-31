package com.internal.bootstrap.renderpipeline;

import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.core.engine.PipelinePackage;

public class RenderPipeline extends PipelinePackage {

    // Render Pipeline
    private CameraManager cameraManager;
    private RenderSystem renderSystem;

    @Override
    protected void create() {

        // Render Pipeline
        this.cameraManager = create(CameraManager.class);
        this.renderSystem = create(RenderSystem.class);
    }

    // Render Pipeline \\

    // TODO: Not happy with this here
    public void draw() {
        renderSystem.draw();
    }
}
