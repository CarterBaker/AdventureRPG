package com.internal.bootstrap.worldpipeline;

import com.internal.bootstrap.worldpipeline.biomemanager.BiomeManager;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import com.internal.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import com.internal.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem;
import com.internal.bootstrap.worldpipeline.worldmanager.WorldManager;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.PipelinePackage;

public class WorldPipeline extends PipelinePackage {
    @Override
    protected void create() {
        create(WorldManager.class);
        create(BlockManager.class);
        create(BiomeManager.class);
        create(WorldGenerationManager.class);
        create(GridManager.class);
        create(ChunkStreamManager.class);
        create(MegaStreamManager.class);
        create(WorldRenderManager.class);
        create(WorldItemRenderSystem.class);
    }
}