package application.bootstrap.worldpipeline.roadmanager;

import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class RoadPlanAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for planning one road: the surface sample the land
     * is read through, and per centreline point the ground, whether water
     * stands over it, the height the road would like to run at there and
     * the heights it is smoothed into. Filled and read by RoadPlanBranch, so
     * a plan allocates only the road it hands back.
     */

    // Surface
    TerrainSurfaceSampleStruct sample;

    // Points
    FloatArrayList ground;
    BooleanArrayList wet;
    FloatArrayList target;
    FloatArrayList smoothed;

    @Override
    protected void create() {
        this.sample = new TerrainSurfaceSampleStruct();
        this.ground = new FloatArrayList();
        this.wet = new BooleanArrayList();
        this.target = new FloatArrayList();
        this.smoothed = new FloatArrayList();
    }

    @Override
    public void reset() {
        sample.getBlend().reset();
        ground.clear();
        wet.clear();
        target.clear();
        smoothed.clear();
    }
}
