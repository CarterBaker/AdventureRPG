package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import application.bootstrap.worldpipeline.layout.LayoutDraftStruct;
import application.bootstrap.worldpipeline.layout.LayoutRandomStruct;
import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import application.bootstrap.worldpipeline.roadmanager.RoadManager;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structurelist.StructureListHandle;
import application.bootstrap.worldpipeline.structurelistmanager.StructureListManager;
import application.bootstrap.worldpipeline.util.RoadPathUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class SettlementTrailBranch extends BranchPackage {

    /*
     * Async — what settlement planning and the roads between settlements lay
     * alike. A road laid into a draft is also kept clear of by every lot
     * after it, by its half width, shoulder and a gap. A trail sets out from
     * a point along a heading as a bending path and either ends in an
     * outpost, a farm, tower or the like turned to face it, or simply runs
     * out as a dead end. A role draws one of the architecture's structures
     * for it, or none when the architecture fills no such role.
     */

    // Internal
    private RoadManager roadManager;
    private LayoutManager layoutManager;
    private StructureListManager structureListManager;

    // Base \\

    @Override
    protected void get() {
        this.roadManager = get(RoadManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.structureListManager = get(StructureListManager.class);
    }

    // Roads \\

    // A planned road laid into the draft, every later lot kept clear of it
    void addRoad(LayoutDraftStruct draft, RoadPathStruct path) {
        draft.addPath(path);
        draft.addObstacle(path, resolveClearance(path.getRoadHandle()));
    }

    // How near a lot may come to a road's centreline
    double resolveClearance(RoadHandle roadHandle) {
        return roadHandle.getHalfWidthBlocks() + roadHandle.getShoulderWidthBlocks()
                + EngineSetting.LAYOUT_LOT_ROAD_GAP_BLOCKS;
    }

    // Trails \\

    // A path from a point along a heading, ending in an outpost by its chance or else in nothing at all
    void layTrail(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            ArchitectureHandle architectureHandle,
            LayoutRandomStruct random,
            double startX,
            double startZ,
            float startY,
            double headingX,
            double headingZ,
            int lengthBlocks,
            float outpostChance) {

        if (lengthBlocks <= 0)
            return;

        RoadHandle pathRoad = architectureHandle.getRoad(LayoutRoadKind.PATH);

        RoadPathUtility.buildCurve(
                startX, startZ,
                startX + headingX * lengthBlocks, startZ + headingZ * lengthBlocks,
                EngineSetting.SETTLEMENT_TRAIL_BEND, random.nextSeed(),
                scratch.pointX, scratch.pointZ);

        RoadPathStruct trail = roadManager.planPath(
                draft.getWorldHandle(), pathRoad, scratch.pointX, scratch.pointZ,
                startY, EngineSetting.ROAD_HEIGHT_FREE);

        addRoad(draft, trail);

        if (random.next() >= outpostChance)
            return;

        StructureHandle outpost = drawRole(architectureHandle, EngineSetting.SETTLEMENT_ROLE_OUTPOST, random);

        if (outpost == null)
            return;

        RoadQueryStruct query = scratch.query;

        RoadPathUtility.sampleAlong(trail, trail.getLength(), query);

        layoutManager.placeLotFacing(
                draft, outpost, query.getCenterX(), query.getCenterZ(), query.getSurfaceY(),
                query.getHeadingZ(), -query.getHeadingX(), 1,
                resolveClearance(pathRoad) + EngineSetting.LAYOUT_LOT_GAP_BLOCKS);
    }

    // Roles \\

    // One of the architecture's structures for a role, null when it fills no such role
    StructureHandle drawRole(ArchitectureHandle architectureHandle, String role, LayoutRandomStruct random) {

        StructureListHandle structureList = architectureHandle.getStructureList(role);

        if (structureList == null)
            return null;

        return structureListManager.drawStructure(structureList, random.next());
    }
}
