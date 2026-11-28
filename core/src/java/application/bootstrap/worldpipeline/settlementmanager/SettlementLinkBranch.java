package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import application.bootstrap.worldpipeline.layout.LayoutDraftStruct;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layout.LayoutRandomStruct;
import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import application.bootstrap.worldpipeline.roadmanager.RoadManager;
import application.bootstrap.worldpipeline.settlement.SettlementPlanStruct;
import application.bootstrap.worldpipeline.util.FrameTransformStruct;
import application.bootstrap.worldpipeline.util.RoadPathUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class SettlementLinkBranch extends BranchPackage {

    /*
     * Async — plans the road joining two neighbouring settlements, as a layout
     * of its own measured around the first. It sets out from whichever exit
     * of the first faces the second best and arrives at whichever exit of
     * the second faces back best, bending across the land between, its ends
     * pinned to the heights of the streets it joins so it meets them level.
     * Two settlements that both link by road are joined by their
     * architecture's road, any other pair by its path. Along the way side
     * trails may branch off to a lone farm or tower, or simply run out.
     */

    // Internal
    private RoadManager roadManager;
    private LayoutManager layoutManager;
    private SettlementTrailBranch settlementTrailBranch;
    private SettlementPlanAsyncContainer planContainer;

    // Base \\

    @Override
    protected void create() {
        this.planContainer = create(SettlementPlanAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.roadManager = get(RoadManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.settlementTrailBranch = get(SettlementTrailBranch.class);
    }

    // Plan \\

    LayoutPlanStruct plan(WorldHandle worldHandle, SettlementPlanStruct from, SettlementPlanStruct to, long seed) {

        if (from.getExitCount() == 0 || to.getExitCount() == 0)
            return null;

        SettlementPlanAsyncContainer scratch = planContainer.getInstance();

        try {

            LayoutRandomStruct random = new LayoutRandomStruct(seed);
            double fromX = from.getLayout().getReferenceX();
            double fromZ = from.getLayout().getReferenceZ();
            FrameTransformStruct toFrame = scratch.transform;
            FrameTransformStruct fromFrame = scratch.identity;

            toFrame.setFrames(
                    worldHandle, to.getLayout().getReferenceX(), to.getLayout().getReferenceZ(), fromX, fromZ);
            fromFrame.setFrames(worldHandle, fromX, fromZ, fromX, fromZ);

            int fromExit = findExit(from, fromFrame,
                    toFrame.mapX(to.getLayout().getReferenceX(), to.getLayout().getReferenceZ()),
                    toFrame.mapZ(to.getLayout().getReferenceX(), to.getLayout().getReferenceZ()));
            int toExit = findExit(to, toFrame, fromX, fromZ);

            ArchitectureHandle architectureHandle = from.getSite().getArchitectureHandle();
            boolean byRoad = from.getSite().getSettlementHandle().getLink() == LayoutRoadKind.ROAD
                    && to.getSite().getSettlementHandle().getLink() == LayoutRoadKind.ROAD;

            RoadPathUtility.buildCurve(
                    from.getExitX(fromExit), from.getExitZ(fromExit),
                    toFrame.mapX(to.getExitX(toExit), to.getExitZ(toExit)),
                    toFrame.mapZ(to.getExitX(toExit), to.getExitZ(toExit)),
                    EngineSetting.SETTLEMENT_LINK_BEND, random.nextSeed(), scratch.pointX, scratch.pointZ);

            RoadPathStruct road = roadManager.planPath(
                    worldHandle, architectureHandle.getRoad(byRoad ? LayoutRoadKind.ROAD : LayoutRoadKind.PATH),
                    scratch.pointX, scratch.pointZ, from.getExitY(fromExit), to.getExitY(toExit));

            LayoutDraftStruct draft = new LayoutDraftStruct(worldHandle, fromX, fromZ, architectureHandle);

            settlementTrailBranch.addRoad(draft, road);
            laySideTrails(draft, scratch, architectureHandle, random, road);

            return layoutManager.seal(draft);
        } finally {
            scratch.reset();
        }
    }

    // The exit of a settlement, carried into the frame the road is drawn in, facing a target point best
    private int findExit(SettlementPlanStruct plan, FrameTransformStruct frame, double targetX, double targetZ) {

        int best = 0;
        double bestAlignment = -Double.MAX_VALUE;

        for (int i = 0; i < plan.getExitCount(); i++) {

            double towardX = targetX - frame.mapX(plan.getExitX(i), plan.getExitZ(i));
            double towardZ = targetZ - frame.mapZ(plan.getExitX(i), plan.getExitZ(i));
            double length = Math.max(EngineSetting.DIVISION_EPSILON,
                    Math.sqrt(towardX * towardX + towardZ * towardZ));
            double headingX = frame.turnX(plan.getExitHeadingX(i), plan.getExitHeadingZ(i));
            double headingZ = frame.turnZ(plan.getExitHeadingX(i), plan.getExitHeadingZ(i));
            double alignment = (headingX * towardX + headingZ * towardZ) / length;

            if (alignment <= bestAlignment)
                continue;

            bestAlignment = alignment;
            best = i;
        }

        return best;
    }

    // Trails branching off the road to either side, each tilted a little from square
    private void laySideTrails(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            ArchitectureHandle architectureHandle,
            LayoutRandomStruct random,
            RoadPathStruct road) {

        RoadQueryStruct query = scratch.query;
        int count = random.nextInt(0, EngineSetting.SETTLEMENT_LINK_MAX_TRAILS);

        for (int i = 0; i < count; i++) {

            float fraction = random.nextRange(
                    EngineSetting.SETTLEMENT_LINK_TRAIL_START_FRACTION,
                    EngineSetting.SETTLEMENT_LINK_TRAIL_END_FRACTION);

            RoadPathUtility.sampleAlong(road, road.getLength() * fraction, query);

            int side = random.next() < 0.5f ? -1 : 1;
            double tilt = random.nextSigned() * EngineSetting.SETTLEMENT_BRANCH_TILT_RADIANS;
            double normalX = -query.getHeadingZ() * side;
            double normalZ = query.getHeadingX() * side;
            double startX = query.getCenterX();
            double startZ = query.getCenterZ();
            float startY = query.getSurfaceY();

            settlementTrailBranch.layTrail(
                    draft, scratch, architectureHandle, random, startX, startZ, startY,
                    RoadPathUtility.turnHeadingX(normalX, normalZ, tilt),
                    RoadPathUtility.turnHeadingZ(normalX, normalZ, tilt),
                    random.nextInt(EngineSetting.SETTLEMENT_LINK_TRAIL_MIN_BLOCKS,
                            EngineSetting.SETTLEMENT_LINK_TRAIL_MAX_BLOCKS),
                    EngineSetting.SETTLEMENT_LINK_OUTPOST_CHANCE);
        }
    }
}
