package com.AdventureRPG.core.renderer;

import com.AdventureRPG.core.engine.PipelineFrame;
import com.AdventureRPG.core.renderer.camerasystem.CameraManager;
import com.AdventureRPG.core.renderer.rendersystem.RenderSystem;

public class RenderPipeline extends PipelineFrame {

    // Render Pipeline
    private CameraManager cameraManager;
    private RenderSystem renderSystem;

    @Override
    protected void create() {

        // Render Pipeline
        this.cameraManager = (CameraManager) register(new CameraManager());
        this.renderSystem = (RenderSystem) register(new RenderSystem());
    }

    // Camera Manager \\

    public void resize(int width, int height) {
        cameraManager.resize(width, height);
    }

    // Render Pipeline \\

    public void draw() {
        renderSystem.draw();
    }
}
