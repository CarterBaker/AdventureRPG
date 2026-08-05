package application.bootstrap.worldpipeline.liquidsimulationsystem;

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

public class LiquidSimulationSystem extends SystemPackage {

    /*
     * Redistributes one subchunk's liquid by a single flow step — gravity
     * first, then lateral movement with whatever a source cell could not send
     * downward. Falling never costs cohesion, and neither does a lateral step
     * that still leads toward an open drop (a "cascade" candidate — the water
     * is still descending, just routed around a block corner). Only a lateral
     * step that resolves into genuine pooling, where every candidate is itself
     * resting on solid ground, pays the fixed per-move dissipation. A same-
     * liquid neighbor already full is treated as solid, redirecting that share
     * one cell upward instead. Every cell is visited at most once per call as
     * a source, so a column advances exactly one step per flow tick. Chunks
     * and subchunks touched beyond the one passed to flow() are collected for
     * the caller to rebuild and re-register with the renderer. Every write
     * below goes through SubChunkInstance.setBlock()/setLiquidLevel() rather
     * than the palette handles directly, so liquidStable is invalidated
     * automatically wherever this system touches it.
     */

    private static final Direction3Vector[] CARDINAL_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    private static final Direction3Vector[] BASIN_PROBE_DIRECTIONS = {
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
    private int[] candidatePacked;
    private ChunkInstance[] candidateChunk;
    private SubChunkInstance[] candidateSubChunk;
    private boolean[] candidateCascade;

    // Scratch — neighbor/target resolution output
    private ChunkInstance scratchNeighborChunk;
    private SubChunkInstance scratchNeighborSubChunk;
    private ChunkInstance scratchTargetChunk;
    private SubChunkInstance scratchTargetSubChunk;

    // Scratch — bounded basin flood-fill probe, reused by every attemptFall()
    // call that needs one. Sized to EngineSetting.LIQUID_BASIN_SCAN_LIMIT — the
    // probe bails the instant it would need to grow past that.
    private int[] basinQueue;
    private int[] basinCells;
    private boolean[] basinVisited;
    private int[] basinVisitedTouched;
    private int basinVisitedCount;
    private int basinCellCount;
    private int basinTotalCapacity;

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
        this.candidatePacked = new int[CARDINAL_DIRECTIONS.length];
        this.candidateChunk = new ChunkInstance[CARDINAL_DIRECTIONS.length];
        this.candidateSubChunk = new SubChunkInstance[CARDINAL_DIRECTIONS.length];
        this.candidateCascade = new boolean[CARDINAL_DIRECTIONS.length];

        this.basinQueue = new int[EngineSetting.LIQUID_BASIN_SCAN_LIMIT];
        this.basinCells = new int[EngineSetting.LIQUID_BASIN_SCAN_LIMIT];
        this.basinVisited = new boolean[ChunkCoordinate3Int.BLOCK_COORDINATE_COUNT];
        this.basinVisitedTouched = new int[EngineSetting.LIQUID_BASIN_SCAN_LIMIT];
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

            if (remaining > 0)
                remaining = attemptSpread(chunkInstance, subChunkInstance, packed, blockID, remaining);

            if (remaining == level)
                continue;

            changed = true;

            if (remaining <= 0) {
                subChunkInstance.setBlock(packed, airBlockId);
                subChunkInstance.setLiquidLevel(packed, EngineSetting.LIQUID_LEVEL_EMPTY);
            } else {
                subChunkInstance.setLiquidLevel(packed, (short) remaining);
            }
        }

