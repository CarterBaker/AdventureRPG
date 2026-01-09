package com.AdventureRPG.bootstrap.worldpipeline;

import com.AdventureRPG.bootstrap.worldpipeline.worldloadmanager.WorldLoadManager;
import com.AdventureRPG.core.engine.PipelinePackage;

public class WorldPipeline extends PipelinePackage {

    /* !!! Released after use !!! */

    @Override
    protected void create() {

        // World Pipeline
        create(WorldLoadManager.class);
    }
}
