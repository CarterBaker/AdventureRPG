package application.bootstrap.worldpipeline.roadmanager;

import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import engine.root.AsyncContainerPackage;

public class RoadStampAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for laying one road into one chunk: where the
     * column being laid stands against the road.
     */

    // Query
    RoadQueryStruct query;

    @Override
    protected void create() {
        this.query = new RoadQueryStruct();
    }
}
