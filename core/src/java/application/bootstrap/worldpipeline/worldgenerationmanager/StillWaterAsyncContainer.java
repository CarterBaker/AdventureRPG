package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class StillWaterAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for resolving the level of lakes that stand
     * relative to their host: the biome field at a lake's heart and a small
     * ring of memos holding each lake's level, since every macro sample and
     * every point the surface is read at around one lake asks for the same
     * level. Every memo carries the world and biome revision it was read
     * under.
     */

    static final int LEVEL_CACHE_SIZE = EngineSetting.LAKE_LEVEL_CACHE_SIZE;

    // Heart
    BiomeBlendStruct blend;

    // Lake Levels
    long[] lakeHashes;
    WorldHandle[] lakeWorlds;
    int[] lakeRevisions;
    int[] lakeLevels;
    int lakeNext;

    @Override
    protected void create() {

        this.blend = new BiomeBlendStruct();

        this.lakeHashes = new long[LEVEL_CACHE_SIZE];
        this.lakeWorlds = new WorldHandle[LEVEL_CACHE_SIZE];
        this.lakeRevisions = new int[LEVEL_CACHE_SIZE];
        this.lakeLevels = new int[LEVEL_CACHE_SIZE];
    }
}
