package application.bootstrap.worldpipeline.rivermanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RiverPlanAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for planning one reach: the surface sample the
     * land is read through, the centreline as it is traced, smoothed, wound
     * and resampled, flipping between two buffers, and per final point the
     * ground, whether the sea or still water reaches it, the still water's
     * level and the biome whose river settings apply there, with the table
     * of distinct biomes the reach passes through. Filled and read by
     * RiverPlanBranch, so a plan allocates only the reach it hands back.
     */

    // Surface
    TerrainSurfaceSampleStruct sample;

    // Centreline — offsets from the reach's origin
    DoubleArrayList lineX;
    DoubleArrayList lineZ;
    DoubleArrayList swapX;
    DoubleArrayList swapZ;

    // Trace
    float reliefSum;
    int reliefCount;

    // Points
    FloatArrayList ground;
    BooleanArrayList sea;
    IntArrayList lakeLevels;
    ObjectArrayList<BiomeHandle> pointBiomes;
    ObjectArrayList<BiomeHandle> biomeTable;

    @Override
    protected void create() {

        // Surface
        this.sample = new TerrainSurfaceSampleStruct();

        // Centreline
        this.lineX = new DoubleArrayList();
        this.lineZ = new DoubleArrayList();
        this.swapX = new DoubleArrayList();
        this.swapZ = new DoubleArrayList();

        // Points
        this.ground = new FloatArrayList();
        this.sea = new BooleanArrayList();
        this.lakeLevels = new IntArrayList();
        this.pointBiomes = new ObjectArrayList<>();
        this.biomeTable = new ObjectArrayList<>();
    }

    @Override
    public void reset() {
        sample.getBlend().reset();
        lineX.clear();
        lineZ.clear();
        swapX.clear();
        swapZ.clear();
        reliefSum = 0f;
        reliefCount = 0;
        ground.clear();
        sea.clear();
        lakeLevels.clear();
        pointBiomes.clear();
        biomeTable.clear();
    }

    // Swaps the working centreline with its spare buffer, leaving the spare empty
    void swapLine() {

        DoubleArrayList previousX = lineX;
        DoubleArrayList previousZ = lineZ;

        lineX = swapX;
        lineZ = swapZ;
        swapX = previousX;
        swapZ = previousZ;
        swapX.clear();
        swapZ.clear();
    }
}
