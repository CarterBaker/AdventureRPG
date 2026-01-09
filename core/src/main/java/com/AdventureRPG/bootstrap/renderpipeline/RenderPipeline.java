package com.AdventureRPG.bootstrap.renderpipeline;

import com.AdventureRPG.bootstrap.renderpipeline.camerasystem.CameraManager;
import com.AdventureRPG.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.AdventureRPG.core.engine.PipelinePackage;

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
