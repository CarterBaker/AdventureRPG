package application.bootstrap.worldpipeline.worldtickmanager;

import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class ComplexTickBranch extends BranchPackage {

    /*
     * Dedicated execution path COMPLEX-geometry blocks tick through. Owns
     * its own frame interval and counter, independent of the other tick
     * branches. Stub implementation, to be filled out with actual per-block
     * tick logic.
     */

    // Settings
    private int intervalFrames;

    // State
    private int frameCounter;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.intervalFrames = EngineSetting.COMPLEX_TICK_INTERVAL_FRAMES;

        // State
        this.frameCounter = EngineSetting.COMPLEX_TICK_PHASE_FRAMES;
    }

    // Schedule \\

    public boolean advance() {

        frameCounter++;

        if (frameCounter < intervalFrames)
            return false;

        frameCounter = 0;
        return true;
    }

    // Tick \\

    public void tick() {

    }
}