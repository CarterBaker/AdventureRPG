package application.bootstrap.worldpipeline.biomemanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.ScatterLatticeStruct;
import application.bootstrap.worldpipeline.util.SurfacePointStruct;
import application.bootstrap.worldpipeline.world.GlobeTexelStruct;
import engine.root.AsyncContainerPackage;
import engine.util.mathematics.vectors.Vector3Double;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeFieldAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one biome field evaluation — the four map
     * samples reconstructed around a position, the distinct biomes they
     * resolve to, the lattice the probable biome chains are walked in at the
     * unwarped place with one chain link per depth, grown once to the deepest
     * chain and reused after, a globe place's warped direction and where it
     * falls on the images, a point for queries given in the world's own
     * coordinates, and a spare blend used by the chunk-granularity getBiome()
     * query.
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
    ScatterLatticeStruct lattice;
    ObjectArrayList<BiomeChainStruct> chain;

    Vector3Double warpedDirection;
    GlobeTexelStruct warpedTexel;
    SurfacePointStruct canonicalPoint;

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
        this.lattice = new ScatterLatticeStruct();
        this.chain = new ObjectArrayList<>();
        this.warpedDirection = new Vector3Double();
        this.warpedTexel = new GlobeTexelStruct();
        this.canonicalPoint = new SurfacePointStruct();
        this.queryBlend = new BiomeBlendStruct();
        this.color2BiomeHandle = new Int2ObjectOpenHashMap<>();
    }
}