package com.internal.runtime.debug;

import com.internal.core.engine.PipelinePackage;

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