package application.bootstrap.worldpipeline.rivermanager;

import application.bootstrap.worldpipeline.river.RiverGatherStruct;
import application.bootstrap.worldpipeline.river.RiverQueryStruct;
import engine.root.AsyncContainerPackage;

public class RiverQueryAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for measuring columns against rivers: the reaches
     * gathered for a single point, and the place on a reach each column is
     * located against. Chunks bring their own gather, filled once per chunk,
     * and every gather clears itself before it fills.
     */

    // Point
    RiverGatherStruct pointGather;

    // Locate
    RiverQueryStruct query;

    @Override
    protected void create() {
        this.pointGather = new RiverGatherStruct();
        this.query = new RiverQueryStruct();
    }
}
