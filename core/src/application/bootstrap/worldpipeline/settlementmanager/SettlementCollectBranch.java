package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.settlement.SettlementPlanStruct;
import application.bootstrap.worldpipeline.settlement.SettlementSiteStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SettlementCollectBranch extends BranchPackage {

    /*
     * Async — gathers every settlement and every road between settlements
     * that reaches a stretch of the world, from the cells around it. A cell's
     * site is settled first and planned only when it stands near enough to
     * reach the stretch, and a road only when both its ends stand and the
     * straight run between them, widened by how far a road may wander, comes
     * near enough. Roads come before settlements, so a settlement's walls and
     * buildings stand over the roads leading in, and each in the fixed order
     * of its wrapped cell, so every caller lays and sees overlapping layouts
     * alike.
     */

    // Internal
    private SettlementCacheBranch settlementCacheBranch;
    private SettlementCollectAsyncContainer collectContainer;

    // Base \\

    @Override
    protected void create() {
        this.collectContainer = create(SettlementCollectAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.settlementCacheBranch = get(SettlementCacheBranch.class);
    }

    // Collect \\

    // Every layout reaching a rectangle of world blocks, which may lie across the world's wrap
    void collect(
            WorldHandle worldHandle,
            double minX,
            double minZ,
            double maxX,
            double maxZ,
            ObjectArrayList<LayoutPlanStruct> out) {

        out.clear();

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

    // A single point widened by a margin, gathered into the thread's own list
    ObjectArrayList<LayoutPlanStruct> collectAround(WorldHandle worldHandle, double x, double z, double margin) {

        SettlementCollectAsyncContainer scratch = collectContainer.getInstance();
        ObjectArrayList<LayoutPlanStruct> layouts = scratch.layouts;

        collect(worldHandle, x - margin, z - margin, x + margin, z + margin, layouts);

        return layouts;
    }

    // Links \\

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

        for (int cellZ = firstZ; cellZ <= lastZ; cellZ++) {
            for (int cellX = firstX; cellX <= lastX; cellX++) {

                int wrappedX = settlementCacheBranch.wrapCellX(worldHandle, cellX);
                int wrappedZ = settlementCacheBranch.wrapCellZ(worldHandle, cellZ);

                for (int direction = 0; direction < EngineSetting.SETTLEMENT_LINK_DIRECTIONS; direction++) {

                    if (!linkMayReach(worldHandle, wrappedX, wrappedZ, direction, minX, minZ, maxX, maxZ))
                        continue;

                    LayoutPlanStruct link = settlementCacheBranch.getLink(worldHandle, wrappedX, wrappedZ, direction);

                    if (link != null && reaches(worldHandle, link, minX, minZ, maxX, maxZ))
                        scratch.order2Layout.put(
                                ((long) wrappedZ * countX + wrappedX) * EngineSetting.SETTLEMENT_LINK_DIRECTIONS
                                        + direction,
                                link);
                }
            }
        }
    }

    // True when both ends of a road stand and the straight run between them, widened, comes near the rectangle
    private boolean linkMayReach(
            WorldHandle worldHandle,
            int cellX,
            int cellZ,
            int direction,
            double minX,
            double minZ,
            double maxX,
            double maxZ) {

        SettlementSiteStruct from = settlementCacheBranch.getSite(worldHandle, cellX, cellZ);

        if (from == null)
            return false;

        SettlementSiteStruct to = settlementCacheBranch.getSite(
                worldHandle,
                settlementCacheBranch.wrapCellX(worldHandle,
                        cellX + (direction == EngineSetting.SETTLEMENT_LINK_EAST ? 1 : 0)),
                settlementCacheBranch.wrapCellZ(worldHandle,
                        cellZ + (direction == EngineSetting.SETTLEMENT_LINK_SOUTH ? 1 : 0)));

        if (to == null)
            return false;

        double centerX = (minX + maxX) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;
        double fromX = WorldWrapUtility.unwrapBlockX(worldHandle, from.getCenterX(), centerX);
        double fromZ = WorldWrapUtility.unwrapBlockZ(worldHandle, from.getCenterZ(), centerZ);
        double toX = WorldWrapUtility.unwrapBlockX(worldHandle, to.getCenterX(), fromX);
        double toZ = WorldWrapUtility.unwrapBlockZ(worldHandle, to.getCenterZ(), fromZ);
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
        long settlementOrder = (long) countX * countZ * EngineSetting.SETTLEMENT_LINK_DIRECTIONS;

        for (int cellZ = firstZ; cellZ <= lastZ; cellZ++) {
            for (int cellX = firstX; cellX <= lastX; cellX++) {

                int wrappedX = settlementCacheBranch.wrapCellX(worldHandle, cellX);
                int wrappedZ = settlementCacheBranch.wrapCellZ(worldHandle, cellZ);
                SettlementSiteStruct site = settlementCacheBranch.getSite(worldHandle, wrappedX, wrappedZ);

                if (site == null || !siteMayReach(worldHandle, site, minX, minZ, maxX, maxZ))
                    continue;

                SettlementPlanStruct plan = settlementCacheBranch.getPlan(worldHandle, wrappedX, wrappedZ);

                if (plan != null && reaches(worldHandle, plan.getLayout(), minX, minZ, maxX, maxZ))
                    scratch.order2Layout.put(settlementOrder + (long) wrappedZ * countX + wrappedX, plan.getLayout());
            }
        }
    }

    private boolean siteMayReach(
            WorldHandle worldHandle,
            SettlementSiteStruct site,
            double minX,
            double minZ,
            double maxX,
            double maxZ) {

        double reach = site.getSettlementHandle().getReachBlocks();
        double centerX = WorldWrapUtility.unwrapBlockX(worldHandle, site.getCenterX(), (minX + maxX) * 0.5);
        double centerZ = WorldWrapUtility.unwrapBlockZ(worldHandle, site.getCenterZ(), (minZ + maxZ) * 0.5);

        return centerX + reach >= minX && centerX - reach <= maxX
                && centerZ + reach >= minZ && centerZ - reach <= maxZ;
    }

    // True when a layout's bounds overlap the rectangle, brought into the layout's own frame
    private boolean reaches(
            WorldHandle worldHandle,
            LayoutPlanStruct layout,
            double minX,
            double minZ,
            double maxX,
            double maxZ) {

        double rectMinX = WorldWrapUtility.unwrapBlockX(worldHandle, minX, layout.getReferenceX());
        double rectMinZ = WorldWrapUtility.unwrapBlockZ(worldHandle, minZ, layout.getReferenceZ());

        return layout.overlaps(rectMinX, rectMinZ, rectMinX + (maxX - minX), rectMinZ + (maxZ - minZ));
    }
}
