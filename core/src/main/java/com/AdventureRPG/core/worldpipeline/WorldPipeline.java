package com.AdventureRPG.core.worldpipeline;

import com.AdventureRPG.core.engine.PipelinePackage;
import com.AdventureRPG.core.worldpipeline.worldloadmanager.WorldLoadManager;

public class WorldPipeline extends PipelinePackage {

    /* !!! Released after use !!! */

    @Override
    protected void create() {

        // World Pipeline
        create(WorldLoadManager.class);
    }
}
