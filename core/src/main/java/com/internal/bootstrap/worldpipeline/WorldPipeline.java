package com.internal.bootstrap.worldpipeline;

import com.internal.bootstrap.worldpipeline.biomemanager.BiomeManager;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.PipelinePackage;

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
