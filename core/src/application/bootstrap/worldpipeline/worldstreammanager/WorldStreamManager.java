package application.bootstrap.worldpipeline.worldstreammanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridmanager.GridManager;
import application.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldStreamManager extends ManagerPackage {

    /*
     * Single public entry point for world streaming. Owns the grid registry and
     * grid lifecycle, one grid per window, and drives coordinate tracking each
     * frame. A rebuild re-lays a grid's slots in place, and the frame a grid
     * wraps around the player raises wrappingPlayer so WorldTickManager holds
     * off.
     */

    // Internal
    private GridManager gridManager;
    private ChunkStreamManager chunkStreamManager;
    private MegaStreamManager megaStreamManager;

    // Grids
    private ObjectArrayList<GridInstance> grids;

    // Tick Coordination
    private boolean wrappingPlayer;

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
        boolean rebuilt = false;

        for (int i = 0; i < size; i++)
            if (((GridInstance) elements[i]).updateActiveChunkCoordinate())
                rebuilt = true;

        this.wrappingPlayer = rebuilt;
    }

    // Grid Lifecycle \\

    public GridInstance createGrid(EntityInstance focalEntity, WindowInstance windowInstance,
            FBOInstance renderTargetFbo) {
        GridInstance grid = gridManager.buildGrid(focalEntity, windowInstance, renderTargetFbo);
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
        gridManager.rebuildGrid(grid);
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

    // Tick Coordination \\

    public boolean isWrappingPlayer() {
        return wrappingPlayer;
    }
}