package application.bootstrap.worldpipeline.worldtickmanager;

import engine.root.BranchPackage;

public class PartialTickBranch extends BranchPackage {

    /*
     * Dedicated execution path PARTIAL-geometry blocks tick through.
     * Scheduling lives entirely in WorldTickManager — this branch only
     * fires when its slot comes up. Stub implementation, to be filled out
     * with actual per-block tick logic.
     */

    public void tick(int frameCount) {
        timeStampDebug("PARTIAL branch ticking on frame " + frameCount);
    }
}