package application.bootstrap.worldpipeline.liquidmanager;

import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.TideUtility;
import application.bootstrap.worldpipeline.worldgenerationmanager.GenerationCacheStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;

class LiquidTideBranch extends BranchPackage {

    /*
     * Re-levels one chunk's ocean to a new tide surface. Only columns world
     * generation marked as reached by the ocean are visited, and only through
     * the tide band — beneath it the sea is full at every tide, above it no
     * tide ever reaches — so re-tiding a chunk touches a few cells per column
     * rather than its whole water volume. Within the band, tidal cells are
     * re-levelled or emptied and air is flooded, but only while nothing but
     * water lies between it and the sea floor: a floor, roof, or any other
     * block closes the column, so the tide rises around a structure instead
     * of filling it. Water the flow simulation owns is never
     * touched. Nothing is woken — the tide moves the whole ocean together,
     * and waking the permanent cells beside a lowered one would pour them
     * straight back in.
     */

    // Internal
    private LiquidManager liquidManager;
    private BlockManager blockManager;

    // Block IDs
    private short airBlockID;
    private short waterBlockID;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.liquidManager = get(LiquidManager.class);
        this.blockManager = get(BlockManager.class);
    }

    @Override
    protected void awake() {

        // Block IDs
        this.airBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
        this.waterBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.DEFAULT_WATER_BLOCK_NAME);
    }

    // Tide \\

    boolean applyTide(ChunkInstance chunkInstance, int surfaceLevels) {

        GenerationCacheStruct terrainCache = chunkInstance.getTerrainCache();
        boolean tidal = terrainCache.isValidFor(chunkInstance.getCoordinate()) && terrainCache.hasTidalColumns();

        if (tidal)
            for (int localZ = 0; localZ < EngineSetting.CHUNK_SIZE; localZ++)
                for (int localX = 0; localX < EngineSetting.CHUNK_SIZE; localX++)
                    applyColumn(chunkInstance, terrainCache, localX, localZ, surfaceLevels);

        chunkInstance.setTideSurfaceLevels(surfaceLevels);

        return tidal;
    }

    private void applyColumn(
            ChunkInstance chunkInstance,
            GenerationCacheStruct terrainCache,
            int localX,
            int localZ,
            int surfaceLevels) {

        int columnIndex = localZ * EngineSetting.CHUNK_SIZE + localX;

        if (!terrainCache.hasOceanWater(columnIndex))
            return;

        int fromY = Math.max(terrainCache.getGroundHeightBlocks(columnIndex) + 1, TideUtility.BAND_MIN_Y);
        boolean open = true;

        for (int worldY = fromY; worldY <= TideUtility.BAND_MAX_Y; worldY++) {

            SubChunkInstance subChunkInstance = chunkInstance.getSubChunk(worldY / EngineSetting.CHUNK_SIZE);
            int packedXYZ = Coordinate3Int.pack(localX, worldY % EngineSetting.CHUNK_SIZE, localZ);
            short blockID = subChunkInstance.getBlock(packedXYZ);
            short level = TideUtility.getFillLevel(surfaceLevels, worldY);

            if (blockID == airBlockID) {

                if (open && level > EngineSetting.LIQUID_LEVEL_EMPTY)
                    liquidManager.writeTidalLiquid(chunkInstance, subChunkInstance, packedXYZ, waterBlockID, level);

                continue;
            }

            if (blockID != waterBlockID) {
                open = false;
                continue;
            }

            if (!subChunkInstance.isLiquidTidal(packedXYZ))
                continue;

            if (level == EngineSetting.LIQUID_LEVEL_EMPTY)
                liquidManager.clearTidalLiquid(chunkInstance, subChunkInstance, packedXYZ);
            else if (level != subChunkInstance.getLiquidLevel(packedXYZ))
                liquidManager.writeTidalLiquid(chunkInstance, subChunkInstance, packedXYZ, waterBlockID, level);
        }
    }
}
