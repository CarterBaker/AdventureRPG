package application.bootstrap.worldpipeline.megastreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import engine.root.ManagerPackage;

public class MegaStreamManager extends ManagerPackage {

    /*
     * Internal mega chunk pipeline facade. Owned and created by
     * WorldStreamManager. All operations are per-GridInstance — each grid
     * owns its own activeMegaChunks map. The mega pool is shared internally.
     * resolveMegaForChunk touches that shared, non-thread-safe registry and
     * pool and must only ever be called from the main thread; mergeIntoMega
     * is safe from any thread since the target mega's own lock guards it.
     */

    // Internal
    private MegaQueueManager megaQueueManager;

    // Internal \\

    @Override
    protected void create() {
        this.megaQueueManager = create(MegaQueueManager.class);
    }

    // Grid Events \\

    public void onGridRebuilt(GridInstance grid) {
        megaQueueManager.onGridRebuilt(grid);
    }

    public void onGridRemoved(GridInstance grid) {
        megaQueueManager.onGridRemoved(grid);
    }

    // Accessible \\

    public MegaChunkInstance resolveMegaForChunk(ChunkInstance chunkInstance, GridInstance grid) {
        return megaQueueManager.resolveMegaForChunk(chunkInstance, grid);
    }

    public void mergeIntoMega(ChunkInstance chunkInstance, MegaChunkInstance mega) {
        megaQueueManager.mergeIntoMega(chunkInstance, mega);
    }

    public void invalidateMegaForChunk(long chunkCoordinate) {
        megaQueueManager.invalidateMegaForChunk(chunkCoordinate);
    }
}