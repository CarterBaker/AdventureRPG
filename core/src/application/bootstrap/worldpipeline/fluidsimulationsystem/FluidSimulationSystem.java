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
     * Advances one subchunk's liquid by a single simulation step. Each liquid
     * cell prefers to fall straight down, then diagonally downward, and only
     * then spreads laterally to open neighbors; lateral movement equalizes
     * with neighbors rather than handing over its full amount and loses
     * EngineSetting.LIQUID_HORIZONTAL_MOVE_CONSISTENCY_LOSS per move, while
     * falling and diagonal falling retain full consistency. Whatever a cell
     * cannot fall or spread away is validated against
     * EngineSetting.LIQUID_BASIN_FILL_THRESHOLD before being left in place —
     * a fully enclosed pocket is discovered structurally, never bounded by
     * how much the arriving trickle alone carries, combines every liquid
     * cell already inside it, and either fills evenly or dissolves as a
     * whole against that threshold; an open cell with nothing left to give
     * away is judged the same way. Dissolving always removes
     * EngineSetting.LIQUID_DISSOLVE_AMOUNT_PER_TICK worth of consistency per
     * step rather than vanishing outright, so a doomed pocket drains out
     * over several ticks instead of popping. Chunks and subchunks touched
     * beyond the one passed to flow() are collected for the caller to
     * rebuild and re-register with the renderer.
     */

    private static final Direction3Vector[] LATERAL_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    private static final Direction3Vector[] BASIN_FLOOD_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST,
            Direction3Vector.DOWN
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

    // Scratch — double-ended BFS enclosed-pocket discovery; downward hops are
    // pushed to the front so they are visited before same-tier lateral hops,
    // giving lower cells first claim on any remainder when a basin fills
    private int[] basinDequePacked;
    private int basinDequeMid;
    private int[] basinCells;
    private boolean[] basinVisited;
    private int[] basinVisitedTouched;
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

        this.basinDequeMid = EngineSetting.LIQUID_BASIN_SCAN_LIMIT;
        this.basinDequePacked = new int[EngineSetting.LIQUID_BASIN_SCAN_LIMIT * 2];
        this.basinCells = new int[EngineSetting.LIQUID_BASIN_SCAN_LIMIT];

        this.basinVisited = new boolean[ChunkCoordinate3Int.BLOCK_COORDINATE_COUNT];
        this.basinVisitedTouched = new int[ChunkCoordinate3Int.BLOCK_COORDINATE_COUNT];
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

        if (remaining >= EngineSetting.LIQUID_BASIN_FILL_THRESHOLD)
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

        sourceSubChunk.setLiquidLevel(sourcePacked, (short) (amount - delivered));
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

            int targetPacked = resolveLateralInChunk(fromChunk, fromSubChunk, fromPacked, LATERAL_DIRECTIONS[d]);

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
            SubChunkInstance subChunkInstance,
            int sourcePacked,
            short blockID,
            int incomingAmount,
            int cellCount) {

        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();
        BlockPaletteHandle levels = subChunkInstance.getLiquidLevelPaletteHandle();

        int total = incomingAmount;

        for (int i = 0; i < cellCount; i++) {
            int cellPacked = basinCells[i];
            if (cellPacked != sourcePacked && blocks.getBlock(cellPacked) == blockID)
                total += levels.getBlock(cellPacked);
        }

        if (total < EngineSetting.LIQUID_BASIN_FILL_THRESHOLD * cellCount)
            return dissolvePocket(subChunkInstance, sourcePacked, blockID, incomingAmount, cellCount);

        int share = total / cellCount;
        int remainder = total % cellCount;
        boolean changed = false;

        for (int i = 0; i < cellCount; i++) {

            int cellPacked = basinCells[i];
            boolean isSource = cellPacked == sourcePacked;

            if (!isSource)
                processed[ChunkCoordinate3Int.getIndex(cellPacked)] = true;

            boolean isMatchingLiquid = blocks.getBlock(cellPacked) == blockID;
            int oldLevel = isSource ? incomingAmount : (isMatchingLiquid ? levels.getBlock(cellPacked) : 0);
            int newLevel = share + (i < remainder ? 1 : 0);

            if (newLevel == oldLevel)
                continue;

            subChunkInstance.setBlock(cellPacked, blockID);
            subChunkInstance.setLiquidLevel(cellPacked, (short) newLevel);
            changed = true;
        }

        return changed;
    }

    /*
     * A pocket whose combined water — every matching cell already inside it,
     * plus whatever is currently arriving — falls short of
     * EngineSetting.LIQUID_BASIN_FILL_THRESHOLD times its own cell count
     * does not vanish in one step. Each matching cell, source included,
     * loses EngineSetting.LIQUID_DISSOLVE_AMOUNT_PER_TICK worth of
     * consistency this tick and is re-evaluated on the next one, so the
     * pocket visibly drains away instead of popping.
     */
    private boolean dissolvePocket(
            SubChunkInstance subChunkInstance,
            int sourcePacked,
            short blockID,
            int incomingAmount,
            int cellCount) {

        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();
        BlockPaletteHandle levels = subChunkInstance.getLiquidLevelPaletteHandle();
        boolean changed = false;

        for (int i = 0; i < cellCount; i++) {

            int cellPacked = basinCells[i];
            boolean isSource = cellPacked == sourcePacked;

            if (!isSource)
                processed[ChunkCoordinate3Int.getIndex(cellPacked)] = true;

            if (!isSource && blocks.getBlock(cellPacked) != blockID)
                continue;

            int currentLevel = isSource ? incomingAmount : levels.getBlock(cellPacked);

            if (decayCell(subChunkInstance, cellPacked, blockID, currentLevel))
                changed = true;
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
     * Discovers the full connected air/matching-liquid region reachable from
     * sourcePacked, up to EngineSetting.LIQUID_BASIN_SCAN_LIMIT cells,
     * walking downward hops before lateral ones so lower cells are recorded
     * first. The region is "enclosed" only if the scan completes without
     * running off the edge of the space or past the scan limit — enclosure
     * is a structural property of the space itself, never a function of how
     * much liquid happens to be arriving right now, so a large but genuinely
     * walled basin is still recognized and its contents combined as one body.
     */
    private int floodFillBasin(int sourcePacked, short blockID) {

        BlockPaletteHandle blocks = currentSubChunk.getBlockPaletteHandle();

        int front = basinDequeMid;
        int back = basinDequeMid;
        int enqueued = 1;
        int cellCount = 0;
        boolean enclosed = true;

        basinDequePacked[back] = sourcePacked;
        back++;
        markBasinVisited(sourcePacked);

        outer: while (front < back) {

            int currentPacked = basinDequePacked[front];
            front++;

            basinCells[cellCount++] = currentPacked;

            for (int d = 0; d < BASIN_FLOOD_DIRECTIONS.length; d++) {

                if (enqueued >= EngineSetting.LIQUID_BASIN_SCAN_LIMIT) {
                    enclosed = false;
                    break outer;
                }

                Direction3Vector direction = BASIN_FLOOD_DIRECTIONS[d];
                int neighborPacked = ChunkCoordinate3Int.getNeighbor(currentPacked, direction);

                if (neighborPacked == -1) {
                    enclosed = false;
                    break outer;
                }

                if (isBasinVisited(neighborPacked))
                    continue;

                short neighborBlockID = blocks.getBlock(neighborPacked);

                if (neighborBlockID != airBlockId && neighborBlockID != blockID) {
                    markBasinVisited(neighborPacked);
                    continue;
                }

                markBasinVisited(neighborPacked);
                enqueued++;

                if (direction == Direction3Vector.DOWN) {
                    front--;
                    basinDequePacked[front] = neighborPacked;
                } else {
                    basinDequePacked[back] = neighborPacked;
                    back++;
                }
            }
        }

        lastFloodEnclosed = enclosed;
        clearBasinVisited();
        return cellCount;
    }

    private void markBasinVisited(int packed) {
        int index = ChunkCoordinate3Int.getIndex(packed);
        basinVisited[index] = true;
        basinVisitedTouched[basinVisitedCount++] = index;
    }

    private boolean isBasinVisited(int packed) {
        return basinVisited[ChunkCoordinate3Int.getIndex(packed)];
    }

    private void clearBasinVisited() {
        for (int i = 0; i < basinVisitedCount; i++)
            basinVisited[basinVisitedTouched[i]] = false;
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

    private int resolveLateralInChunk(
            ChunkInstance chunk,
            SubChunkInstance subChunk,
            int packed,
            Direction3Vector direction) {

        if (ChunkCoordinate3Int.isAtEdge(packed, direction)) {
            scratchNeighborChunk = null;
            scratchNeighborSubChunk = null;
            return -1;
        }

        scratchNeighborChunk = chunk;
        scratchNeighborSubChunk = subChunk;
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