package program.bootstrap.worldpipeline;

import program.bootstrap.worldpipeline.biomemanager.BiomeManager;
import program.bootstrap.worldpipeline.blockmanager.BlockManager;
import program.bootstrap.worldpipeline.gridmanager.GridManager;
import program.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import program.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem;
import program.bootstrap.worldpipeline.worldmanager.WorldManager;
import program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import program.core.engine.PipelinePackage;

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