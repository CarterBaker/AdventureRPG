package application.bootstrap.worldpipeline.biomemanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.ProbablePatchStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class BiomeFieldAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one biome field evaluation — the four map
     * samples reconstructed around a position, the distinct biomes they
     * resolve to, the unwarped position and map the probable biome chains
     * are walked at with the patch each step resolves, and a spare blend used by the
     * chunk-granularity getBiome() query.
     * World generation evaluates the field several times per chunk on
     * whichever worker thread owns that chunk, so holding these here, along
     * with a per-thread memo of map color to biome, keeps the whole path
     * allocation-free without any locking.
     */

    int[] mapPixelX;
    int[] mapPixelZ;
    float[] mapWeights;

    BiomeHandle[] mapBiomes;
    float[] mapBiomeWeights;

    long probableSeed;
    double probablePixelX;
    double probablePixelZ;
    int mapWidth;
    int mapHeight;
    ProbablePatchStruct probablePatch;

    BiomeBlendStruct queryBlend;

    Int2ObjectOpenHashMap<BiomeHandle> color2BiomeHandle;
    Object colorIndexStamp;

    @Override
    protected void create() {
        this.mapPixelX = new int[BiomeFieldUtility.MAP_SAMPLE_COUNT];
        this.mapPixelZ = new int[BiomeFieldUtility.MAP_SAMPLE_COUNT];
        this.mapWeights = new float[BiomeFieldUtility.MAP_SAMPLE_COUNT];
        this.mapBiomes = new BiomeHandle[BiomeFieldUtility.MAP_SAMPLE_COUNT];
        this.mapBiomeWeights = new float[BiomeFieldUtility.MAP_SAMPLE_COUNT];
        this.probablePatch = new ProbablePatchStruct();
        this.queryBlend = new BiomeBlendStruct();
        this.color2BiomeHandle = new Int2ObjectOpenHashMap<>();
    }
}