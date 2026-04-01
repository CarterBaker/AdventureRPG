package program.bootstrap.worldpipeline.chunkstreammanager;

import program.bootstrap.geometrypipeline.vao.VAOHandle;
import program.bootstrap.geometrypipeline.vaomanager.VAOManager;
import program.bootstrap.worldpipeline.chunk.ChunkInstance;
import program.bootstrap.worldpipeline.grid.GridInstance;
import program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import program.core.engine.ManagerPackage;
import program.core.settings.EngineSetting;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ChunkStreamManager extends ManagerPackage {

    /*
     * Internal chunk streaming implementation. Owned and created by
     * WorldStreamManager. All queue logic is delegated to ChunkQueueManager.
     * Coordinate tracking and render queue rebuilds are owned by each GridInstance
     * and driven by WorldStreamManager.update().
     */

    // Internal
    private VAOManager vaoManager;
    private WorldStreamManager worldStreamManager;
    private ChunkQueueManager chunkQueueManager;

    // State
    private VAOHandle chunkVAO;

    // Internal \\

    @Override
    protected void create() {
        this.chunkQueueManager = create(ChunkQueueManager.class);
    }

    @Override
    protected void get() {
        this.vaoManager = get(VAOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void start() {
        this.chunkVAO = vaoManager.getVAOHandleFromVAOName(EngineSetting.CHUNK_VAO);
    }

    // Grid Events \\

    public void onGridRebuilt(GridInstance grid) {
        chunkQueueManager.onGridRebuilt(grid);
    }

    public void onGridRemoved(GridInstance grid) {
        chunkQueueManager.onGridRemoved(grid);
    }

    // Utility \\

    public void invalidateChunkBatch(long chunkCoordinate) {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {
            GridInstance grid = (GridInstance) elements[i];
            ChunkInstance chunk = grid.getActiveChunks().get(chunkCoordinate);

            if (chunk != null) {
                chunkQueueManager.invalidateChunkBatch(chunk);
                return;
            }
        }
    }

    // Accessible \\

    public VAOHandle getChunkVAO() {
        return chunkVAO;
    }
}