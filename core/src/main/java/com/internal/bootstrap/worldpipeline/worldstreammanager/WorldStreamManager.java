package com.internal.bootstrap.worldpipeline.worldstreammanager;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.grid.GridInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldStreamManager extends ManagerPackage {

    /*
     * Single public entry point for all world streaming. Owns the grid registry
     * and drives grid lifecycle — create, remove, rebuild. Each grid is tied to
     * a WindowInstance so frustum culling and rendering operate per-window
     * independently. update() drives coordinate tracking across all grids —
     * each grid owns its own render queue rebuild on boundary crossing.
     * ChunkStreamManager and MegaStreamManager are internal.
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

        this.grids = new ObjectArrayList<>();
        this.chunkStreamManager = create(ChunkStreamManager.class);
        this.megaStreamManager = create(MegaStreamManager.class);
    }

    @Override
    protected void get() {
        this.gridManager = get(GridManager.class);
    }

    @Override
    protected void update() {

        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            ((GridInstance) elements[i]).updateActiveChunkCoordinate();
    }

    // Grid Lifecycle \\

    public GridInstance createGrid(EntityInstance focalEntity, WindowInstance windowInstance) {
        GridInstance grid = gridManager.buildGrid(focalEntity, windowInstance);
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
        GridInstance rebuilt = gridManager.buildGrid(grid.getFocalEntity(), grid.getWindowInstance());
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