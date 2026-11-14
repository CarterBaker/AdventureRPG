package application.bootstrap.worldpipeline.layoutmanager;

import application.bootstrap.worldpipeline.layout.LayoutDraftStruct;
import application.bootstrap.worldpipeline.layout.LayoutLotStruct;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layout.LayoutWallStruct;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.util.RoadPathUtility;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class LayoutLotBranch extends BranchPackage {

    /*
     * Async — fits structures onto a layout while it is planned, and seals
     * the finished draft. A lot beside a road is turned so its front faces
     * the road and set back from it; a lot that spans a road, an overpass
     * or a gatehouse, is turned to run along it and stands over it. A lot
     * takes only ground it can stand on: clear of every other lot, of every
     * centreline it must keep clear of unless it spans one, dry, and level
     * enough across its corners, its floor no further from the road it
     * faces than a small step. Its floor is set level with that road, so
     * every door opens onto the street.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private LayoutLotAsyncContainer lotContainer;

    // Base \\

    @Override
    protected void create() {
        this.lotContainer = create(LayoutLotAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Placement \\

    // A structure beside a road point on one side of it, its front turned to face the road across a gap
    LayoutLotStruct placeFacing(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            double pointX,
            double pointZ,
            float roadY,
            double headingX,
            double headingZ,
            int side,
            double frontDistance) {

        double outX = -headingZ * side;
        double outZ = headingX * side;
        int quarterTurns = StructurePlacementUtility.resolveQuarterTurns(structureHandle.getFront(), -outX, -outZ);
        Direction3Vector facing = StructurePlacementUtility.rotateDirection(structureHandle.getFront(), quarterTurns);
        double reach = frontDistance + resolveFrontEdge(structureHandle, quarterTurns, facing);

        return tryLot(
                draft, structureHandle, quarterTurns,
                Math.round(pointX - facing.x * reach), Math.round(pointZ - facing.z * reach),
                roadY, true);
    }

    // A structure standing over a road point, turned so its front runs along the road
    LayoutLotStruct placeSpanning(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            double pointX,
            double pointZ,
            float roadY,
            double headingX,
            double headingZ) {

        int quarterTurns = StructurePlacementUtility.resolveQuarterTurns(
                structureHandle.getFront(), headingX, headingZ);

        return tryLot(
                draft, structureHandle, quarterTurns,
                (long) Math.floor(pointX), (long) Math.floor(pointZ), roadY, false);
    }

    // A structure stood exactly where it is asked, kept only clear of other lots
    LayoutLotStruct placeAt(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            int quarterTurns,
            long planX,
            long planZ,
            float floorY) {

        if (overlapsLots(draft, structureHandle, quarterTurns, planX, planZ))
            return null;

        return addLot(draft, structureHandle, quarterTurns, planX, planZ, floorY);
    }

    // The distance from the anchor to the edge of the footprint the front faces out of
    private int resolveFrontEdge(StructureHandle structureHandle, int quarterTurns, Direction3Vector facing) {

        if (facing.x > 0)
            return StructurePlacementUtility.resolveTurnedMaxX(structureHandle, quarterTurns) + 1;

        if (facing.x < 0)
            return -StructurePlacementUtility.resolveTurnedMinX(structureHandle, quarterTurns);

        if (facing.z > 0)
            return StructurePlacementUtility.resolveTurnedMaxZ(structureHandle, quarterTurns) + 1;

        return -StructurePlacementUtility.resolveTurnedMinZ(structureHandle, quarterTurns);
    }

    // Fit \\

    private LayoutLotStruct tryLot(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            int quarterTurns,
            long planX,
            long planZ,
            float roadY,
            boolean keepClear) {

        if (overlapsLots(draft, structureHandle, quarterTurns, planX, planZ))
            return null;

        if (keepClear && crossesObstacle(draft, structureHandle, quarterTurns, planX, planZ))
            return null;

        if (!standsOnGround(draft, structureHandle, quarterTurns, planX, planZ, roadY))
            return null;

        return addLot(draft, structureHandle, quarterTurns, planX, planZ, roadY);
    }

    private boolean overlapsLots(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            int quarterTurns,
            long planX,
            long planZ) {

        double gap = EngineSetting.LAYOUT_LOT_GAP_BLOCKS;
        double minX = planX + StructurePlacementUtility.resolveTurnedMinX(structureHandle, quarterTurns) - gap;
        double maxX = planX + StructurePlacementUtility.resolveTurnedMaxX(structureHandle, quarterTurns) + 1 + gap;
        double minZ = planZ + StructurePlacementUtility.resolveTurnedMinZ(structureHandle, quarterTurns) - gap;
        double maxZ = planZ + StructurePlacementUtility.resolveTurnedMaxZ(structureHandle, quarterTurns) + 1 + gap;

        ObjectArrayList<LayoutLotStruct> lots = draft.getLots();

        for (int i = 0; i < lots.size(); i++) {

            LayoutLotStruct lot = lots.get(i);

            if (maxX > lot.getMinX() && minX < lot.getMaxX() && maxZ > lot.getMinZ() && minZ < lot.getMaxZ())
                return true;
        }

        return false;
    }

    // True when any point of the footprint, sampled a short step apart, comes inside a centreline's clearance
    private boolean crossesObstacle(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            int quarterTurns,
            long planX,
            long planZ) {

        LayoutLotAsyncContainer scratch = lotContainer.getInstance();
        double minX = planX + StructurePlacementUtility.resolveTurnedMinX(structureHandle, quarterTurns);
        double maxX = planX + StructurePlacementUtility.resolveTurnedMaxX(structureHandle, quarterTurns) + 1;
        double minZ = planZ + StructurePlacementUtility.resolveTurnedMinZ(structureHandle, quarterTurns);
        double maxZ = planZ + StructurePlacementUtility.resolveTurnedMaxZ(structureHandle, quarterTurns) + 1;
        double step = EngineSetting.LAYOUT_LOT_SAMPLE_STEP_BLOCKS;

        for (int i = 0; i < draft.getObstacleCount(); i++) {

            RoadPathStruct obstacle = draft.getObstacle(i);
            double clearance = draft.getObstacleClearance(i);

            if (maxX + clearance < obstacle.getMinX() || minX - clearance > obstacle.getMaxX()
                    || maxZ + clearance < obstacle.getMinZ() || minZ - clearance > obstacle.getMaxZ())
                continue;

            for (double z = minZ; z <= maxZ; z = nextSample(z, maxZ, step))
                for (double x = minX; x <= maxX; x = nextSample(x, maxX, step))
                    if (RoadPathUtility.locate(obstacle, x, z, scratch.query) < clearance)
                        return true;
        }

        return false;
    }

    // Steps across a span a sample at a time, always landing on its far edge last
    private double nextSample(double value, double end, double step) {

        if (value >= end)
            return end + step;

        return Math.min(end, value + step);
    }

    // True when the ground under the footprint is dry, level enough, and within a step of the road it faces
    private boolean standsOnGround(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            int quarterTurns,
            long planX,
            long planZ,
            float roadY) {

        WorldHandle worldHandle = draft.getWorldHandle();
        int minX = StructurePlacementUtility.resolveTurnedMinX(structureHandle, quarterTurns);
        int maxX = StructurePlacementUtility.resolveTurnedMaxX(structureHandle, quarterTurns);
        int minZ = StructurePlacementUtility.resolveTurnedMinZ(structureHandle, quarterTurns);
        int maxZ = StructurePlacementUtility.resolveTurnedMaxZ(structureHandle, quarterTurns);
        int floorY = (int) Math.floor(roadY);
        int slopeLimit = structureHandle.getRules().isSlopeLimited()
                ? structureHandle.getRules().getMaxSlopeBlocks()
                : EngineSetting.LAYOUT_LOT_MAX_SLOPE_BLOCKS;

        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;

        for (int corner = 0; corner < EngineSetting.LAYOUT_LOT_GROUND_SAMPLES; corner++) {

            long x = planX + switch (corner) {
                case 0, 2 -> minX;
                case 1, 3 -> maxX;
                default -> (minX + maxX) / 2;
            };
            long z = planZ + switch (corner) {
                case 0, 1 -> minZ;
                case 2, 3 -> maxZ;
                default -> (minZ + maxZ) / 2;
            };

            int ground = probeGround(worldHandle, x, z);

            if (ground == EngineSetting.LAYOUT_GROUND_FLOODED)
                return false;

            lowest = Math.min(lowest, ground);
            highest = Math.max(highest, ground);
        }

        return highest - lowest <= slopeLimit
                && highest - floorY <= EngineSetting.LAYOUT_LOT_MAX_STEP_BLOCKS
                && floorY - lowest <= EngineSetting.LAYOUT_LOT_MAX_STEP_BLOCKS;
    }

    // The ground's top block at a column of the layout, LAYOUT_GROUND_FLOODED where water stands over it
    int probeGround(WorldHandle worldHandle, double planX, double planZ) {

        LayoutLotAsyncContainer scratch = lotContainer.getInstance();
        TerrainSurfaceSampleStruct sample = scratch.sample;

        worldGenerationManager.sampleSurface(
                worldHandle,
                WorldWrapUtility.wrapBlockX(worldHandle, planX + EngineSetting.BLOCK_CENTER_OFFSET),
                WorldWrapUtility.wrapBlockZ(worldHandle, planZ + EngineSetting.BLOCK_CENTER_OFFSET),
                sample);

        int ground = TerrainShapeUtility.finalizeGroundHeightBlocks(sample.getGroundHeightBlocks());

        scratch.reset();

        return sample.isOpenWater() || sample.isLakeWater() ? EngineSetting.LAYOUT_GROUND_FLOODED : ground;
    }

    private LayoutLotStruct addLot(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            int quarterTurns,
            long planX,
            long planZ,
            float floorY) {

        WorldHandle worldHandle = draft.getWorldHandle();
        LayoutLotStruct lot = new LayoutLotStruct(
                structureHandle,
                quarterTurns,
                WorldWrapUtility.wrapBlockX(worldHandle, planX),
                WorldWrapUtility.wrapBlockZ(worldHandle, planZ),
                (int) Math.floor(floorY) + 1 + structureHandle.getYOffsetBlocks(),
                planX,
                planZ,
                planX + StructurePlacementUtility.resolveTurnedMinX(structureHandle, quarterTurns),
                planX + StructurePlacementUtility.resolveTurnedMaxX(structureHandle, quarterTurns) + 1,
                planZ + StructurePlacementUtility.resolveTurnedMinZ(structureHandle, quarterTurns),
                planZ + StructurePlacementUtility.resolveTurnedMaxZ(structureHandle, quarterTurns) + 1);

        draft.addLot(lot);

        return lot;
    }

    // Seal \\

    LayoutPlanStruct seal(LayoutDraftStruct draft) {

        double minX = draft.getReferenceX();
        double maxX = draft.getReferenceX();
        double minZ = draft.getReferenceZ();
        double maxZ = draft.getReferenceZ();

        for (int i = 0; i < draft.getPaths().size(); i++) {

            RoadPathStruct path = draft.getPaths().get(i);

            minX = Math.min(minX, path.getMinX());
            maxX = Math.max(maxX, path.getMaxX());
            minZ = Math.min(minZ, path.getMinZ());
            maxZ = Math.max(maxZ, path.getMaxZ());
        }

        for (int i = 0; i < draft.getWalls().size(); i++) {

            LayoutWallStruct wall = draft.getWalls().get(i);
            RoadPathStruct centerline = wall.getCenterline();
            double reach = wall.getWall().getHalfThicknessBlocks() + 1.0;

            minX = Math.min(minX, centerline.getMinX() - reach);
            maxX = Math.max(maxX, centerline.getMaxX() + reach);
            minZ = Math.min(minZ, centerline.getMinZ() - reach);
            maxZ = Math.max(maxZ, centerline.getMaxZ() + reach);
        }

        for (int i = 0; i < draft.getLots().size(); i++) {

            LayoutLotStruct lot = draft.getLots().get(i);

            minX = Math.min(minX, lot.getMinX());
            maxX = Math.max(maxX, lot.getMaxX());
            minZ = Math.min(minZ, lot.getMinZ());
            maxZ = Math.max(maxZ, lot.getMaxZ());
        }

        return new LayoutPlanStruct(
                draft.getReferenceX(),
                draft.getReferenceZ(),
                draft.getPaths().toArray(new RoadPathStruct[0]),
                draft.getWalls().toArray(new LayoutWallStruct[0]),
                draft.getLots().toArray(new LayoutLotStruct[0]),
                minX, maxX, minZ, maxZ);
    }
}
