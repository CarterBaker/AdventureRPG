package application.bootstrap.worldpipeline.layoutmanager;

import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import application.bootstrap.worldpipeline.util.FrameTransformStruct;
import engine.root.AsyncContainerPackage;

public class LayoutQueryAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for laying, sampling or claiming against a layout:
     * where the point being judged stands against one of its centrelines.
     */

    // Query
    RoadQueryStruct query;
    FrameTransformStruct transform;

    @Override
    protected void create() {
        this.query = new RoadQueryStruct();
        this.transform = new FrameTransformStruct();
    }
}
