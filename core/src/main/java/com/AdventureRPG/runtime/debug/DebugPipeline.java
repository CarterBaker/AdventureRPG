package com.AdventureRPG.runtime.debug;

import com.AdventureRPG.core.engine.PipelinePackage;

public class DebugPipeline extends PipelinePackage {

    // Lighting
    public Sky sky;

    // Base \\

    @Override
    protected void create() {

        // Lighting
        this.sky = create(Sky.class);
    }
}