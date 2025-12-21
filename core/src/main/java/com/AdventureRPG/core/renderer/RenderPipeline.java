package com.AdventureRPG.core.renderer;

import com.AdventureRPG.core.engine.PipelineFrame;
import com.AdventureRPG.core.renderer.camerasystem.CameraManager;
import com.AdventureRPG.core.renderer.rendermanager.RenderManager;

public class RenderPipeline extends PipelineFrame {

    // Render Pipeline
    private CameraManager cameraManager;
    private RenderManager renderManager;

    @Override
    protected void create() {

        // Render Pipeline
        this.cameraManager = (CameraManager) register(new CameraManager());
        this.renderManager = (RenderManager) register(new RenderManager());
    }

    // Camera Manager \\

    public void resize(int width, int height) {
        cameraManager.resize(width, height);
    }

    // Render Pipeline \\

    public void draw() {

    }
}
