package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class GenerationCacheStruct extends StructPackage {

    /*
     * Per-chunk memo of computeColumn(): the identity biome and, per block
     * column, ground height, dressing blocks, ocean reach and smoothing
     * octants. Output is a pure function of seed and coordinate, so this only
     * skips recomputation on a reload; heights are stored as shorts.
     */

    private static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

    private boolean valid;
    private long cachedChunkCoordinate;

    private short biomeID;

    private final short[] groundHeightBlocks = new short[COLUMN_COUNT];
    private final short[] surfaceBlockID = new short[COLUMN_COUNT];
    private final short[] subsurfaceBlockID = new short[COLUMN_COUNT];
    private final short[] underwaterBlockID = new short[COLUMN_COUNT];
    private final boolean[] oceanWater = new boolean[COLUMN_COUNT];
    private final byte[] groundMask = new byte[COLUMN_COUNT];
    private final byte[] capMask = new byte[COLUMN_COUNT];

    private short columnMinGroundHeightBlocks;
    private short columnMaxGroundHeightBlocks;
    private short columnTopBlocks;

    private boolean allOceanWater;
    private boolean hasTidalColumns;
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
            byte[] groundMask,
            byte[] capMask,
            int columnMinGroundHeightBlocks,
            int columnMaxGroundHeightBlocks,
            int columnTopBlocks,
            boolean allOceanWater,
            boolean hasTidalColumns,
            boolean allFillBlocksFullGeometry) {

        this.cachedChunkCoordinate = chunkCoordinate;
        this.biomeID = biomeID;

        for (int i = 0; i < COLUMN_COUNT; i++)
            this.groundHeightBlocks[i] = (short) groundHeightBlocks[i];

        System.arraycopy(surfaceBlockID, 0, this.surfaceBlockID, 0, COLUMN_COUNT);
        System.arraycopy(subsurfaceBlockID, 0, this.subsurfaceBlockID, 0, COLUMN_COUNT);
        System.arraycopy(underwaterBlockID, 0, this.underwaterBlockID, 0, COLUMN_COUNT);
        System.arraycopy(oceanWater, 0, this.oceanWater, 0, COLUMN_COUNT);
        System.arraycopy(groundMask, 0, this.groundMask, 0, COLUMN_COUNT);
        System.arraycopy(capMask, 0, this.capMask, 0, COLUMN_COUNT);

        this.columnMinGroundHeightBlocks = (short) columnMinGroundHeightBlocks;
        this.columnMaxGroundHeightBlocks = (short) columnMaxGroundHeightBlocks;
        this.columnTopBlocks = (short) columnTopBlocks;

        this.allOceanWater = allOceanWater;
        this.hasTidalColumns = hasTidalColumns;
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

    public void copyGroundMasksInto(byte[] destination) {
        System.arraycopy(groundMask, 0, destination, 0, COLUMN_COUNT);
    }

    public void copyCapMasksInto(byte[] destination) {
        System.arraycopy(capMask, 0, destination, 0, COLUMN_COUNT);
    }

    public int getGroundHeightBlocks(int columnIndex) {
        return groundHeightBlocks[columnIndex];
    }

    public boolean hasOceanWater(int columnIndex) {
        return oceanWater[columnIndex];
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

    public boolean hasTidalColumns() {
        return hasTidalColumns;
    }

    public boolean hasAllFillBlocksFullGeometry() {
        return allFillBlocksFullGeometry;
    }
}