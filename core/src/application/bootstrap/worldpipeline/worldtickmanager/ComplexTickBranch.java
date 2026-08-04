package application.bootstrap.worldpipeline.worldtickmanager;

import engine.root.BranchPackage;

public class ComplexTickBranch extends BranchPackage {

    /*
     * Dedicated execution path COMPLEX-geometry blocks tick through.
     * Scheduling lives entirely in WorldTickManager — this branch only
     * fires when its slot comes up. Stub implementation, to be filled out
     * with actual per-block tick logic.
     */

    public void tick(int frameCount) {
        timeStampDebug("COMPLEX branch ticking on frame " + frameCount);
    }
}