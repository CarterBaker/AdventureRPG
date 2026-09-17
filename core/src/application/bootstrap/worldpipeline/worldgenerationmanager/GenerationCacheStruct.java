package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;

/**
 * Per-chunk-column memo of WorldGenerationManager.computeColumn()'s output —
 * the chunk's identity biome, and per block column its ground height, its
 * dressing blocks, and whether it floods below sea level. computeColumn() is a
 * pure function of (seed, coordinate), so a cache hit and a fresh recompute
 * always produce identical results; this exists purely to skip the noise and
 * biome field work on a GENERATION_DATA reload, never to preserve player-edited
 * state — it never observes a block write, so it carries none. Heights are
 * stored as short rather than int: TERRAIN_MIN/MAX_HEIGHT_BLOCKS bound every
 * value to [24, 900], comfortably inside a short, halving that array's
 * footprint for free.
 */
public class GenerationCacheStruct extends StructPackage {

    private static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

    private boolean valid;
    private long cachedChunkCoordinate;

    private short biomeID;

    private final short[] groundHeightBlocks = new short[COLUMN_COUNT];
    private final short[] surfaceBlockID = new short[COLUMN_COUNT];
    private final short[] subsurfaceBlockID = new short[COLUMN_COUNT];
    private final short[] underwaterBlockID = new short[COLUMN_COUNT];
    private final boolean[] oceanWater = new boolean[COLUMN_COUNT];

    private short columnMinGroundHeightBlocks;
    private short columnMaxGroundHeightBlocks;
    private short columnTopBlocks;

    private boolean allOceanWater;
    private boolean allFillBlocksFullGeometry;

    // Store \\

    public void store(
            long chunkCoordinate,
            short biomeID,
            int[] groundHeightBlocks,
            short[] surfaceBlockID,
            short[] subsurfaceBlockID,
            short[] underwaterBlockID,
            boolean[] oceanWater,
            int columnMinGroundHeightBlocks,
            int columnMaxGroundHeightBlocks,
            int columnTopBlocks,
            boolean allOceanWater,
            boolean allFillBlocksFullGeometry) {

        this.cachedChunkCoordinate = chunkCoordinate;
        this.biomeID = biomeID;

        for (int i = 0; i < COLUMN_COUNT; i++)
            this.groundHeightBlocks[i] = (short) groundHeightBlocks[i];

        System.arraycopy(surfaceBlockID, 0, this.surfaceBlockID, 0, COLUMN_COUNT);
        System.arraycopy(subsurfaceBlockID, 0, this.subsurfaceBlockID, 0, COLUMN_COUNT);
        System.arraycopy(underwaterBlockID, 0, this.underwaterBlockID, 0, COLUMN_COUNT);
        System.arraycopy(oceanWater, 0, this.oceanWater, 0, COLUMN_COUNT);

        this.columnMinGroundHeightBlocks = (short) columnMinGroundHeightBlocks;
        this.columnMaxGroundHeightBlocks = (short) columnMaxGroundHeightBlocks;
        this.columnTopBlocks = (short) columnTopBlocks;

        this.allOceanWater = allOceanWater;
        this.allFillBlocksFullGeometry = allFillBlocksFullGeometry;

        this.valid = true;
    }

    public void invalidate() {
        this.valid = false;
    }

    public boolean isValidFor(long chunkCoordinate) {
        return valid && cachedChunkCoordinate == chunkCoordinate;
    }

    // Accessible \\

    public short getBiomeID() {
        return biomeID;
    }

    /*
     * Widens the cached shorts directly into the caller's own int[] scratch
     * buffer — no intermediate array is ever materialized here, so this costs
     * nothing beyond the copy WorldGenerationManager already needed to do.
     */
    public void copyGroundHeightsInto(int[] destination) {
        for (int i = 0; i < COLUMN_COUNT; i++)
            destination[i] = groundHeightBlocks[i];
    }

    public void copySurfaceBlockIDsInto(short[] destination) {
        System.arraycopy(surfaceBlockID, 0, destination, 0, COLUMN_COUNT);
    }

    public void copySubsurfaceBlockIDsInto(short[] destination) {
        System.arraycopy(subsurfaceBlockID, 0, destination, 0, COLUMN_COUNT);
    }

    public void copyUnderwaterBlockIDsInto(short[] destination) {
        System.arraycopy(underwaterBlockID, 0, destination, 0, COLUMN_COUNT);
    }

    public void copyOceanWaterInto(boolean[] destination) {
        System.arraycopy(oceanWater, 0, destination, 0, COLUMN_COUNT);
    }

    public int getColumnMinGroundHeightBlocks() {
        return columnMinGroundHeightBlocks;
    }

    public int getColumnMaxGroundHeightBlocks() {
        return columnMaxGroundHeightBlocks;
    }

    public int getColumnTopBlocks() {
        return columnTopBlocks;
    }

    public boolean hasAllOceanWater() {
        return allOceanWater;
    }

    public boolean hasAllFillBlocksFullGeometry() {
        return allFillBlocksFullGeometry;
    }
}