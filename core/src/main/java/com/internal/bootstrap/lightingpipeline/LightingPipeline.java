package com.internal.bootstrap.lightingpipeline;

import com.internal.bootstrap.lightingpipeline.naturallightmanager.NaturalLightManager;
import com.internal.core.engine.PipelinePackage;

public class LightingPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Lighting Pipeline
        create(NaturalLightManager.class);
    }
}
