package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.roadmanager.RoadManager;
import application.bootstrap.worldpipeline.settlement.SettlementPlanStruct;
import application.bootstrap.worldpipeline.settlement.SettlementSiteStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

class SettlementCacheBranch extends BranchPackage {

    /*
     * Async — every settlement cell settled so far, the least recently used
     * let go once the cache is full, so a site, a plan or a road is worked
     * out once however many chunks, macros and map tiles ask for it. The
     * work itself runs outside the lock, on whatever thread first asks;
     * when two threads race to the same answer the first one stored wins,
     * and both are the same since every answer is a pure function of the
     * world. A change to hand placements, to any biome or to any settlement
     * type raises the revision, so every cell settled before it is settled
     * again. A globe's cells are whole cells of each cube face in canonical
     * coordinates; a cell counted past a face's edge is folded onto the cell
     * it truly is, and the road between two cells is owned by the one of lower
     * key, kept on the side its neighbour lies to in its own face's axes.
     */

    // Internal
    private WorldManager worldManager;
    private BiomeManager biomeManager;
    private SettlementManager settlementManager;
    private RoadManager roadManager;
    private SettlementSiteBranch settlementSiteBranch;
    private SettlementPlanBranch settlementPlanBranch;
    private SettlementLinkBranch settlementLinkBranch;

    // Cells — guarded by cellKey2Cell
    private Long2ObjectLinkedOpenHashMap<SettlementCellStruct> cellKey2Cell;

    // Base \\

    @Override
    protected void create() {
        this.cellKey2Cell = new Long2ObjectLinkedOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.roadManager = get(RoadManager.class);
        this.settlementSiteBranch = get(SettlementSiteBranch.class);
        this.settlementPlanBranch = get(SettlementPlanBranch.class);
        this.settlementLinkBranch = get(SettlementLinkBranch.class);
    }

    // Site \\

    // The settlement standing in a wrapped cell, null when none does
    SettlementSiteStruct getSite(WorldHandle worldHandle, int cellX, int cellZ) {
        return acquireCell(worldHandle, cellX, cellZ).getSite();
    }

    // Plan \\

    // The planned settlement of a wrapped cell, null when none stands there
    SettlementPlanStruct getPlan(WorldHandle worldHandle, int cellX, int cellZ) {

        SettlementCellStruct cell = acquireCell(worldHandle, cellX, cellZ);

        if (cell.getSite() == null)
            return null;

        synchronized (cellKey2Cell) {
            if (cell.isPlanned())
                return cell.getPlan();
        }

        SettlementPlanStruct plan = settlementPlanBranch.plan(worldHandle, cell.getSite());

        synchronized (cellKey2Cell) {

            if (!cell.isPlanned())
                cell.setPlan(plan);

            return cell.getPlan();
        }
    }

    // Links \\

    // The road from a wrapped cell's settlement to its neighbour east or south, null when none runs
    LayoutPlanStruct getLink(WorldHandle worldHandle, int cellX, int cellZ, int direction) {

        SettlementCellStruct cell = acquireCell(worldHandle, cellX, cellZ);

        synchronized (cellKey2Cell) {
            if (cell.isLinkResolved(direction))
                return cell.getLink(direction);
        }

        LayoutPlanStruct link = planLink(worldHandle, cell, cellX, cellZ, direction);

        synchronized (cellKey2Cell) {

            if (!cell.isLinkResolved(direction))
                cell.setLink(direction, link);

            return cell.getLink(direction);
        }
    }

    // The road between a globe cell and the neighbour on one of its sides, kept by whichever of the two has the
    // lower key on the side the other lies to in its own face's axes, null when none runs
    LayoutPlanStruct getGlobeLink(WorldHandle worldHandle, int ownerX, int ownerZ, int side, int otherX, int otherZ) {

        SettlementCellStruct cell = acquireCell(worldHandle, ownerX, ownerZ);

        synchronized (cellKey2Cell) {
            if (cell.isLinkResolved(side))
                return cell.getLink(side);
        }

        long seed = BiomeFieldUtility.hashCell(
                worldHandle.getSeed() ^ EngineSetting.SETTLEMENT_LINK_SALT,
                ownerX * EngineSetting.SETTLEMENT_LINK_SIDES + side, ownerZ);
        LayoutPlanStruct link = cell.getSite() == null
                || getSite(worldHandle, otherX, otherZ) == null
                || BiomeFieldUtility.hash01(seed) >= EngineSetting.SETTLEMENT_LINK_CHANCE
                ? null
                : settlementLinkBranch.plan(
                        worldHandle,
                        getPlan(worldHandle, ownerX, ownerZ),
                        getPlan(worldHandle, otherX, otherZ),
                        seed);

        synchronized (cellKey2Cell) {

            if (!cell.isLinkResolved(side))
                cell.setLink(side, link);

            return cell.getLink(side);
        }
    }

