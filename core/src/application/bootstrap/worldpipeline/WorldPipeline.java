package application.bootstrap.worldpipeline;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.gridmanager.GridManager;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import application.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import application.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.core.engine.PipelinePackage;

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