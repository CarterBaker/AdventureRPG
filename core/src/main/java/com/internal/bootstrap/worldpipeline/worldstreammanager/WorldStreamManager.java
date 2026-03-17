package com.internal.bootstrap.worldpipeline.worldstreammanager;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldStreamManager extends ManagerPackage {

    /*
     * Single public entry point for all world streaming. Owns the grid registry
     * and drives grid lifecycle — create, remove, rebuild. ChunkStreamManager
     * and MegaStreamManager are internal implementation details created here.
     * All external systems that need streaming data go through this class only.
     */

    // Internal
    private GridManager gridManager;
    private ChunkStreamManager chunkStreamManager;
    private MegaStreamManager megaStreamManager;

    // Grids
    private ObjectArrayList<GridInstance> grids;

    // Internal \\

    @Override
    protected void create() {

        // Grids
        this.grids = new ObjectArrayList<>();

        // Internal
        this.chunkStreamManager = create(ChunkStreamManager.class);
        this.megaStreamManager = create(MegaStreamManager.class);
    }

    @Override
    protected void get() {

        // Internal
        this.gridManager = get(GridManager.class);
    }

    // Grid Lifecycle \\

    public GridInstance createGrid(EntityInstance focalEntity) {

        GridInstance grid = gridManager.buildGrid(focalEntity);
        grids.add(grid);

        return grid;
    }

    public void removeGrid(GridInstance grid) {

        grids.remove(grid);

        chunkStreamManager.onGridRemoved(grid);
        megaStreamManager.onGridRemoved(grid);
    }

    public void rebuildGrid(GridInstance grid) {

        chunkStreamManager.onGridRebuilt(grid);
        megaStreamManager.onGridRebuilt(grid);

        GridInstance rebuilt = gridManager.buildGrid(grid.getFocalEntity());

        int index = grids.indexOf(grid);
        grids.set(index, rebuilt);
    }

    // Utility \\

    public void invalidateChunkBatch(long chunkCoordinate) {
        chunkStreamManager.invalidateChunkBatch(chunkCoordinate);
    }

    public void invalidateMegaForChunk(long chunkCoordinate) {
        megaStreamManager.invalidateMegaForChunk(chunkCoordinate);
    }

    // Accessible \\

    public ObjectArrayList<GridInstance> getGrids() {
        return grids;
    }

    public boolean hasGrids() {
        return !grids.isEmpty();
    }

    public ChunkInstance getChunkInstance(long chunkCoordinate) {

        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {
            ChunkInstance chunk = ((GridInstance) elements[i]).getActiveChunks().get(chunkCoordinate);
            if (chunk != null)
                return chunk;
        }

        return null;
    }

    public WorldHandle getActiveWorldHandle() {

        if (grids.isEmpty())
            return null;

        return grids.get(0).getWorldHandle();
    }
}