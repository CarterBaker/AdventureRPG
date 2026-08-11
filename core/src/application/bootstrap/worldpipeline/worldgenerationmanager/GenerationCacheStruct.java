package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class GenerationCacheStruct extends StructPackage {

    /*
     * Persists the deterministic per-column terrain output — ground heights,
     * biome, and dressing block IDs — that WorldGenerationManager.computeColumn
     * already derived for this chunk, so a later GENERATION_DATA reload (fired
     * every time this chunk cycles back from DISTANT into NEAR range) can skip
     * the macro/detail noise stack and biome PNG resolution entirely and go
     * straight to palette reconstruction. Cheap to keep resident — one int per
     * block column — relative to the noise and biome work it replaces. Owned
     * by ChunkInstance and invalidated whenever that instance is reset for
     * reuse at a different coordinate.
     */

    private static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

    private boolean valid;
    private long cachedChunkCoordinate;

    private short biomeID;
    private short surfaceBlockID;
    private short subsurfaceBlockID;
    private short underwaterBlockID;

    private final int[] groundHeightBlocks = new int[COLUMN_COUNT];
    private int columnMinGroundHeightBlocks;
    private int columnMaxGroundHeightBlocks;
    private int columnTopBlocks;

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
        System.arraycopy(groundHeightBlocks, 0, this.groundHeightBlocks, 0, COLUMN_COUNT);
        this.columnMinGroundHeightBlocks = columnMinGroundHeightBlocks;
        this.columnMaxGroundHeightBlocks = columnMaxGroundHeightBlocks;
        this.columnTopBlocks = columnTopBlocks;
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

    public int[] getGroundHeightBlocks() {
        return groundHeightBlocks;
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