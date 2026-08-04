package application.bootstrap.worldpipeline.worldtickmanager;

import engine.root.BranchPackage;

public class LiquidTickBranch extends BranchPackage {

    /*
     * Dedicated execution path LIQUID-geometry blocks tick through.
     * Scheduling lives entirely in WorldTickManager — this branch only
     * fires when its slot comes up. Stub implementation, to be filled out
     * with actual per-block tick logic.
     */

    public void tick(int frameCount) {
        timeStampDebug("LIQUID branch ticking on frame " + frameCount);
    }
}