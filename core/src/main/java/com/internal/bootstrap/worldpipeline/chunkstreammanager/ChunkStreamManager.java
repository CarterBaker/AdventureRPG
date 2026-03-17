package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ChunkStreamManager extends ManagerPackage {

    /*
     * Internal chunk streaming implementation. Owned and created by
     * WorldStreamManager. Drives per-frame grid coordinate tracking and
     * triggers render queue rebuilds on boundary crossings. All queue logic
     * is delegated to ChunkQueueManager.
     */

    // Internal
    private VAOManager vaoManager;
    private WorldStreamManager worldStreamManager;
    private WorldRenderManager worldRenderManager;
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

        // Internal
        this.vaoManager = get(VAOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    @Override
    protected void start() {
        this.chunkVAO = vaoManager.getVAOHandleFromVAOName(EngineSetting.CHUNK_VAO);
    }

    @Override
    protected void update() {
        streamAllGrids();
    }

    // Streaming \\

    private void streamAllGrids() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            streamGrid((GridInstance) elements[i]);
    }

    private void streamGrid(GridInstance grid) {

        long entityChunkCoordinate = grid.getFocalEntity()
                .getWorldPositionStruct()
                .getChunkCoordinate();

        if (grid.getActiveChunkCoordinate() == entityChunkCoordinate)
            return;

        grid.setActiveChunkCoordinate(entityChunkCoordinate);
        worldRenderManager.rebuildRenderQueue();
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