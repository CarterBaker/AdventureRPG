package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;

/**
 * Per-chunk-column memo of WorldGenerationManager.computeColumn()'s output —
 * biome, dressing
 * block IDs, and the 256 ground heights for a chunk column. computeColumn() is
 * a pure function
 * of (seed, coordinate), so a cache hit and a fresh recompute always produce
 * identical results;
 * this exists purely to skip the noise/biome work on a GENERATION_DATA reload,
 * never to
 * preserve player-edited state — it never observes a block write, so it carries
 * none. Heights
 * are stored as short rather than int: TERRAIN_MIN/MAX_HEIGHT_BLOCKS bound
 * every value to
 * [24, 900], comfortably inside a short, halving this struct's footprint for
 * free.
 */
public class GenerationCacheStruct extends StructPackage {

    private static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

    private boolean valid;
    private long cachedChunkCoordinate;

    private short biomeID;
    private short surfaceBlockID;
    private short subsurfaceBlockID;
    private short underwaterBlockID;

    private final short[] groundHeightBlocks = new short[COLUMN_COUNT];
    private short columnMinGroundHeightBlocks;
    private short columnMaxGroundHeightBlocks;
    private short columnTopBlocks;

    // Store \\

    public void store(
            long chunkCoordinate,
            short biomeID,
            short surfaceBlockID,
            short subsurfaceBlockID,
            short underwaterBlockID,
            int[] groundHeightBlocks,
            int columnMinGroundHeightBlocks,
            int columnMaxGroundHeightBlocks,
            int columnTopBlocks) {

        this.cachedChunkCoordinate = chunkCoordinate;
        this.biomeID = biomeID;
        this.surfaceBlockID = surfaceBlockID;
        this.subsurfaceBlockID = subsurfaceBlockID;
        this.underwaterBlockID = underwaterBlockID;

        for (int i = 0; i < COLUMN_COUNT; i++)
            this.groundHeightBlocks[i] = (short) groundHeightBlocks[i];

        this.columnMinGroundHeightBlocks = (short) columnMinGroundHeightBlocks;
        this.columnMaxGroundHeightBlocks = (short) columnMaxGroundHeightBlocks;
        this.columnTopBlocks = (short) columnTopBlocks;
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

    public short getSurfaceBlockID() {
        return surfaceBlockID;
    }

    public short getSubsurfaceBlockID() {
        return subsurfaceBlockID;
    }

    public short getUnderwaterBlockID() {
        return underwaterBlockID;
    }

    /*
     * Widens the cached shorts directly into the caller's own int[] scratch buffer
     * — no
     * intermediate array is ever materialized here, so this costs nothing beyond
     * the copy
     * WorldGenerationManager already needed to do.
     */
    public void copyGroundHeightsInto(int[] destination) {
        for (int i = 0; i < COLUMN_COUNT; i++)
            destination[i] = groundHeightBlocks[i];
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
}