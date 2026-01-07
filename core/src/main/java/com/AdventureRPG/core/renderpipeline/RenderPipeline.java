package com.AdventureRPG.core.renderpipeline;

import com.AdventureRPG.core.engine.PipelinePackage;
import com.AdventureRPG.core.renderpipeline.camerasystem.CameraManager;
import com.AdventureRPG.core.renderpipeline.rendersystem.RenderSystem;

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
