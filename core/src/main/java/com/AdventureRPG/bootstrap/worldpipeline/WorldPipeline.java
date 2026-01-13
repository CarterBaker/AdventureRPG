package com.AdventureRPG.bootstrap.worldpipeline;

import com.AdventureRPG.bootstrap.worldpipeline.biomemanager.BiomeManager;
import com.AdventureRPG.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.AdventureRPG.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.AdventureRPG.bootstrap.worldpipeline.gridmanager.GridManager;
import com.AdventureRPG.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import com.AdventureRPG.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.AdventureRPG.core.engine.PipelinePackage;

public class WorldPipeline extends PipelinePackage {

    /* !!! Released after use !!! */

    @Override
    protected void create() {

        // World Pipeline
        create(WorldStreamManager.class);
        create(BlockManager.class);
        create(BiomeManager.class);
        create(WorldGenerationManager.class);
        create(GridManager.class);
        create(ChunkStreamManager.class);
    }
}
