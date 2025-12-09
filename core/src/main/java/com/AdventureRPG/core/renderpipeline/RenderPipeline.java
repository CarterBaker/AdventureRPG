package com.AdventureRPG.core.renderpipeline;

import com.AdventureRPG.core.kernel.PipelineFrame;
import com.AdventureRPG.core.renderpipeline.rendermanager.RenderManager;

public class RenderPipeline extends PipelineFrame {

    // Render Pipeline
    private RenderManager renderManager;

    @Override
    protected void create() {

        // Render Pipeline
        this.renderManager = (RenderManager) register(new RenderManager());
    }

    // Render Pipeline \\

    public void draw() {

    }
}
