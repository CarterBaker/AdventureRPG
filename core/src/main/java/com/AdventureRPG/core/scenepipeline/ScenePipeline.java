package com.AdventureRPG.core.scenepipeline;

import com.AdventureRPG.core.engine.PipelineFrame;
import com.AdventureRPG.core.scenepipeline.worldenginesystem.WorldEngineSystem;

public class ScenePipeline extends PipelineFrame {

    // Scene Pipeline
    private WorldEngineSystem worldEngineSystem;

    @Override
    protected void create() {

        // Scene Pipeline
        this.worldEngineSystem = create(WorldEngineSystem.class);
    }
}
