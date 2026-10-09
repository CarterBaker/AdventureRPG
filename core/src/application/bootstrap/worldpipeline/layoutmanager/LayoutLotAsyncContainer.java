package application.bootstrap.worldpipeline.layoutmanager;

import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import engine.root.AsyncContainerPackage;

public class LayoutLotAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for fitting one lot: the surface sample the ground
     * under it is read through, and where each point of it stands against a
     * centreline it must keep clear of.
     */

    // Surface
    TerrainSurfaceSampleStruct sample;

    // Query
    RoadQueryStruct query;

    @Override
    protected void create() {
        this.sample = new TerrainSurfaceSampleStruct();
        this.query = new RoadQueryStruct();
    }

    @Override
    public void reset() {
        sample.getBlend().reset();
    }
}
