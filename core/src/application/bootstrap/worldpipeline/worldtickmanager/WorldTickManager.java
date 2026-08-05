package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.ManagerPackage;

public class WorldTickManager extends ManagerPackage {

    /*
     * Drives the world's per-block-type tick cycle. FULL, PARTIAL, COMPLEX,
     * and LIQUID each own their own frame interval and counter rather than
     * sharing one cycle length, so a branch that must run often (liquid) and
     * branches that can run rarely are both just their own EngineSetting
     * interval. Every branch is held rather than advanced on any frame
     * WorldStreamManager reports as a player-wrap frame, so no tick ever
     * competes with that rebuild for the same frame.
     */

    // Internal
    private WorldStreamManager worldStreamManager;

    // Branches
    private FullTickBranch fullTickBranch;
    private PartialTickBranch partialTickBranch;
    private ComplexTickBranch complexTickBranch;
    private LiquidTickBranch liquidTickBranch;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.fullTickBranch = create(FullTickBranch.class);
        this.partialTickBranch = create(PartialTickBranch.class);
        this.complexTickBranch = create(ComplexTickBranch.class);
        this.liquidTickBranch = create(LiquidTickBranch.class);
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

        if (fullTickBranch.advance())
            fullTickBranch.tick();

        if (partialTickBranch.advance())
            partialTickBranch.tick();

        if (complexTickBranch.advance())
            complexTickBranch.tick();

        if (liquidTickBranch.advance())
            liquidTickBranch.tick();
    }
}