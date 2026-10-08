package application.bootstrap.worldpipeline.worldstreammanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridmanager.GridManager;
import application.bootstrap.worldpipeline.macrostreammanager.MacroStreamManager;
import application.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import application.bootstrap.worldpipeline.world.WorldEditRegionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldStreamManager extends ManagerPackage {

    /*
     * Single public entry point for world streaming. Owns the grid registry and
     * grid lifecycle, one grid per window, and drives coordinate tracking each
     * frame. Chunks, megas and the distant macro terrain beyond the grid each
     * stream through their own manager. A rebuild re-lays a grid's slots in
     * place, and the frame a grid wraps around the player raises
     * wrappingPlayer so WorldTickManager holds off. Live world and biome edits
     * gather into one region and, once quiet for a moment, restream in place
     * only the chunks and macro tiles that region reaches, so a preview or Dev
     * window far from the brush never streams again, one near it keeps every
     * chunk the edit cannot change, and a player whose own chunk regenerated is
     * set down again on the new ground. The restream frame raises
     * wrappingPlayer as a wrap does.
     */

    // Internal
    private GridManager gridManager;
    private BiomeManager biomeManager;
    private PlayerManager playerManager;
    private ChunkStreamManager chunkStreamManager;
    private MegaStreamManager megaStreamManager;
    private MacroStreamManager macroStreamManager;

    // Grids
    private ObjectArrayList<GridInstance> grids;

    // Tick Coordination
    private boolean wrappingPlayer;

    // Live Rebuild
    private int rebuiltBiomeRevision;
    private WorldEditRegionStruct liveRebuildRegion;
    private float liveRebuildCountdown;

    // Internal \\

    @Override
    protected void create() {

        this.grids = new ObjectArrayList<>();
        this.liveRebuildRegion = new WorldEditRegionStruct();
        this.chunkStreamManager = create(ChunkStreamManager.class);
        this.megaStreamManager = create(MegaStreamManager.class);
        this.macroStreamManager = create(MacroStreamManager.class);
    }

    @Override
    protected void get() {
        this.gridManager = get(GridManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.playerManager = get(PlayerManager.class);
    }

    @Override
    protected void update() {

        Object[] elements = grids.elements();
        int size = grids.size();
        boolean rebuilt = false;

        for (int i = 0; i < size; i++)
            if (((GridInstance) elements[i]).updateActiveChunkCoordinate())
                rebuilt = true;

        if (advanceLiveRebuild())
            rebuilt = true;

        this.wrappingPlayer = rebuilt;
    }

    // Live Rebuild \\

    public void requestLiveRebuild(
            WorldHandle editedWorldHandle,
            int minPixelX,
            int minPixelZ,
            int maxPixelX,
            int maxPixelZ) {

        if (grids.isEmpty())
            return;

        liveRebuildRegion.includePixels(editedWorldHandle, minPixelX, minPixelZ, maxPixelX, maxPixelZ);
        this.liveRebuildCountdown = EngineSetting.WORLD_LIVE_REBUILD_DELAY_SECONDS;
    }

    // A rebuilt biome can change any part of the field, so it reaches everything
    private void requestBiomeRebuild() {

        if (grids.isEmpty())
            return;

        liveRebuildRegion.includeEverything();
        this.liveRebuildCountdown = EngineSetting.WORLD_LIVE_REBUILD_DELAY_SECONDS;
    }

    private boolean advanceLiveRebuild() {

        int biomeRevision = biomeManager.getRevision();

        if (biomeRevision != rebuiltBiomeRevision) {
            this.rebuiltBiomeRevision = biomeRevision;
            requestBiomeRebuild();
        }

        if (liveRebuildRegion.isEmpty())
            return false;

        liveRebuildCountdown -= internal.getDeltaTime();

        if (liveRebuildCountdown > 0f)
            return false;

        for (int i = 0; i < grids.size(); i++)
            restreamRegion(grids.get(i), liveRebuildRegion);

        liveRebuildRegion.clear();
        return true;
    }

    private void restreamRegion(GridInstance grid, WorldEditRegionStruct region) {

        chunkStreamManager.restreamRegion(grid, region);
        macroStreamManager.restreamRegion(grid, region);

        if (!region.reachesChunk(grid.getWorldHandle(), grid.getActiveChunkCoordinate()))
            return;

        int windowID = grid.getWindowInstance().getWindowID();

        if (playerManager.hasPlayerForWindow(windowID))
            playerManager.verifyPlayerPositionForWindow(windowID);
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
        macroStreamManager.onGridRemoved(grid);
    }

    public void rebuildGrid(GridInstance grid) {
        chunkStreamManager.onGridRebuilt(grid);
        megaStreamManager.onGridRebuilt(grid);
        macroStreamManager.onGridRebuilt(grid);
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

    // The grid that streams a chunk, whose weather and sea the world there follows — null when none does
    public GridInstance getGridForChunk(long chunkCoordinate) {

        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];

            if (grid.getGridSlotForChunk(chunkCoordinate) != null)
                return grid;
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