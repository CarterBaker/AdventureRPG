package application.bootstrap.worldpipeline.rivermanager;

import application.bootstrap.worldpipeline.river.RiverGatherStruct;
import application.bootstrap.worldpipeline.river.RiverQueryStruct;
import application.bootstrap.worldpipeline.util.FrameTransformStruct;
import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import engine.root.AsyncContainerPackage;

public class RiverQueryAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for measuring columns against rivers: the reaches
     * gathered for a single point, and the place on a reach each column is
     * located against. Chunks bring their own gather, filled once per chunk,
     * and every gather clears itself before it fills. The fold steps a
     * globe's drainage cells and the transform measures places across its
     * faces.
     */

    // Point
    RiverGatherStruct pointGather;

    // Locate
    RiverQueryStruct query;

    // Globe
    GlobeFrameStruct fold;
    FrameTransformStruct transform;

    @Override
    protected void create() {
        this.pointGather = new RiverGatherStruct();
        this.query = new RiverQueryStruct();
        this.fold = new GlobeFrameStruct();
        this.transform = new FrameTransformStruct();
    }
}
