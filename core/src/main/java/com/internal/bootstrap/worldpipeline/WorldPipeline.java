package com.internal.bootstrap.worldpipeline;

import com.internal.bootstrap.worldpipeline.biomemanager.BiomeManager;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import com.internal.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import com.internal.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem;
import com.internal.bootstrap.worldpipeline.worldmanager.WorldManager;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.PipelinePackage;

public class WorldPipeline extends PipelinePackage {

    /*
     * Registers all world pipeline managers in dependency order. Cross-system
     * references resolve in each manager's get() phase after all managers are
     * created. WorldStreamManager must update before WorldRenderManager each
     * frame so the render queue is current when rendering runs.
     */

    @Override
    protected void create() {
        create(WorldManager.class);
        create(BlockManager.class);
        create(BiomeManager.class);
        create(WorldGenerationManager.class);
        create(GridManager.class);
        create(WorldStreamManager.class);
        create(WorldRenderManager.class);
        create(WorldItemRenderSystem.class);
        create(WorldItemPlacementSystem.class);
    }
}