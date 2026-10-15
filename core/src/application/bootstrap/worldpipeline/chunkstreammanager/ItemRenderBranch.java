package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.entitypipeline.npcmanager.NpcManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import engine.root.BranchPackage;

public class ItemRenderBranch extends BranchPackage {

    /*
     * Main thread — always tryAcquire, never async. Pushes the chunk's built
     * item instances to the composite renderer, and brings every NPC living
     * at the chunk's world spawns into the world with them. Fires only after
     * ITEM_DATA is true — instances already built by ItemLoadBranch. Sets
     * ITEM_RENDER_DATA on success.
     */

    // Internal
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private NpcManager npcManager;

    // Settings
    private int itemRenderDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.npcManager = get(NpcManager.class);

        // Settings
        this.itemRenderDataIndex = ChunkData.ITEM_RENDER_DATA.index;
    }

    // Item Render \\

    public void renderItems(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {
            worldItemPlacementSystem.pushChunkToRenderer(
                    chunkInstance, chunkInstance.getCoordinate());
            npcManager.spawnResidents(chunkInstance);
            syncContainer.getData()[itemRenderDataIndex] = true;
        } finally {
            syncContainer.release();
        }
    }
}