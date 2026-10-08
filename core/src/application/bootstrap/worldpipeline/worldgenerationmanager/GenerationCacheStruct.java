package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class GenerationCacheStruct extends StructPackage {

    /*
     * Per-chunk memo of computeColumn(): the identity biome and, per block
     * column, ground height, dressing blocks, sea reach and still water,
     * smoothing octants, the band its caves and sea caves may hollow, its
     * veins and the lowest cell the tide reaches, with the chunk's vein
     * palette and carve ranges. Output is a pure function of seed and
     * coordinate, so this only skips recomputation on a reload; heights are
     * stored as shorts.
     */

    private static final int COLUMN_COUNT = TerrainColumnAsyncContainer.COLUMN_COUNT;

    private boolean valid;
    private long cachedChunkCoordinate;

    private short biomeID;

    // Per Block Column
    private final short[] groundHeightBlocks = new short[COLUMN_COUNT];
    private final short[] topBlockID = new short[COLUMN_COUNT];
    private final short[] fillBlockID = new short[COLUMN_COUNT];
    private final short[] rockBlockID = new short[COLUMN_COUNT];
    private final boolean[] oceanWater = new boolean[COLUMN_COUNT];
    private final int[] lakeLevelBlocks = new int[COLUMN_COUNT];
    private final byte[] groundMask = new byte[COLUMN_COUNT];
    private final byte[] capMask = new byte[COLUMN_COUNT];
    private final TerrainSurfaceProfileStruct[] profile = new TerrainSurfaceProfileStruct[COLUMN_COUNT];

    // Per Block Column — Carving
    private final byte[] seaZone = new byte[COLUMN_COUNT];
    private final int[] caveFloorY = new int[COLUMN_COUNT];
    private final int[] caveCeilingY = new int[COLUMN_COUNT];
    private final float[] caveTunnels = new float[COLUMN_COUNT];
    private final float[] caveCaverns = new float[COLUMN_COUNT];
    private final int[] seaCeilingY = new int[COLUMN_COUNT];
    private final float[] shoreDistanceBlocks = new float[COLUMN_COUNT];
    private final float[] faceDistanceBlocks = new float[COLUMN_COUNT];
    private final float[] overhangBlocks = new float[COLUMN_COUNT];
    private final float[] seaCaves = new float[COLUMN_COUNT];
    private final int[] tideFloorY = new int[COLUMN_COUNT];

    // Whole Column
    private short columnMinGroundHeightBlocks;
    private short columnMaxGroundHeightBlocks;
    private short columnTopBlocks;
    private int carveMinY;
    private int carveMaxY;
    private boolean hasSeaFeatures;
    private int veinMinY;
    private int veinMaxY;

    private boolean allOceanWater;
    private boolean hasTidalColumns;
    private boolean allFillBlocksFullGeometry;

    // Vein Palette
    private final long[] veinSeeds = new long[EngineSetting.TERRAIN_VEIN_PALETTE_MAX];
    private int veinCount;

    // Store \\

    void store(TerrainColumnAsyncContainer column) {

        this.cachedChunkCoordinate = column.computedChunkCoordinate;
        this.biomeID = column.biomeID;

        for (int i = 0; i < COLUMN_COUNT; i++)
            this.groundHeightBlocks[i] = (short) column.groundHeightBlocks[i];

        System.arraycopy(column.columnTopBlockID, 0, topBlockID, 0, COLUMN_COUNT);
        System.arraycopy(column.columnFillBlockID, 0, fillBlockID, 0, COLUMN_COUNT);
        System.arraycopy(column.columnRockBlockID, 0, rockBlockID, 0, COLUMN_COUNT);
        System.arraycopy(column.columnOceanWater, 0, oceanWater, 0, COLUMN_COUNT);
        System.arraycopy(column.columnLakeLevelBlocks, 0, lakeLevelBlocks, 0, COLUMN_COUNT);
        System.arraycopy(column.columnGroundMask, 0, groundMask, 0, COLUMN_COUNT);
        System.arraycopy(column.columnCapMask, 0, capMask, 0, COLUMN_COUNT);
        System.arraycopy(column.columnProfile, 0, profile, 0, COLUMN_COUNT);

        System.arraycopy(column.columnSeaZone, 0, seaZone, 0, COLUMN_COUNT);
        System.arraycopy(column.columnCaveFloorY, 0, caveFloorY, 0, COLUMN_COUNT);
        System.arraycopy(column.columnCaveCeilingY, 0, caveCeilingY, 0, COLUMN_COUNT);
        System.arraycopy(column.columnCaveTunnels, 0, caveTunnels, 0, COLUMN_COUNT);
        System.arraycopy(column.columnCaveCaverns, 0, caveCaverns, 0, COLUMN_COUNT);
        System.arraycopy(column.columnSeaCeilingY, 0, seaCeilingY, 0, COLUMN_COUNT);
        System.arraycopy(column.columnShoreDistanceBlocks, 0, shoreDistanceBlocks, 0, COLUMN_COUNT);
        System.arraycopy(column.columnFaceDistanceBlocks, 0, faceDistanceBlocks, 0, COLUMN_COUNT);
        System.arraycopy(column.columnOverhangBlocks, 0, overhangBlocks, 0, COLUMN_COUNT);
        System.arraycopy(column.columnSeaCaves, 0, seaCaves, 0, COLUMN_COUNT);
        System.arraycopy(column.columnTideFloorY, 0, tideFloorY, 0, COLUMN_COUNT);

        this.columnMinGroundHeightBlocks = (short) column.columnMinGroundHeightBlocks;
        this.columnMaxGroundHeightBlocks = (short) column.columnMaxGroundHeightBlocks;
        this.columnTopBlocks = (short) column.columnTopBlocks;
        this.carveMinY = column.carveMinY;
        this.carveMaxY = column.carveMaxY;
        this.hasSeaFeatures = column.hasSeaFeatures;
        this.veinMinY = column.veinMinY;
        this.veinMaxY = column.veinMaxY;

        this.allOceanWater = column.allOceanWater;
        this.hasTidalColumns = column.hasTidalColumns;
        this.allFillBlocksFullGeometry = column.allFillBlocksFullGeometry;

        System.arraycopy(column.veinSeeds, 0, veinSeeds, 0, column.veinCount);
        this.veinCount = column.veinCount;

        this.valid = true;
    }

    void applyTo(TerrainColumnAsyncContainer column) {

        column.biomeID = biomeID;

        for (int i = 0; i < COLUMN_COUNT; i++)
            column.groundHeightBlocks[i] = groundHeightBlocks[i];

        System.arraycopy(topBlockID, 0, column.columnTopBlockID, 0, COLUMN_COUNT);
        System.arraycopy(fillBlockID, 0, column.columnFillBlockID, 0, COLUMN_COUNT);
        System.arraycopy(rockBlockID, 0, column.columnRockBlockID, 0, COLUMN_COUNT);
        System.arraycopy(oceanWater, 0, column.columnOceanWater, 0, COLUMN_COUNT);
        System.arraycopy(lakeLevelBlocks, 0, column.columnLakeLevelBlocks, 0, COLUMN_COUNT);
        System.arraycopy(groundMask, 0, column.columnGroundMask, 0, COLUMN_COUNT);
        System.arraycopy(capMask, 0, column.columnCapMask, 0, COLUMN_COUNT);
        System.arraycopy(profile, 0, column.columnProfile, 0, COLUMN_COUNT);

        System.arraycopy(seaZone, 0, column.columnSeaZone, 0, COLUMN_COUNT);
        System.arraycopy(caveFloorY, 0, column.columnCaveFloorY, 0, COLUMN_COUNT);
        System.arraycopy(caveCeilingY, 0, column.columnCaveCeilingY, 0, COLUMN_COUNT);
        System.arraycopy(caveTunnels, 0, column.columnCaveTunnels, 0, COLUMN_COUNT);
        System.arraycopy(caveCaverns, 0, column.columnCaveCaverns, 0, COLUMN_COUNT);
        System.arraycopy(seaCeilingY, 0, column.columnSeaCeilingY, 0, COLUMN_COUNT);
        System.arraycopy(shoreDistanceBlocks, 0, column.columnShoreDistanceBlocks, 0, COLUMN_COUNT);
        System.arraycopy(faceDistanceBlocks, 0, column.columnFaceDistanceBlocks, 0, COLUMN_COUNT);
        System.arraycopy(overhangBlocks, 0, column.columnOverhangBlocks, 0, COLUMN_COUNT);
        System.arraycopy(seaCaves, 0, column.columnSeaCaves, 0, COLUMN_COUNT);
        System.arraycopy(tideFloorY, 0, column.columnTideFloorY, 0, COLUMN_COUNT);

        column.columnMinGroundHeightBlocks = columnMinGroundHeightBlocks;
        column.columnMaxGroundHeightBlocks = columnMaxGroundHeightBlocks;
        column.columnTopBlocks = columnTopBlocks;
        column.carveMinY = carveMinY;
        column.carveMaxY = carveMaxY;
        column.hasSeaFeatures = hasSeaFeatures;
        column.veinMinY = veinMinY;
        column.veinMaxY = veinMaxY;

        column.allOceanWater = allOceanWater;
        column.hasTidalColumns = hasTidalColumns;
        column.allFillBlocksFullGeometry = allFillBlocksFullGeometry;

        System.arraycopy(veinSeeds, 0, column.veinSeeds, 0, veinCount);
        column.veinCount = veinCount;
    }

    public void invalidate() {
        this.valid = false;
    }

    public boolean isValidFor(long chunkCoordinate) {
        return valid && cachedChunkCoordinate == chunkCoordinate;
    }

    // Accessible \\

    public int getGroundHeightBlocks(int columnIndex) {
        return groundHeightBlocks[columnIndex];
    }

    public boolean hasOceanWater(int columnIndex) {
        return oceanWater[columnIndex];
    }

    // The lowest cell the tide re-levels in this column, past the tide band where it reaches none
    public int getTideFloorY(int columnIndex) {
        return tideFloorY[columnIndex];
    }

    public boolean hasTidalColumns() {
        return hasTidalColumns;
    }
}