    private LayoutPlanStruct planLink(
            WorldHandle worldHandle,
            SettlementCellStruct cell,
            int cellX,
            int cellZ,
            int direction) {

        int neighbourX = wrapCellX(worldHandle, cellX + (direction == EngineSetting.SETTLEMENT_LINK_EAST ? 1 : 0));
        int neighbourZ = wrapCellZ(worldHandle, cellZ + (direction == EngineSetting.SETTLEMENT_LINK_SOUTH ? 1 : 0));
        long seed = BiomeFieldUtility.hashCell(
                worldHandle.getSeed() ^ EngineSetting.SETTLEMENT_LINK_SALT,
                cellX * EngineSetting.SETTLEMENT_LINK_DIRECTIONS + direction, cellZ);

        if (cell.getSite() == null
                || getSite(worldHandle, neighbourX, neighbourZ) == null
                || BiomeFieldUtility.hash01(seed) >= EngineSetting.SETTLEMENT_LINK_CHANCE)
            return null;

        return settlementLinkBranch.plan(
                worldHandle,
                getPlan(worldHandle, cellX, cellZ),
                getPlan(worldHandle, neighbourX, neighbourZ),
                seed);
    }

    // Cells \\

    private SettlementCellStruct acquireCell(WorldHandle worldHandle, int cellX, int cellZ) {

        long key = Coordinate2Long.pack(cellX, cellZ);
        long revision = resolveRevision();

        synchronized (cellKey2Cell) {

            SettlementCellStruct cell = cellKey2Cell.getAndMoveToLast(key);

            if (isCurrent(cell, worldHandle, revision))
                return cell;
        }

        SettlementCellStruct settled = new SettlementCellStruct(
                worldHandle, revision, settlementSiteBranch.resolveSite(worldHandle, cellX, cellZ));

        synchronized (cellKey2Cell) {

            SettlementCellStruct cell = cellKey2Cell.get(key);

            if (isCurrent(cell, worldHandle, revision))
                return cell;

            cellKey2Cell.putAndMoveToLast(key, settled);

            while (cellKey2Cell.size() > EngineSetting.SETTLEMENT_CACHE_CAPACITY)
                cellKey2Cell.removeFirst();

            return settled;
        }
    }

    private boolean isCurrent(SettlementCellStruct cell, WorldHandle worldHandle, long revision) {
        return cell != null && cell.getWorldHandle() == worldHandle && cell.getRevision() == revision;
    }

    // Hand placements generation stands up, settlement types, road types and biomes together, so a change to any
    // settles every cell again; the first three only ever rise, so their sum changes whenever any does
    private long resolveRevision() {
        return Coordinate2Long.pack(
                worldManager.getGenerationRevision() + settlementManager.getRevision() + roadManager.getRevision(),
                biomeManager.getRevision());
    }

    // Wrap \\

    // The cell a pair of cell indices truly names, packed: wrapped into a torus, or on a globe folded from the face
    // its x is counted from onto the face it lies on, the turns from that face's axes into the cell's left in fold
    long canonicalizeCell(WorldHandle worldHandle, int cellX, int cellZ, GlobeFrameStruct fold) {

        if (!worldHandle.isGlobe()) {
            fold.setFold(0, 0.0, 0.0, 0);
            return Coordinate2Long.pack(wrapCellX(worldHandle, cellX), wrapCellZ(worldHandle, cellZ));
        }

        int span = resolveCellsPerFace(worldHandle);
        int face = GlobeUtility.faceOf(cellX, span);

        GlobeUtility.foldPoint(
                span, face,
                2.0 * (cellX - GlobeUtility.faceOriginX(face, span)) + 1.0 - span, 2.0 * cellZ + 1.0 - span,
                0.0, 0.0, 0, fold);

        return Coordinate2Long.pack(
                (int) GlobeUtility.faceOriginX(fold.getFace(), span)
                        + (int) Math.round((fold.getFoldA() - 1.0 + span) * 0.5),
                (int) Math.round((fold.getFoldB() - 1.0 + span) * 0.5));
    }

    int resolveCellsPerFace(WorldHandle worldHandle) {
        return GlobeUtility.getFaceBlocks(worldHandle) / EngineSetting.SETTLEMENT_CELL_SIZE_BLOCKS;
    }

    int wrapCellX(WorldHandle worldHandle, int cellX) {
        return Math.floorMod(cellX, resolveCellCountX(worldHandle));
    }

    int wrapCellZ(WorldHandle worldHandle, int cellZ) {
        return Math.floorMod(cellZ, resolveCellCountZ(worldHandle));
    }

    // The span of cell x a world's cells number across — on a globe every face's stride, gaps and all
    int resolveCellCountX(WorldHandle worldHandle) {

        if (worldHandle.isGlobe())
            return resolveCellsPerFace(worldHandle) * EngineSetting.GLOBE_FACE_STRIDE_SPANS
                    * EngineSetting.GLOBE_FACE_COUNT;

        return worldHandle.getWorldScale().x / EngineSetting.SETTLEMENT_CELL_SIZE_BLOCKS;
    }

    int resolveCellCountZ(WorldHandle worldHandle) {

        if (worldHandle.isGlobe())
            return resolveCellsPerFace(worldHandle);

        return worldHandle.getWorldScale().y / EngineSetting.SETTLEMENT_CELL_SIZE_BLOCKS;
    }
}
