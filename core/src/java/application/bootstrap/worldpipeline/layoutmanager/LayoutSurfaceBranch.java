package application.bootstrap.worldpipeline.layoutmanager;

import application.bootstrap.worldpipeline.architecture.ArchitectureWallStruct;
import application.bootstrap.worldpipeline.layout.LayoutLotStruct;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layout.LayoutSurfaceKind;
import application.bootstrap.worldpipeline.layout.LayoutSurfaceStruct;
import application.bootstrap.worldpipeline.layout.LayoutWallStruct;
import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import application.bootstrap.worldpipeline.road.RoadSpanType;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.util.RoadPathUtility;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class LayoutSurfaceBranch extends BranchPackage {

    /*
     * Async — what planned layouts show at a point, and what ground they
     * claim. Seen from afar a structure rises to the top of its footprint's
     * column in that column's colors, a wall to its capped top, a bridge to
     * its deck, and a road on the land only recolors it; a tolerance widens
     * every road, wall and lot so the coarse lattice of distant terrain still
     * catches them. A point is claimed when a lot, a road with its shoulder
     * or a wall comes within a margin of it, so trees and self-placing
     * structures stay off a settlement's ground.
     */

    // Internal
    private LayoutQueryAsyncContainer queryContainer;

    // Base \\

    @Override
    protected void create() {
        this.queryContainer = create(LayoutQueryAsyncContainer.class);
    }

    // Surface \\

    void sampleSurface(
            WorldHandle worldHandle,
            ObjectArrayList<LayoutPlanStruct> plans,
            double worldX,
            double worldZ,
            double toleranceBlocks,
            LayoutSurfaceStruct out) {

        out.clear();

        LayoutQueryAsyncContainer scratch = queryContainer.getInstance();
        RoadQueryStruct query = scratch.query;

        for (int p = 0; p < plans.size(); p++) {

            LayoutPlanStruct plan = plans.get(p);
            double x = WorldWrapUtility.unwrapBlockX(worldHandle, worldX, plan.getReferenceX());
            double z = WorldWrapUtility.unwrapBlockZ(worldHandle, worldZ, plan.getReferenceZ());

            if (!plan.overlaps(x - toleranceBlocks, z - toleranceBlocks, x + toleranceBlocks, z + toleranceBlocks))
                continue;

            sampleLots(plan, x, z, toleranceBlocks, out);
            sampleWalls(plan, x, z, toleranceBlocks, query, out);
            samplePaths(plan, x, z, toleranceBlocks, query, out);
        }
    }

    private void sampleLots(
            LayoutPlanStruct plan,
            double x,
            double z,
            double toleranceBlocks,
            LayoutSurfaceStruct out) {

        LayoutLotStruct[] lots = plan.getLots();

        for (int i = 0; i < lots.length; i++) {

            LayoutLotStruct lot = lots[i];

            if (!lot.covers(x, z, toleranceBlocks))
                continue;

            StructureHandle structureHandle = lot.getStructureHandle();
            double clampedX = Math.max(lot.getMinX(), Math.min(lot.getMaxX() - 1.0, x));
            double clampedZ = Math.max(lot.getMinZ(), Math.min(lot.getMaxZ() - 1.0, z));
            int column = StructurePlacementUtility.resolveFootprintColumn(
                    structureHandle, lot.getQuarterTurns(),
                    (int) (Math.floor(clampedX) - lot.getPlanX()),
                    (int) (Math.floor(clampedZ) - lot.getPlanZ()));

            if (column == EngineSetting.INDEX_NOT_FOUND)
                continue;

            int topOffsetY = structureHandle.getFootprintTopOffsetY()[column];

            if (topOffsetY == EngineSetting.STRUCTURE_FOOTPRINT_EMPTY)
                continue;

            out.offer(
                    LayoutSurfaceKind.RAISED,
                    lot.getAnchorY() + topOffsetY + 1f,
                    structureHandle.getFootprintTopColors()[column],
                    structureHandle.getFootprintSideColors()[column]);
        }
    }

    private void sampleWalls(
            LayoutPlanStruct plan,
            double x,
            double z,
            double toleranceBlocks,
            RoadQueryStruct query,
            LayoutSurfaceStruct out) {

        LayoutWallStruct[] walls = plan.getWalls();

        for (int i = 0; i < walls.length; i++) {

            ArchitectureWallStruct wall = walls[i].getWall();
            RoadPathStruct centerline = walls[i].getCenterline();
            double reach = wall.getHalfThicknessBlocks() + toleranceBlocks;

            if (!RoadPathUtility.reaches(centerline, x, z, reach)
                    || RoadPathUtility.locate(centerline, x, z, query) > reach
                    || walls[i].isGate(query.getAlong()))
                continue;

            out.offer(
                    LayoutSurfaceKind.RAISED,
                    (float) Math.floor(query.getSurfaceY()) + wall.getHeightBlocks() + 1f,
                    wall.getTopColor(),
                    wall.getSideColor());
        }
    }

    private void samplePaths(
            LayoutPlanStruct plan,
            double x,
            double z,
            double toleranceBlocks,
            RoadQueryStruct query,
            LayoutSurfaceStruct out) {

        RoadPathStruct[] paths = plan.getPaths();

        for (int i = 0; i < paths.length; i++) {

            RoadPathStruct path = paths[i];
            RoadHandle roadHandle = path.getRoadHandle();
            double reach = roadHandle.getHalfWidthBlocks() + toleranceBlocks;

            if (!RoadPathUtility.reaches(path, x, z, toleranceBlocks)
                    || RoadPathUtility.locate(path, x, z, query) > reach
                    || query.getSpan() == RoadSpanType.TUNNEL)
                continue;

            float surfaceY = (float) Math.floor(query.getSurfaceY()) + 1f;

            if (query.getSpan() == RoadSpanType.BRIDGE)
                out.offer(LayoutSurfaceKind.RAISED, surfaceY, roadHandle.getDeckColor(), roadHandle.getDeckColor());
            else
                out.offer(LayoutSurfaceKind.GROUND, surfaceY, roadHandle.getSurfaceColor(), roadHandle.getSideColor());
        }
    }

    // Claims \\

    boolean isClaimed(
            WorldHandle worldHandle,
            ObjectArrayList<LayoutPlanStruct> plans,
            double worldX,
            double worldZ,
            double marginBlocks) {

        LayoutQueryAsyncContainer scratch = queryContainer.getInstance();
        RoadQueryStruct query = scratch.query;

        for (int p = 0; p < plans.size(); p++) {

            LayoutPlanStruct plan = plans.get(p);
            double x = WorldWrapUtility.unwrapBlockX(worldHandle, worldX, plan.getReferenceX());
            double z = WorldWrapUtility.unwrapBlockZ(worldHandle, worldZ, plan.getReferenceZ());

            if (!plan.overlaps(x - marginBlocks, z - marginBlocks, x + marginBlocks, z + marginBlocks))
                continue;

            if (claimsLot(plan, x, z, marginBlocks) || claimsWall(plan, x, z, marginBlocks, query)
                    || claimsPath(plan, x, z, marginBlocks, query))
                return true;
        }

        return false;
    }

    private boolean claimsLot(LayoutPlanStruct plan, double x, double z, double marginBlocks) {

        LayoutLotStruct[] lots = plan.getLots();

        for (int i = 0; i < lots.length; i++)
            if (lots[i].covers(x, z, marginBlocks))
                return true;

        return false;
    }

    private boolean claimsWall(
            LayoutPlanStruct plan,
            double x,
            double z,
            double marginBlocks,
            RoadQueryStruct query) {

        LayoutWallStruct[] walls = plan.getWalls();

        for (int i = 0; i < walls.length; i++) {

            RoadPathStruct centerline = walls[i].getCenterline();
            double reach = walls[i].getWall().getHalfThicknessBlocks() + marginBlocks;

            if (RoadPathUtility.reaches(centerline, x, z, reach)
                    && RoadPathUtility.locate(centerline, x, z, query) <= reach)
                return true;
        }

        return false;
    }

    private boolean claimsPath(
            LayoutPlanStruct plan,
            double x,
            double z,
            double marginBlocks,
            RoadQueryStruct query) {

        RoadPathStruct[] paths = plan.getPaths();

        for (int i = 0; i < paths.length; i++) {

            RoadHandle roadHandle = paths[i].getRoadHandle();
            double reach = roadHandle.getHalfWidthBlocks() + roadHandle.getShoulderWidthBlocks() + marginBlocks;

            if (RoadPathUtility.reaches(paths[i], x, z, marginBlocks)
                    && RoadPathUtility.locate(paths[i], x, z, query) <= reach)
                return true;
        }

        return false;
    }
}
