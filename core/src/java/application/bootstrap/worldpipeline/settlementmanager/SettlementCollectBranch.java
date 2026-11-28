package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.settlement.SettlementPlanStruct;
import application.bootstrap.worldpipeline.settlement.SettlementSiteStruct;
import application.bootstrap.worldpipeline.util.FrameTransformStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SettlementCollectBranch extends BranchPackage {

    /*
     * Async — gathers every settlement and every road between settlements
     * that reaches a stretch of the world, from the cells around it. A cell's
     * site is settled first and planned only when it stands near enough to
     * reach the stretch, and a road only when both its ends stand and the
     * straight run between them, widened by how far a road may wander, comes
     * near enough. The layouts a tool plans on the world come first, then
     * roads before settlements, so a settlement's walls and
     * buildings stand over the roads leading in, and each in the fixed order
     * of its wrapped cell, so every caller lays and sees overlapping layouts
     * alike. On a globe the cells are walked in the stretch's own face frame
     * and folded onto the cells they truly are, and every centre and bound is
     * carried between frames before it is measured.
     */

    // Internal
    private SettlementManager settlementManager;
    private SettlementCacheBranch settlementCacheBranch;
    private SettlementCollectAsyncContainer collectContainer;

    // Base \\

    @Override
    protected void create() {
        this.collectContainer = create(SettlementCollectAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.settlementManager = get(SettlementManager.class);
        this.settlementCacheBranch = get(SettlementCacheBranch.class);
    }

    // Collect \\

    // Every layout reaching a rectangle of world blocks, which may lie across the world's wrap or a globe's edges
    void collect(
            WorldHandle worldHandle,
            double minX,
            double minZ,
            double maxX,
            double maxZ,
            ObjectArrayList<LayoutPlanStruct> out) {

        out.clear();
        collectPlanned(worldHandle, out);

        int countX = settlementCacheBranch.resolveCellCountX(worldHandle);
        int countZ = settlementCacheBranch.resolveCellCountZ(worldHandle);

        if (countX <= 0 || countZ <= 0)
            return;

        SettlementCollectAsyncContainer scratch = collectContainer.getInstance();

        try {
            collectLinks(scratch, worldHandle, minX, minZ, maxX, maxZ, countX);
            collectSettlements(scratch, worldHandle, minX, minZ, maxX, maxZ, countX, countZ);
            out.addAll(scratch.order2Layout.values());
        } finally {
            scratch.reset();
        }
    }

    // Every layout a tool plans on the world, laid before anything the world grows itself
    private void collectPlanned(WorldHandle worldHandle, ObjectArrayList<LayoutPlanStruct> out) {

        LayoutPlanStruct[] planned = settlementManager.getPlannedLayouts(worldHandle);

        if (planned == null)
            return;

        for (int i = 0; i < planned.length; i++)
            out.add(planned[i]);
    }

    // A single point widened by a margin, gathered into the thread's own list
    ObjectArrayList<LayoutPlanStruct> collectAround(WorldHandle worldHandle, double x, double z, double margin) {

        SettlementCollectAsyncContainer scratch = collectContainer.getInstance();
        ObjectArrayList<LayoutPlanStruct> layouts = scratch.layouts;

        collect(worldHandle, x - margin, z - margin, x + margin, z + margin, layouts);

        return layouts;
    }

    // Links \\

    // A torus cell owns its roads east and south; a globe pair of cells is walked from either, its road owned once
    private void collectLinks(
            SettlementCollectAsyncContainer scratch,
            WorldHandle worldHandle,
            double minX,
            double minZ,
            double maxX,
            double maxZ,
            int countX) {

        int cellSize = EngineSetting.SETTLEMENT_CELL_SIZE_BLOCKS;
        int window = EngineSetting.SETTLEMENT_LINK_WINDOW_CELLS;
        int firstX = (int) Math.floorDiv((long) Math.floor(minX), cellSize) - window;
        int lastX = (int) Math.floorDiv((long) Math.floor(maxX), cellSize) + 1;
        int firstZ = (int) Math.floorDiv((long) Math.floor(minZ), cellSize) - window;
        int lastZ = (int) Math.floorDiv((long) Math.floor(maxZ), cellSize) + 1;
        boolean globe = worldHandle.isGlobe();
        int sides = globe ? EngineSetting.SETTLEMENT_LINK_SIDES : EngineSetting.SETTLEMENT_LINK_DIRECTIONS;

        for (int cellZ = firstZ; cellZ <= lastZ; cellZ++) {
            for (int cellX = firstX; cellX <= lastX; cellX++) {

                for (int direction = 0; direction < EngineSetting.SETTLEMENT_LINK_DIRECTIONS; direction++) {

                    int neighbourX = cellX + (direction == EngineSetting.SETTLEMENT_LINK_EAST ? 1 : 0);
                    int neighbourZ = cellZ + (direction == EngineSetting.SETTLEMENT_LINK_SOUTH ? 1 : 0);

                    long from = settlementCacheBranch.canonicalizeCell(worldHandle, cellX, cellZ, scratch.fold);
                    int fromSide = (direction + scratch.fold.getTurns()) & 3;
                    long to = settlementCacheBranch.canonicalizeCell(
                            worldHandle, neighbourX, neighbourZ, scratch.fold);
                    int toSide = (direction + 2 + scratch.fold.getTurns()) & 3;

                    boolean fromOwns = !globe || from < to;
                    long owner = fromOwns ? from : to;
                    long other = fromOwns ? to : from;
                    int side = globe ? (fromOwns ? fromSide : toSide) : direction;
                    int ownerX = Coordinate2Long.unpackX(owner);
                    int ownerZ = Coordinate2Long.unpackY(owner);
                    int otherX = Coordinate2Long.unpackX(other);
                    int otherZ = Coordinate2Long.unpackY(other);

                    if (!linkMayReach(scratch, worldHandle, ownerX, ownerZ, otherX, otherZ, minX, minZ, maxX, maxZ))
                        continue;

                    LayoutPlanStruct link = globe
                            ? settlementCacheBranch.getGlobeLink(worldHandle, ownerX, ownerZ, side, otherX, otherZ)
                            : settlementCacheBranch.getLink(worldHandle, ownerX, ownerZ, side);

                    if (link != null && reaches(scratch, worldHandle, link, minX, minZ, maxX, maxZ))
                        scratch.order2Layout.put(((long) ownerZ * countX + ownerX) * sides + side, link);
                }
            }
        }
    }

    // True when both ends of a road stand and the straight run between them, widened, comes near the rectangle
    private boolean linkMayReach(
            SettlementCollectAsyncContainer scratch,
            WorldHandle worldHandle,
            int fromCellX,
            int fromCellZ,
            int toCellX,
            int toCellZ,
            double minX,
            double minZ,
            double maxX,
            double maxZ) {

        SettlementSiteStruct from = settlementCacheBranch.getSite(worldHandle, fromCellX, fromCellZ);

        if (from == null)
            return false;

        SettlementSiteStruct to = settlementCacheBranch.getSite(worldHandle, toCellX, toCellZ);

        if (to == null)
            return false;

        double centerX = (minX + maxX) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;
        FrameTransformStruct transform = scratch.transform;

        transform.setFrames(worldHandle, from.getCenterX(), from.getCenterZ(), centerX, centerZ);

        double fromX = transform.mapX(from.getCenterX(), from.getCenterZ());
        double fromZ = transform.mapZ(from.getCenterX(), from.getCenterZ());

        transform.setFrames(worldHandle, to.getCenterX(), to.getCenterZ(), fromX, fromZ);

        double toX = transform.mapX(to.getCenterX(), to.getCenterZ());
        double toZ = transform.mapZ(to.getCenterX(), to.getCenterZ());
        double reach = EngineSetting.SETTLEMENT_LINK_REACH_BLOCKS;

        return Math.max(fromX, toX) + reach >= minX && Math.min(fromX, toX) - reach <= maxX
                && Math.max(fromZ, toZ) + reach >= minZ && Math.min(fromZ, toZ) - reach <= maxZ;
    }

    // Settlements \\

    private void collectSettlements(
            SettlementCollectAsyncContainer scratch,
            WorldHandle worldHandle,
            double minX,
            double minZ,
            double maxX,
            double maxZ,
            int countX,
            int countZ) {

        int cellSize = EngineSetting.SETTLEMENT_CELL_SIZE_BLOCKS;
        int reach = EngineSetting.SETTLEMENT_MAX_REACH_BLOCKS;
        int firstX = (int) Math.floorDiv((long) Math.floor(minX) - reach, cellSize);
        int lastX = (int) Math.floorDiv((long) Math.floor(maxX) + reach, cellSize);
        int firstZ = (int) Math.floorDiv((long) Math.floor(minZ) - reach, cellSize);
        int lastZ = (int) Math.floorDiv((long) Math.floor(maxZ) + reach, cellSize);
        int sides = worldHandle.isGlobe() ? EngineSetting.SETTLEMENT_LINK_SIDES : EngineSetting.SETTLEMENT_LINK_DIRECTIONS;
        long settlementOrder = (long) countX * countZ * sides;

        for (int cellZ = firstZ; cellZ <= lastZ; cellZ++) {
            for (int cellX = firstX; cellX <= lastX; cellX++) {

                long cell = settlementCacheBranch.canonicalizeCell(worldHandle, cellX, cellZ, scratch.fold);
                int wrappedX = Coordinate2Long.unpackX(cell);
                int wrappedZ = Coordinate2Long.unpackY(cell);
                SettlementSiteStruct site = settlementCacheBranch.getSite(worldHandle, wrappedX, wrappedZ);

                if (site == null || !siteMayReach(scratch, worldHandle, site, minX, minZ, maxX, maxZ))
                    continue;

                SettlementPlanStruct plan = settlementCacheBranch.getPlan(worldHandle, wrappedX, wrappedZ);

                if (plan != null && reaches(scratch, worldHandle, plan.getLayout(), minX, minZ, maxX, maxZ))
                    scratch.order2Layout.put(settlementOrder + (long) wrappedZ * countX + wrappedX, plan.getLayout());
            }
        }
    }

    private boolean siteMayReach(
            SettlementCollectAsyncContainer scratch,
            WorldHandle worldHandle,
            SettlementSiteStruct site,
            double minX,
            double minZ,
            double maxX,
            double maxZ) {

        double reach = site.getSettlementHandle().getReachBlocks();
        FrameTransformStruct transform = scratch.transform;

        transform.setFrames(
                worldHandle, site.getCenterX(), site.getCenterZ(), (minX + maxX) * 0.5, (minZ + maxZ) * 0.5);

        double centerX = transform.mapX(site.getCenterX(), site.getCenterZ());
        double centerZ = transform.mapZ(site.getCenterX(), site.getCenterZ());

        return centerX + reach >= minX && centerX - reach <= maxX
                && centerZ + reach >= minZ && centerZ - reach <= maxZ;
    }

    // True when a layout's bounds overlap the rectangle, brought into the layout's own frame
    private boolean reaches(
            SettlementCollectAsyncContainer scratch,
            WorldHandle worldHandle,
            LayoutPlanStruct layout,
            double minX,
            double minZ,
            double maxX,
            double maxZ) {

        if (!worldHandle.isGlobe()) {

            double rectMinX = WorldWrapUtility.unwrapBlockX(worldHandle, minX, layout.getReferenceX());
            double rectMinZ = WorldWrapUtility.unwrapBlockZ(worldHandle, minZ, layout.getReferenceZ());

            return layout.overlaps(rectMinX, rectMinZ, rectMinX + (maxX - minX), rectMinZ + (maxZ - minZ));
        }

        FrameTransformStruct transform = scratch.transform;

        transform.setFrames(
                worldHandle, (minX + maxX) * 0.5, (minZ + maxZ) * 0.5, layout.getReferenceX(), layout.getReferenceZ());

        double cornerAX = transform.mapX(minX, minZ);
        double cornerAZ = transform.mapZ(minX, minZ);
        double cornerBX = transform.mapX(maxX, maxZ);
        double cornerBZ = transform.mapZ(maxX, maxZ);

        return layout.overlaps(
                Math.min(cornerAX, cornerBX), Math.min(cornerAZ, cornerBZ),
                Math.max(cornerAX, cornerBX), Math.max(cornerAZ, cornerBZ));
    }
}