        return changed;
    }

    // Gravity \\

    private int attemptFall(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int packed, short blockID,
            int amount) {

        int belowPacked = resolveNeighbor(chunkInstance, subChunkInstance, packed, Direction3Vector.DOWN);

        if (scratchNeighborSubChunk == null)
            return amount;

        ChunkInstance belowChunk = scratchNeighborChunk;
        SubChunkInstance belowSubChunk = scratchNeighborSubChunk;
        short belowBlockID = belowSubChunk.getBlockPaletteHandle().getBlock(belowPacked);

        if (belowBlockID != airBlockId && belowBlockID != blockID)
            return amount;

        short existingLevel = belowBlockID == blockID
                ? belowSubChunk.getLiquidLevelPaletteHandle().getBlock(belowPacked)
                : 0;
        int capacity = EngineSetting.LIQUID_LEVEL_MAX - existingLevel;

        if (capacity <= 0)
            return amount;

        int transfer = Math.min(amount, capacity);

        if (existingLevel <= 0 && transfer < EngineSetting.LIQUID_LEVEL_MIN_PERSIST)
            return resolveBasinOutcome(belowSubChunk, belowChunk, belowPacked, blockID, amount);

        belowSubChunk.setBlock(belowPacked, blockID);
        belowSubChunk.setLiquidLevel(belowPacked, (short) (existingLevel + transfer));

        if (belowSubChunk == subChunkInstance)
            processed[ChunkCoordinate3Int.getIndex(belowPacked)] = true;

        markTouched(belowChunk, belowSubChunk);

        return amount - transfer;
    }

    // Lateral Movement \\

    /*
     * A candidate is a "cascade" when the cell beyond it still has open air
     * underneath — reaching it is a detour around an obstacle on the way
     * further down, not a genuine rest. Whenever at least one cascade
     * candidate exists, the full amount is routed only through cascade
     * candidates and no cohesion is spent. Only when every candidate is
     * itself resting on solid ground does the fixed per-move dissipation
     * apply.
     */
    private int attemptSpread(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int packed, short blockID,
            int amount) {

        if (amount < EngineSetting.LIQUID_LEVEL_MIN_PERSIST)
            return amount;

        int candidateCount = 0;
        boolean hasCascade = false;

        for (int d = 0; d < CARDINAL_DIRECTIONS.length; d++) {

            int targetPacked = resolveSpreadTarget(chunkInstance, subChunkInstance, packed, blockID,
                    CARDINAL_DIRECTIONS[d]);

            if (targetPacked == -1)
                continue;

            candidatePacked[candidateCount] = targetPacked;
            candidateChunk[candidateCount] = scratchTargetChunk;
            candidateSubChunk[candidateCount] = scratchTargetSubChunk;

            boolean cascade = isCascadeTarget(scratchTargetChunk, scratchTargetSubChunk, targetPacked);
            candidateCascade[candidateCount] = cascade;
            hasCascade |= cascade;

            candidateCount++;
        }

        if (candidateCount == 0)
            return amount;

        if (hasCascade)
            return distributeCascade(subChunkInstance, blockID, amount, candidateCount);

        return distributePool(subChunkInstance, blockID, amount, candidateCount);
    }

    private boolean isCascadeTarget(ChunkInstance chunk, SubChunkInstance subChunk, int packed) {

        int belowPacked = resolveNeighbor(chunk, subChunk, packed, Direction3Vector.DOWN);

        if (scratchNeighborSubChunk == null)
            return false;

        return scratchNeighborSubChunk.getBlockPaletteHandle().getBlock(belowPacked) == airBlockId;
    }

    // Cascade candidates carry the whole amount forward at no cost — this is
    // still descending motion, just routed around whatever blocked a straight
    // fall. Non-cascade candidates found in the same pass are ignored here;
    // they simply have nothing offered to them this step.
    private int distributeCascade(SubChunkInstance subChunkInstance, short blockID, int amount, int candidateCount) {

        int cascadeCount = 0;

        for (int i = 0; i < candidateCount; i++) {

            if (!candidateCascade[i])
                continue;

            if (cascadeCount != i) {
                candidatePacked[cascadeCount] = candidatePacked[i];
                candidateChunk[cascadeCount] = candidateChunk[i];
                candidateSubChunk[cascadeCount] = candidateSubChunk[i];
            }

            cascadeCount++;
        }

        int maxRecipients = Math.max(1, amount / EngineSetting.LIQUID_LEVEL_MIN_PERSIST);
        int usedCandidates = Math.min(cascadeCount, maxRecipients);

        int share = amount / usedCandidates;
        int shareRemainder = amount % usedCandidates;

        int remaining = amount;

        for (int i = 0; i < usedCandidates; i++) {

            int targetPacked = candidatePacked[i];
            SubChunkInstance targetSubChunk = candidateSubChunk[i];
            ChunkInstance targetChunk = candidateChunk[i];

            int offer = share + (i < shareRemainder ? 1 : 0);

            short existingLevel = targetSubChunk.getLiquidLevelPaletteHandle().getBlock(targetPacked);
            int capacity = EngineSetting.LIQUID_LEVEL_MAX - existingLevel;
            int transfer = Math.min(offer, capacity);

            if (transfer <= 0)
                continue;

            targetSubChunk.setBlock(targetPacked, blockID);
            targetSubChunk.setLiquidLevel(targetPacked, (short) (existingLevel + transfer));

            remaining -= transfer;

            if (targetSubChunk == subChunkInstance)
                processed[ChunkCoordinate3Int.getIndex(targetPacked)] = true;

            markTouched(targetChunk, targetSubChunk);
        }

        return Math.max(remaining, 0);
    }

    // Pool candidates are all resting on solid ground — genuine sideways
    // spreading, so the fixed per-move dissipation applies, and an offer too
    // small to seed a fresh cell is discarded rather than placed.
    private int distributePool(SubChunkInstance subChunkInstance, short blockID, int amount, int candidateCount) {

        int dissipated = Math.min(EngineSetting.LIQUID_SPREAD_DISSIPATION_AMOUNT, amount);
        int spreadable = amount - dissipated;

        if (spreadable <= 0)
            return 0;

        int maxRecipients = Math.max(1, spreadable / EngineSetting.LIQUID_LEVEL_MIN_PERSIST);
        int usedCandidates = Math.min(candidateCount, maxRecipients);

        int share = spreadable / usedCandidates;
        int shareRemainder = spreadable % usedCandidates;

        int remaining = amount;

        for (int i = 0; i < usedCandidates; i++) {

            int targetPacked = candidatePacked[i];
            SubChunkInstance targetSubChunk = candidateSubChunk[i];
            ChunkInstance targetChunk = candidateChunk[i];

            int offer = share + (i < shareRemainder ? 1 : 0);

            short existingLevel = targetSubChunk.getLiquidLevelPaletteHandle().getBlock(targetPacked);
            int capacity = EngineSetting.LIQUID_LEVEL_MAX - existingLevel;
            int transfer = Math.min(offer, capacity);

            if (transfer <= 0)
                continue;

            if (existingLevel <= 0 && transfer < EngineSetting.LIQUID_LEVEL_MIN_PERSIST) {
                remaining -= transfer;
                continue;
            }

            targetSubChunk.setBlock(targetPacked, blockID);
            targetSubChunk.setLiquidLevel(targetPacked, (short) (existingLevel + transfer));

            remaining -= transfer;

            if (targetSubChunk == subChunkInstance)
                processed[ChunkCoordinate3Int.getIndex(targetPacked)] = true;

            markTouched(targetChunk, targetSubChunk);
        }

        remaining -= dissipated;

        return Math.max(remaining, 0);
    }

    /*
     * Resolves where one cardinal direction's share should land. A same-
     * liquid neighbor already at LIQUID_LEVEL_MAX is treated as solid and the
     * cell directly above it is tried instead — plain air only, no further
     * stacking. Returns -1 when the direction is fully blocked.
     */
    private int resolveSpreadTarget(ChunkInstance chunk, SubChunkInstance subChunk, int packed, short blockID,
            Direction3Vector direction) {

        int neighborPacked = resolveNeighbor(chunk, subChunk, packed, direction);

        if (scratchNeighborSubChunk == null)
            return -1;

        ChunkInstance neighborChunk = scratchNeighborChunk;
        SubChunkInstance neighborSubChunk = scratchNeighborSubChunk;
        short neighborBlockID = neighborSubChunk.getBlockPaletteHandle().getBlock(neighborPacked);

        if (neighborBlockID == blockID) {

            if (neighborSubChunk.getLiquidLevelPaletteHandle()
                    .getBlock(neighborPacked) < EngineSetting.LIQUID_LEVEL_MAX) {
                scratchTargetChunk = neighborChunk;
                scratchTargetSubChunk = neighborSubChunk;
                return neighborPacked;
            }

            int upPacked = resolveNeighbor(neighborChunk, neighborSubChunk, neighborPacked, Direction3Vector.UP);

            if (scratchNeighborSubChunk == null
                    || scratchNeighborSubChunk.getBlockPaletteHandle().getBlock(upPacked) != airBlockId)
                return -1;

            scratchTargetChunk = scratchNeighborChunk;
            scratchTargetSubChunk = scratchNeighborSubChunk;
            return upPacked;
        }

        if (neighborBlockID != airBlockId)
            return -1;

        scratchTargetChunk = neighborChunk;
        scratchTargetSubChunk = neighborSubChunk;
        return neighborPacked;
    }

    // Basin Probe \\

    /*
     * Called only when a single destination cell can't accept a valid fall
     * transfer on its own — the offered amount would seed a brand-new puddle
     * below EngineSetting.LIQUID_LEVEL_MIN_PERSIST. Rather than always
     * discarding that amount, this walks the connected pocket the destination
     * sits in and either distributes the whole amount across that pocket when
     * it can genuinely hold it, or dissolves the amount entirely when it can't.
     */
    private int resolveBasinOutcome(
            SubChunkInstance belowSubChunk,
            ChunkInstance belowChunk,
            int belowPacked,
            short blockID,
            int amount) {

        boolean enclosed = probeBasin(belowSubChunk, belowPacked, blockID);

        if (enclosed && basinTotalCapacity >= amount) {
            fillBasin(belowSubChunk, blockID, amount);
            markTouched(belowChunk, belowSubChunk);
        }

        return 0;
    }

    /*
     * Bounded flood fill of the air/open-same-liquid pocket that startPacked
     * sits in, walking the four cardinal directions plus down — never up.
     * Touching the subchunk's edge, or needing more than
     * EngineSetting.LIQUID_BASIN_SCAN_LIMIT cells, both mean the pocket can't
     * be bounded cheaply and the probe reports failure.
     */
    private boolean probeBasin(SubChunkInstance subChunk, int startPacked, short blockID) {

        basinCellCount = 0;
        basinTotalCapacity = 0;

        int head = 0;
        int tail = 0;

        basinQueue[tail++] = startPacked;
        markBasinVisited(startPacked);

        while (head < tail) {

            int currentPacked = basinQueue[head++];

            short currentBlockID = subChunk.getBlockPaletteHandle().getBlock(currentPacked);
            short currentLevel = currentBlockID == blockID
                    ? subChunk.getLiquidLevelPaletteHandle().getBlock(currentPacked)
                    : 0;

            basinCells[basinCellCount++] = currentPacked;
            basinTotalCapacity += EngineSetting.LIQUID_LEVEL_MAX - currentLevel;

            for (int d = 0; d < BASIN_PROBE_DIRECTIONS.length; d++) {

                int neighborPacked = ChunkCoordinate3Int.getNeighbor(currentPacked, BASIN_PROBE_DIRECTIONS[d]);

                if (neighborPacked == -1) {
                    clearBasinVisited();
                    return false;
                }

                if (isBasinVisited(neighborPacked))
                    continue;

                short neighborBlockID = subChunk.getBlockPaletteHandle().getBlock(neighborPacked);

                boolean isOpenAir = neighborBlockID == airBlockId;
                boolean isOpenLiquid = neighborBlockID == blockID
                        && subChunk.getLiquidLevelPaletteHandle()
                                .getBlock(neighborPacked) < EngineSetting.LIQUID_LEVEL_MAX;

                if (!isOpenAir && !isOpenLiquid)
                    continue;

                if (tail >= EngineSetting.LIQUID_BASIN_SCAN_LIMIT) {
                    clearBasinVisited();
                    return false;
                }

                markBasinVisited(neighborPacked);
                basinQueue[tail++] = neighborPacked;
            }
        }

        clearBasinVisited();
        return true;
    }

    /*
     * Distributes amount across every cell probeBasin() just found, filling
     * each to its own capacity before moving to the next. Only ever called
     * once basinTotalCapacity has already been confirmed sufficient, so this
     * always fully spends amount.
     */
    private void fillBasin(SubChunkInstance subChunk, short blockID, int amount) {

        int remaining = amount;

        for (int i = 0; i < basinCellCount && remaining > 0; i++) {

            int cellPacked = basinCells[i];
            short currentBlockID = subChunk.getBlockPaletteHandle().getBlock(cellPacked);
            short existingLevel = currentBlockID == blockID
                    ? subChunk.getLiquidLevelPaletteHandle().getBlock(cellPacked)
                    : 0;

            int capacity = EngineSetting.LIQUID_LEVEL_MAX - existingLevel;

            if (capacity <= 0)
                continue;

            int transfer = Math.min(remaining, capacity);

            subChunk.setBlock(cellPacked, blockID);
            subChunk.setLiquidLevel(cellPacked, (short) (existingLevel + transfer));

            if (subChunk == currentSubChunk)
                processed[ChunkCoordinate3Int.getIndex(cellPacked)] = true;

            remaining -= transfer;
        }
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

    private int resolveNeighbor(ChunkInstance chunk, SubChunkInstance subChunk, int packed,
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