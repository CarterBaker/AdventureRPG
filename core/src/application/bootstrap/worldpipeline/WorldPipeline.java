package application.bootstrap.worldpipeline;

import application.bootstrap.worldpipeline.architecturemanager.ArchitectureManager;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.cavebiomemanager.CaveBiomeManager;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.gridmanager.GridManager;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.liquidmanager.LiquidManager;
import application.bootstrap.worldpipeline.roadmanager.RoadManager;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.structurelistmanager.StructureListManager;
import application.bootstrap.worldpipeline.structuremanager.StructureManager;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemManager;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.bootstrap.worldpipeline.worldtickmanager.WorldTickManager;
import engine.root.PipelinePackage;

public class WorldPipeline extends PipelinePackage {

    /*
     * Registers all world pipeline managers in dependency order. Cross-system
     * references resolve in each manager's get() phase after all managers are
     * created. CoveringManager follows BlockManager, whose blocks its
     * coverings name as hosts, and CaveBiomeManager follows both, since a
     * cave biome lines its caves with blocks and coverings, ahead of the
     * biomes that hold cave biomes beneath them. WorldStreamManager must
     * update before WorldRenderManager each frame so the render queue is
     * current when rendering runs. WorldTickManager
     * is registered immediately after WorldStreamManager so its update() runs
     * later in the same frame, after that frame's wrap state is known.
     * StructureManager follows WorldGenerationManager, whose terrain probe it
     * anchors structures against, and the settlement managers follow it in
     * the order they resolve one another: roads, the structure lists that
     * draw from structures, the architectures that name both, the layouts
     * they are laid as, and the settlements built from them all. TreeManager
     * follows in turn, so the game day it publishes each frame is current
     * before the world streams.
     */

    @Override
    protected void create() {
        create(WorldManager.class);
        create(BlockManager.class);
        create(CoveringManager.class);
        create(CaveBiomeManager.class);
        create(BiomeManager.class);
        create(LiquidManager.class);
        create(WorldGenerationManager.class);
        create(StructureManager.class);
        create(RoadManager.class);
        create(StructureListManager.class);
        create(ArchitectureManager.class);
        create(LayoutManager.class);
        create(SettlementManager.class);
        create(TreeManager.class);
        create(GridManager.class);
        create(WorldStreamManager.class);
        create(WorldTickManager.class);
        create(WorldRenderManager.class);
        create(WorldItemManager.class);
    }
}