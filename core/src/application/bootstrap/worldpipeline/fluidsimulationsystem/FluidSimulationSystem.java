package application.bootstrap.worldpipeline.fluidsimulationsystem;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FluidSimulationSystem extends SystemPackage {

    /*
     * Advances one subchunk's liquid by a single simulation step: falling,
     * then diagonal falling, then either lateral equalization or a full
     * basin fill. A basin is discovered by flooding outward through open,
     * supported floor cells only — air or matching liquid whose own down
     * neighbor is solid or liquid — using the same neighbor resolution fall
     * and spread use, so it crosses subchunk and chunk boundaries freely.
     * Any lateral cell with nothing beneath it, or any neighbor that can't
     * be resolved at all, breaks the seal immediately and the location is
     * treated as open rather than a basin. Chunks and subchunks touched
     * outside the one passed to flow() are collected for the caller to
     * rebuild and re-register with the renderer.
     */

    private static final Direction3Vector[] LATERAL_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    // Internal
    private BlockManager blockManager;

    // Settings
    private int worldHeight;
    private short airBlockId;

    // Scratch — reused every flow() call, never reallocated
    private boolean[] processed;
    private ObjectArrayList<ChunkInstance> touchedChunks;
    private IntArrayList touchedSubChunkY;

    // Scratch — neighbor resolution output
    private ChunkInstance scratchNeighborChunk;
    private SubChunkInstance scratchNeighborSubChunk;

    // Scratch — up to one target per lateral direction, shared by the
    // diagonal-fall step and the lateral equalization step
    private final int[] spreadTargetPacked = new int[LATERAL_DIRECTIONS.length];
    private final ChunkInstance[] spreadTargetChunk = new ChunkInstance[LATERAL_DIRECTIONS.length];
    private final SubChunkInstance[] spreadTargetSubChunk = new SubChunkInstance[LATERAL_DIRECTIONS.length];
    private final int[] spreadTargetCapacity = new int[LATERAL_DIRECTIONS.length];
    private final int[] spreadTargetExistingLevel = new int[LATERAL_DIRECTIONS.length];

    // Scratch — basin floor flood-fill. Every queued and collected cell
    // carries its owning chunk/subchunk alongside its packed local position
    // so the walk can cross subchunk and chunk boundaries the same way
    // fall and spread do.
    private int[] basinDequePacked;
    private ChunkInstance[] basinDequeChunk;
    private SubChunkInstance[] basinDequeSubChunk;

    private int[] basinCellPacked;
    private ChunkInstance[] basinCellChunk;
    private SubChunkInstance[] basinCellSubChunk;

    private SubChunkInstance[] basinVisitedSubChunk;
    private int[] basinVisitedPacked;
    private int basinVisitedCount;

    private boolean lastFloodEnclosed;

    // Current tick context
    private ChunkInstance currentChunk;
    private SubChunkInstance currentSubChunk;

    // Internal \\

    @Override
    protected void create() {

        this.worldHeight = EngineSetting.WORLD_HEIGHT;

        this.processed = new boolean[ChunkCoordinate3Int.BLOCK_COORDINATE_COUNT];
        this.touchedChunks = new ObjectArrayList<>();
        this.touchedSubChunkY = new IntArrayList();

        int scanLimit = EngineSetting.LIQUID_BASIN_SCAN_LIMIT;

        this.basinDequePacked = new int[scanLimit];
        this.basinDequeChunk = new ChunkInstance[scanLimit];
        this.basinDequeSubChunk = new SubChunkInstance[scanLimit];

        this.basinCellPacked = new int[scanLimit];
        this.basinCellChunk = new ChunkInstance[scanLimit];
        this.basinCellSubChunk = new SubChunkInstance[scanLimit];

        int maxVisited = scanLimit * (LATERAL_DIRECTIONS.length + 1);
        this.basinVisitedSubChunk = new SubChunkInstance[maxVisited];
        this.basinVisitedPacked = new int[maxVisited];
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
    }

    @Override
    protected void awake() {
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    // Flow \\

    public boolean flow(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance) {

        this.currentChunk = chunkInstance;
        this.currentSubChunk = subChunkInstance;

        touchedChunks.clear();
        touchedSubChunkY.clear();
        Arrays.fill(processed, false);

        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();
        BlockPaletteHandle levels = subChunkInstance.getLiquidLevelPaletteHandle();
        int[] coordinates = ChunkCoordinate3Int.getBlockCoordinates();
        boolean changed = false;

        for (int i = 0; i < coordinates.length; i++) {

            if (processed[i])
                continue;

            int packed = coordinates[i];
            short blockID = blocks.getBlock(packed);
            BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(blockID);

            if (blockHandle.getGeometry() != DynamicGeometryType.LIQUID)
                continue;

            int level = levels.getBlock(packed);

            if (level <= 0)
                continue;

            processed[i] = true;

            int remaining = attemptFall(chunkInstance, subChunkInstance, packed, blockID, level);

            if (remaining != level)
                changed = true;

            if (remaining <= 0)
                continue;

            int beforeDiagonal = remaining;
            remaining = attemptDiagonalFall(chunkInstance, subChunkInstance, packed, blockID, remaining);

            if (remaining != beforeDiagonal)
                changed = true;

            if (remaining <= 0)
                continue;

            if (attemptLateralSpread(chunkInstance, subChunkInstance, packed, blockID, remaining))
                changed = true;
        }

        return changed;
    }

    // Gravity \\

    private int attemptFall(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int packed,
            short blockID,
            int amount) {

        int belowPacked = resolveNeighbor(chunkInstance, subChunkInstance, packed, Direction3Vector.DOWN);

        if (scratchNeighborSubChunk == null)
            return amount;

        SubChunkInstance belowSubChunk = scratchNeighborSubChunk;
        short belowBlockID = belowSubChunk.getBlockPaletteHandle().getBlock(belowPacked);

        if (belowBlockID != airBlockId && belowBlockID != blockID)
            return amount;

        int existingLevel = belowBlockID == blockID
                ? belowSubChunk.getLiquidLevelPaletteHandle().getBlock(belowPacked)
                : 0;
        int capacity = EngineSetting.LIQUID_LEVEL_MAX - existingLevel;

        if (capacity <= 0)
            return amount;

        spreadTargetPacked[0] = belowPacked;
        spreadTargetChunk[0] = scratchNeighborChunk;
        spreadTargetSubChunk[0] = belowSubChunk;
        spreadTargetCapacity[0] = capacity;
        spreadTargetExistingLevel[0] = existingLevel;

        return distributeAcrossTargets(subChunkInstance, packed, blockID, amount, 1);
    }

    // Diagonal Fall \\

    private int attemptDiagonalFall(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int packed,
            short blockID,
            int amount) {

        int belowPacked = resolveNeighbor(chunkInstance, subChunkInstance, packed, Direction3Vector.DOWN);

        if (scratchNeighborSubChunk == null)
            return amount;

        ChunkInstance belowChunk = scratchNeighborChunk;
        SubChunkInstance belowSubChunk = scratchNeighborSubChunk;

        int targetCount = collectSpreadTargets(belowChunk, belowSubChunk, belowPacked, blockID);

        if (targetCount == 0)
            return amount;

        return distributeAcrossTargets(subChunkInstance, packed, blockID, amount, targetCount);
    }

    // Lateral Spread \\

    private boolean attemptLateralSpread(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int packed,
            short blockID,
            int amount) {

        int cellCount = floodFillBasin(packed, blockID);

        if (lastFloodEnclosed)
            return settleEnclosedPocket(subChunkInstance, packed, blockID, amount, cellCount);

        int targetCount = collectSpreadTargets(chunkInstance, subChunkInstance, packed, blockID);
        boolean moved = targetCount > 0
                && equalizeLaterally(subChunkInstance, packed, blockID, amount, targetCount);

        int remaining = moved
                ? subChunkInstance.getLiquidLevelPaletteHandle().getBlock(packed)
                : amount;

        if (remaining <= 0)
            return moved;

        return decayCell(subChunkInstance, packed, blockID, remaining) || moved;
    }

    private boolean equalizeLaterally(
            SubChunkInstance sourceSubChunk,
            int sourcePacked,
            short blockID,
            int amount,
            int targetCount) {

        int pool = amount;
        for (int i = 0; i < targetCount; i++)
            pool += spreadTargetExistingLevel[i];

        int poolSize = targetCount + 1;
        int fairShare = pool / poolSize;
        int remainder = pool % poolSize;

        int outgoing = amount - fairShare;

        if (outgoing <= 0)
            return false;

        int budget = outgoing - EngineSetting.LIQUID_HORIZONTAL_MOVE_CONSISTENCY_LOSS;

        if (budget <= 0)
            return false;

        int delivered = 0;

        for (int i = 0; i < targetCount && budget > 0; i++) {

            int targetFairShare = fairShare + (i < remainder ? 1 : 0);
            int need = targetFairShare - spreadTargetExistingLevel[i];

            if (need <= 0)
                continue;

            int transferable = Math.min(need, Math.min(budget, spreadTargetCapacity[i]));

            if (transferable <= 0)
                continue;

            SubChunkInstance targetSubChunk = spreadTargetSubChunk[i];
            int targetPacked = spreadTargetPacked[i];

            targetSubChunk.setBlock(targetPacked, blockID);
            targetSubChunk.setLiquidLevel(targetPacked, (short) (spreadTargetExistingLevel[i] + transferable));

            if (targetSubChunk == sourceSubChunk)
                processed[ChunkCoordinate3Int.getIndex(targetPacked)] = true;

            markTouched(spreadTargetChunk[i], targetSubChunk);

            budget -= transferable;
            delivered += transferable;
        }

        if (delivered == 0)
            return false;

        int sourceRemaining = amount - delivered;

        if (sourceRemaining <= 0) {
            sourceSubChunk.setBlock(sourcePacked, airBlockId);
            sourceSubChunk.setLiquidLevel(sourcePacked, EngineSetting.LIQUID_LEVEL_EMPTY);
        } else {
            sourceSubChunk.setLiquidLevel(sourcePacked, (short) sourceRemaining);
        }

        return true;
    }

    // Spread Targets \\

    private int collectSpreadTargets(
            ChunkInstance fromChunk,
            SubChunkInstance fromSubChunk,
            int fromPacked,
            short blockID) {

        int targetCount = 0;

        for (int d = 0; d < LATERAL_DIRECTIONS.length; d++) {

            int targetPacked = resolveNeighbor(fromChunk, fromSubChunk, fromPacked, LATERAL_DIRECTIONS[d]);

            if (scratchNeighborSubChunk == null)
                continue;

            SubChunkInstance targetSubChunk = scratchNeighborSubChunk;
            short targetBlockID = targetSubChunk.getBlockPaletteHandle().getBlock(targetPacked);

            if (targetBlockID != airBlockId && targetBlockID != blockID)
                continue;

            int existingLevel = targetBlockID == blockID
                    ? targetSubChunk.getLiquidLevelPaletteHandle().getBlock(targetPacked)
                    : 0;
            int capacity = EngineSetting.LIQUID_LEVEL_MAX - existingLevel;

            if (capacity <= 0)
                continue;

            spreadTargetPacked[targetCount] = targetPacked;
            spreadTargetChunk[targetCount] = scratchNeighborChunk;
            spreadTargetSubChunk[targetCount] = targetSubChunk;
            spreadTargetCapacity[targetCount] = capacity;
            spreadTargetExistingLevel[targetCount] = existingLevel;
            targetCount++;
        }

        return targetCount;
    }

    private int distributeAcrossTargets(
            SubChunkInstance sourceSubChunk,
            int sourcePacked,
            short blockID,
            int amount,
            int targetCount) {

        int share = amount / targetCount;
        int remainder = amount % targetCount;
        int leftover = 0;

        for (int i = 0; i < targetCount; i++) {

            int desired = share + (i < remainder ? 1 : 0);

            if (desired <= 0)
                continue;

            int capacity = spreadTargetCapacity[i];
            int transferable = Math.min(desired, capacity);

            leftover += desired - transferable;

            if (transferable <= 0)
                continue;

            SubChunkInstance targetSubChunk = spreadTargetSubChunk[i];
            int targetPacked = spreadTargetPacked[i];

            targetSubChunk.setBlock(targetPacked, blockID);
            targetSubChunk.setLiquidLevel(targetPacked, (short) (spreadTargetExistingLevel[i] + transferable));

            if (targetSubChunk == sourceSubChunk)
                processed[ChunkCoordinate3Int.getIndex(targetPacked)] = true;

            markTouched(spreadTargetChunk[i], targetSubChunk);
        }

        if (leftover == amount)
            return leftover;

        if (leftover <= 0) {
            sourceSubChunk.setBlock(sourcePacked, airBlockId);
            sourceSubChunk.setLiquidLevel(sourcePacked, EngineSetting.LIQUID_LEVEL_EMPTY);
            return 0;
        }

        sourceSubChunk.setLiquidLevel(sourcePacked, (short) leftover);
        return leftover;
    }

    // Enclosed Pocket \\

    private boolean settleEnclosedPocket(
            SubChunkInstance sourceSubChunk,
            int sourcePacked,
            short blockID,
            int incomingAmount,
            int cellCount) {

        int total = incomingAmount;

        for (int i = 0; i < cellCount; i++) {

            SubChunkInstance cellSubChunk = basinCellSubChunk[i];
            int cellPacked = basinCellPacked[i];

            if (cellSubChunk == sourceSubChunk && cellPacked == sourcePacked)
                continue;

            if (cellSubChunk.getBlockPaletteHandle().getBlock(cellPacked) == blockID)
                total += cellSubChunk.getLiquidLevelPaletteHandle().getBlock(cellPacked);
        }

        if (total < EngineSetting.LIQUID_BASIN_FILL_THRESHOLD * cellCount)
            return dissolvePocket(sourceSubChunk, sourcePacked, blockID, incomingAmount, cellCount);

        int share = total / cellCount;
        int remainder = total % cellCount;
        boolean changed = false;

        for (int i = 0; i < cellCount; i++) {

            ChunkInstance cellChunk = basinCellChunk[i];
            SubChunkInstance cellSubChunk = basinCellSubChunk[i];
            int cellPacked = basinCellPacked[i];
            boolean isSource = cellSubChunk == sourceSubChunk && cellPacked == sourcePacked;

            if (!isSource && cellSubChunk == currentSubChunk)
                processed[ChunkCoordinate3Int.getIndex(cellPacked)] = true;

            boolean isMatchingLiquid = cellSubChunk.getBlockPaletteHandle().getBlock(cellPacked) == blockID;
            int oldLevel = isSource
                    ? incomingAmount
                    : (isMatchingLiquid ? cellSubChunk.getLiquidLevelPaletteHandle().getBlock(cellPacked) : 0);
            int newLevel = share + (i < remainder ? 1 : 0);

            if (newLevel == oldLevel)
                continue;

            if (newLevel <= 0) {
                cellSubChunk.setBlock(cellPacked, airBlockId);
                cellSubChunk.setLiquidLevel(cellPacked, EngineSetting.LIQUID_LEVEL_EMPTY);
            } else {
                cellSubChunk.setBlock(cellPacked, blockID);
                cellSubChunk.setLiquidLevel(cellPacked, (short) newLevel);
            }

            markTouched(cellChunk, cellSubChunk);
            changed = true;
        }

        return changed;
    }

    private boolean dissolvePocket(
            SubChunkInstance sourceSubChunk,
            int sourcePacked,
            short blockID,
            int incomingAmount,
            int cellCount) {

        boolean changed = false;

        for (int i = 0; i < cellCount; i++) {

            ChunkInstance cellChunk = basinCellChunk[i];
            SubChunkInstance cellSubChunk = basinCellSubChunk[i];
            int cellPacked = basinCellPacked[i];
            boolean isSource = cellSubChunk == sourceSubChunk && cellPacked == sourcePacked;

            if (!isSource && cellSubChunk == currentSubChunk)
                processed[ChunkCoordinate3Int.getIndex(cellPacked)] = true;

            if (!isSource && cellSubChunk.getBlockPaletteHandle().getBlock(cellPacked) != blockID)
                continue;

            int currentLevel = isSource
                    ? incomingAmount
                    : cellSubChunk.getLiquidLevelPaletteHandle().getBlock(cellPacked);

            if (decayCell(cellSubChunk, cellPacked, blockID, currentLevel)) {
                markTouched(cellChunk, cellSubChunk);
                changed = true;
            }
        }

        return changed;
    }

    private boolean decayCell(
            SubChunkInstance subChunkInstance,
            int packed,
            short blockID,
            int currentLevel) {

        int newLevel = currentLevel - EngineSetting.LIQUID_HORIZONTAL_MOVE_CONSISTENCY_LOSS;

        if (newLevel >= currentLevel)
            return false;

        if (newLevel <= 0) {
            subChunkInstance.setBlock(packed, airBlockId);
            subChunkInstance.setLiquidLevel(packed, EngineSetting.LIQUID_LEVEL_EMPTY);
        } else {
            subChunkInstance.setBlock(packed, blockID);
            subChunkInstance.setLiquidLevel(packed, (short) newLevel);
        }

        return true;
    }

    /*
     * Floods outward from the source through same-level floor cells only —
     * air or matching liquid whose own down neighbor is solid or matching
     * liquid. A lateral neighbor with nothing beneath it means water can
     * spill out that way, which immediately breaks the seal; so does a
     * neighbor that can't be resolved at all (world edge, unloaded chunk).
     * Hitting the scan limit while still sealed treats the pocket as too
     * large to bother simulating as a basin.
     */
    private int floodFillBasin(int sourcePacked, short blockID) {

        int front = 0;
        int back = 0;
        int cellCount = 0;
        boolean enclosed = true;

        basinDequePacked[back] = sourcePacked;
        basinDequeChunk[back] = currentChunk;
        basinDequeSubChunk[back] = currentSubChunk;
        back++;

        basinCellPacked[cellCount] = sourcePacked;
        basinCellChunk[cellCount] = currentChunk;
        basinCellSubChunk[cellCount] = currentSubChunk;
        cellCount++;

        markBasinVisited(currentSubChunk, sourcePacked);

        if (!hasSealedFloor(currentChunk, currentSubChunk, sourcePacked, blockID)) {
            lastFloodEnclosed = false;
            clearBasinVisited();
            return cellCount;
        }

        outer: while (front < back) {

            int cellPacked = basinDequePacked[front];
            ChunkInstance cellChunk = basinDequeChunk[front];
            SubChunkInstance cellSubChunk = basinDequeSubChunk[front];
            front++;

            for (int d = 0; d < LATERAL_DIRECTIONS.length; d++) {

                Direction3Vector direction = LATERAL_DIRECTIONS[d];
                int neighborPacked = resolveNeighbor(cellChunk, cellSubChunk, cellPacked, direction);

                if (scratchNeighborSubChunk == null) {
                    enclosed = false;
                    break outer;
                }

                ChunkInstance neighborChunk = scratchNeighborChunk;
                SubChunkInstance neighborSubChunk = scratchNeighborSubChunk;

                if (isBasinVisited(neighborSubChunk, neighborPacked))
                    continue;

                short neighborBlockID = neighborSubChunk.getBlockPaletteHandle().getBlock(neighborPacked);

                if (neighborBlockID != airBlockId && neighborBlockID != blockID) {
                    markBasinVisited(neighborSubChunk, neighborPacked);
                    continue;
                }

                if (cellCount >= EngineSetting.LIQUID_BASIN_SCAN_LIMIT) {
                    enclosed = false;
                    break outer;
                }

                if (!hasSealedFloor(neighborChunk, neighborSubChunk, neighborPacked, blockID)) {
                    enclosed = false;
                    break outer;
                }

                markBasinVisited(neighborSubChunk, neighborPacked);

                basinCellPacked[cellCount] = neighborPacked;
                basinCellChunk[cellCount] = neighborChunk;
                basinCellSubChunk[cellCount] = neighborSubChunk;
                cellCount++;

                basinDequePacked[back] = neighborPacked;
                basinDequeChunk[back] = neighborChunk;
                basinDequeSubChunk[back] = neighborSubChunk;
                back++;
            }
        }

        lastFloodEnclosed = enclosed;
        clearBasinVisited();
        return cellCount;
    }

    private boolean hasSealedFloor(ChunkInstance chunk, SubChunkInstance subChunk, int packed, short blockID) {

        int belowPacked = resolveNeighbor(chunk, subChunk, packed, Direction3Vector.DOWN);

        if (scratchNeighborSubChunk == null)
            return true;

        SubChunkInstance belowSubChunk = scratchNeighborSubChunk;
        short belowBlockID = belowSubChunk.getBlockPaletteHandle().getBlock(belowPacked);

        if (belowBlockID == airBlockId)
            return false;

        if (belowBlockID == blockID)
            return belowSubChunk.getLiquidLevelPaletteHandle().getBlock(belowPacked) >= EngineSetting.LIQUID_LEVEL_MAX;

        return true;
    }

    private void markBasinVisited(SubChunkInstance subChunk, int packed) {
        basinVisitedSubChunk[basinVisitedCount] = subChunk;
        basinVisitedPacked[basinVisitedCount] = packed;
        basinVisitedCount++;
    }

    private boolean isBasinVisited(SubChunkInstance subChunk, int packed) {
        for (int i = 0; i < basinVisitedCount; i++)
            if (basinVisitedSubChunk[i] == subChunk && basinVisitedPacked[i] == packed)
                return true;
        return false;
    }

    private void clearBasinVisited() {
        basinVisitedCount = 0;
    }

    // Neighbor Resolution \\

    private int resolveNeighbor(
            ChunkInstance chunk,
            SubChunkInstance subChunk,
            int packed,
            Direction3Vector direction) {

        if (!ChunkCoordinate3Int.isAtEdge(packed, direction)) {
            scratchNeighborChunk = chunk;
            scratchNeighborSubChunk = subChunk;
            return ChunkCoordinate3Int.getNeighborAndWrap(packed, direction);
        }

        if (direction == Direction3Vector.UP || direction == Direction3Vector.DOWN) {

            int neighborSubChunkY = (int) subChunk.getCoordinate() + direction.y;

            if (neighborSubChunkY < 0 || neighborSubChunkY >= worldHeight) {
                scratchNeighborChunk = null;
                scratchNeighborSubChunk = null;
                return -1;
            }

            scratchNeighborChunk = chunk;
            scratchNeighborSubChunk = chunk.getSubChunk(neighborSubChunkY);
            return ChunkCoordinate3Int.getNeighborAndWrap(packed, direction);
        }

        ChunkInstance neighborChunk = chunk.getChunkNeighbors().getNeighborChunk(direction.to2D().index);

        if (neighborChunk == null) {
            scratchNeighborChunk = null;
            scratchNeighborSubChunk = null;
            return -1;
        }

        scratchNeighborChunk = neighborChunk;
        scratchNeighborSubChunk = neighborChunk.getSubChunk((int) subChunk.getCoordinate());
        return ChunkCoordinate3Int.getNeighborAndWrap(packed, direction);
    }

    // Touched Tracking \\

    private void markTouched(ChunkInstance chunk, SubChunkInstance subChunk) {

        if (chunk == currentChunk && subChunk == currentSubChunk)
            return;

        for (int i = 0; i < touchedChunks.size(); i++)
            if (touchedChunks.get(i) == chunk && touchedSubChunkY.getInt(i) == (int) subChunk.getCoordinate())
                return;

        touchedChunks.add(chunk);
        touchedSubChunkY.add((int) subChunk.getCoordinate());
    }

    // Accessible \\

    public ObjectArrayList<ChunkInstance> getTouchedChunks() {
        return touchedChunks;
    }

    public IntArrayList getTouchedSubChunkY() {
        return touchedSubChunkY;
    }
}