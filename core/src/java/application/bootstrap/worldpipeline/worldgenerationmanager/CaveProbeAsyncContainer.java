package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.bootstrap.worldpipeline.util.TerrainFeatureStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CaveProbeAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for reading caves away from any chunk being built:
     * the biome field and ground at a single point, the frame and lattice
     * down a single column, and two small rings of memos. A lake cell keeps
     * what the ground holds around its basin, and a region cell keeps the
     * surface biome whose cave biomes line it, since neighbouring chunks on
     * the same worker ask the same cells over and over. Every memo carries
     * the world and biome revision it was read under.
     */

    static final int LAKE_CACHE_SIZE = EngineSetting.CAVE_LAKE_PROBE_CACHE_SIZE;
    static final int REGION_CACHE_SIZE = EngineSetting.CAVE_REGION_CACHE_SIZE;
    static final int COLUMN_LATTICE_ROWS = EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE
            / TerrainCarveUtility.LATTICE_STEP + 2;

    // Point
    BiomeBlendStruct blend;
    TerrainFeatureStruct features;
    CaveSiteProbeStruct point;

    // Column
    CaveFrameStruct frame;
    float[] lattice;

    // Lake Cells
    IntArrayList lakeCellsX;
    IntArrayList lakeCellsZ;
    long[] lakeCellKeys;
    WorldHandle[] lakeCellWorlds;
    int[] lakeCellRevisions;
    boolean[] lakeCellDry;
    int[] lakeCellGround;
    float[] lakeCellLakes;
    int[] lakeCellFloor;
    int lakeCellNext;

    // Region Cells
    long[] regionCellKeys;
    WorldHandle[] regionCellWorlds;
    int[] regionCellRevisions;
    TerrainSurfaceProfileStruct[] regionCellProfiles;
    int regionCellNext;

    @Override
    protected void create() {

        this.blend = new BiomeBlendStruct();
        this.features = new TerrainFeatureStruct();
        this.point = new CaveSiteProbeStruct();

        this.frame = new CaveFrameStruct();
        this.lattice = new float[TerrainCarveUtility.COLUMN_LATTICE_SIDE * TerrainCarveUtility.COLUMN_LATTICE_SIDE
                * COLUMN_LATTICE_ROWS * TerrainCarveUtility.CAVE_CHANNELS];

        this.lakeCellsX = new IntArrayList();
        this.lakeCellsZ = new IntArrayList();
        this.lakeCellKeys = new long[LAKE_CACHE_SIZE];
        this.lakeCellWorlds = new WorldHandle[LAKE_CACHE_SIZE];
        this.lakeCellRevisions = new int[LAKE_CACHE_SIZE];
        this.lakeCellDry = new boolean[LAKE_CACHE_SIZE];
        this.lakeCellGround = new int[LAKE_CACHE_SIZE];
        this.lakeCellLakes = new float[LAKE_CACHE_SIZE];
        this.lakeCellFloor = new int[LAKE_CACHE_SIZE];

        this.regionCellKeys = new long[REGION_CACHE_SIZE];
        this.regionCellWorlds = new WorldHandle[REGION_CACHE_SIZE];
        this.regionCellRevisions = new int[REGION_CACHE_SIZE];
        this.regionCellProfiles = new TerrainSurfaceProfileStruct[REGION_CACHE_SIZE];
    }
}
