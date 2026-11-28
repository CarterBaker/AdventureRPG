package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.util.FrameTransformStruct;
import application.bootstrap.worldpipeline.util.SurfacePointStruct;
import engine.root.AsyncContainerPackage;

public class SettlementStampAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for laying every settlement and road reaching one
     * chunk: the chunk homed where its frame lays it, and the transform that
     * carries its columns into each layout's own frame.
     */

    // Chunk
    SurfacePointStruct home;

    // Layout
    FrameTransformStruct chunkToLayout;

    @Override
    protected void create() {
        this.home = new SurfacePointStruct();
        this.chunkToLayout = new FrameTransformStruct();
    }
}
