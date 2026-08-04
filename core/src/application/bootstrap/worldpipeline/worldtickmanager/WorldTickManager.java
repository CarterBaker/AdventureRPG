package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;

public class WorldTickManager extends ManagerPackage {

    /*
     * Drives the world's per-block-type tick cycle. Every frame advances an
     * internal counter and dispatches to exactly one branch — FULL, PARTIAL,
     * COMPLEX, or LIQUID — once its scheduled offset within the cycle is
     * reached, so no two branches ever tick on the same frame; the cycle
     * spans EngineSetting.WORLD_TICK_CYCLE_FRAMES frames, giving each block
     * type one tick per full cycle. On any frame WorldStreamManager reports
     * as a player-wrap frame the counter is held rather than advanced, so a
     * block tick never competes with that rebuild for the same frame.
     */

    // Internal
    private WorldStreamManager worldStreamManager;

    // Branches
    private FullTickBranch fullTickBranch;
    private PartialTickBranch partialTickBranch;
    private ComplexTickBranch complexTickBranch;
    private LiquidTickBranch liquidTickBranch;

    // Settings
    private int intervalFrames;
    private int cycleFrames;

    // State
    private int frameCounter;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.fullTickBranch = create(FullTickBranch.class);
        this.partialTickBranch = create(PartialTickBranch.class);
        this.complexTickBranch = create(ComplexTickBranch.class);
        this.liquidTickBranch = create(LiquidTickBranch.class);

        // Settings
        this.intervalFrames = EngineSetting.WORLD_TICK_INTERVAL_FRAMES;
        this.cycleFrames = EngineSetting.WORLD_TICK_CYCLE_FRAMES;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Tick Dispatch \\

    @Override
    protected void update() {

        if (worldStreamManager.isWrappingPlayer())
            return;

        int slot = frameCounter % cycleFrames;

        if (slot == 0)
            fullTickBranch.tick(frameCounter);
        else if (slot == intervalFrames)
            partialTickBranch.tick(frameCounter);
        else if (slot == intervalFrames * 2)
            complexTickBranch.tick(frameCounter);
        else if (slot == intervalFrames * 3)
            liquidTickBranch.tick(frameCounter);

        frameCounter++;
    }
}