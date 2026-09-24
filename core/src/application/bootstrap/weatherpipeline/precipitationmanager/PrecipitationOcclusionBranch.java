package application.bootstrap.weatherpipeline.precipitationmanager;

import application.bootstrap.weatherpipeline.precipitation.PrecipitationInstance;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.ColumnHeightUtility;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class PrecipitationOcclusionBranch extends BranchPackage {

    /*
     * Keeps each grid's precipitation column map current around its focal
     * entity. The map window is centred on the entity's block, expressed in
     * absolute block columns and anchored to the entity's own chunk corner,
     * which is the same frame the camera position is in. Columns that
     * scrolled into the window are refreshed first, up to a budget, and a
     * rolling cursor re-reads a slice of the whole map every frame so placed
     * or broken blocks start or stop sheltering within a moment.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;

    // Map
    private int mapSize;
    private int mapMask;

    // Base \\

    @Override
    protected void create() {

        // Map
        this.mapSize = EngineSetting.PRECIPITATION_MAP_SIZE;
        this.mapMask = mapSize - 1;
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Occlusion \\

    void updateOcclusion(GridInstance grid, PrecipitationInstance precipitation) {

        WorldPositionStruct focal = grid.getFocalEntity().getWorldPositionStruct();
        long chunkCoordinate = focal.getChunkCoordinate();
        Vector3 position = focal.getPosition();

        int frameX = Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE;
        int frameZ = Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE;
        int windowX = frameX + (int) Math.floor(position.x) - mapSize / 2;
        int windowZ = frameZ + (int) Math.floor(position.z) - mapSize / 2;

        precipitation.setWindow(windowX, windowZ, frameX, frameZ);

        WorldHandle world = grid.getWorldHandle();

        refreshScrolledColumns(world, precipitation, windowX, windowZ);
        refreshRollingColumns(world, precipitation, windowX, windowZ);
    }

    private void refreshScrolledColumns(
            WorldHandle world,
            PrecipitationInstance precipitation,
            int windowX,
            int windowZ) {

        int budget = EngineSetting.PRECIPITATION_STALE_REFRESH_LIMIT;

        for (int slotZ = 0; slotZ < mapSize; slotZ++) {

            int columnZ = resolveColumn(slotZ, windowZ);

            for (int slotX = 0; slotX < mapSize; slotX++) {

                int columnX = resolveColumn(slotX, windowX);
                int slot = slotZ * mapSize + slotX;

                if (precipitation.isColumnCurrent(slot, columnX, columnZ))
                    continue;

                if (budget-- > 0)
                    refreshColumn(world, precipitation, slot, columnX, columnZ);
                else
                    precipitation.clearColumn(slot);
            }
        }
    }

    private void refreshRollingColumns(
            WorldHandle world,
            PrecipitationInstance precipitation,
            int windowX,
            int windowZ) {

        for (int i = 0; i < EngineSetting.PRECIPITATION_REFRESH_PER_FRAME; i++) {

            int slot = precipitation.advanceRefreshCursor();
            int columnX = resolveColumn(slot % mapSize, windowX);
            int columnZ = resolveColumn(slot / mapSize, windowZ);

            refreshColumn(world, precipitation, slot, columnX, columnZ);
        }
    }

    private void refreshColumn(
            WorldHandle world,
            PrecipitationInstance precipitation,
            int slot,
            int columnX,
            int columnZ) {

        long chunkCoordinate = WorldWrapUtility.wrapAroundWorld(
                world,
                Coordinate2Long.pack(
                        Math.floorDiv(columnX, EngineSetting.CHUNK_SIZE),
                        Math.floorDiv(columnZ, EngineSetting.CHUNK_SIZE)));
        ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);

        if (chunk == null || !chunk.getChunkDataSyncContainer().hasData(ChunkData.GENERATION_DATA)) {
            precipitation.clearColumn(slot);
            return;
        }

        int columnTop = ColumnHeightUtility.findColumnTop(
                chunk,
                blockManager,
                Math.floorMod(columnX, EngineSetting.CHUNK_SIZE),
                Math.floorMod(columnZ, EngineSetting.CHUNK_SIZE));

        precipitation.setColumn(slot, columnX, columnZ, columnTop);
    }

    // Utility \\

    // The one absolute column inside the window whose ring slot is this one.
    private int resolveColumn(int slot, int windowOrigin) {
        return windowOrigin + ((slot - windowOrigin) & mapMask);
    }
}
