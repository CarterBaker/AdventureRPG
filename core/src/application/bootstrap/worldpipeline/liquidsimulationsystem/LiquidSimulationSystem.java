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
     * first, then horizontal spread with whatever a source cell could not
     * send downward. Falling never loses volume; spreading sideways always
     * dissipates a fraction of what moves, so a puddle thins out and
     * vanishes after crossing a handful of flat blocks instead of
     * persisting at a fixed size forever. Spread recipients are capped so
     * a step never divides itself down to a zero share and freezes in
     * place. A fall that would only be able to seed a brand-new
     * destination cell below EngineSetting.LIQUID_LEVEL_MIN_PERSIST
     * instead probes the connected pocket that cell sits in: a pocket
     * bounded within EngineSetting.LIQUID_BASIN_SCAN_LIMIT cells that can
     * hold the whole amount gets filled outright, and anything too large
     * to bound cheaply, or too small to hold the water at all, dissolves
     * the water entirely rather than leaving an under-threshold trace
     * behind. A same-liquid neighbor already at LIQUID_LEVEL_MAX is
     * treated as solid, redirecting that share one cell upward instead,
     * mirroring water overflowing a full container. Every cell is visited
     * at most once per call as a source, so a column falls exactly one
     * step per flow tick rather than cascading the whole way down in one
     * pass. Mutations are applied directly against the live palettes —
     * this runs on the main thread only, the same as block placement and
     * breaking, so no additional synchronization is required. Chunks and
     * subchunks touched beyond the one passed to flow() are collected for
     * the caller to rebuild and re-register with the renderer.
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

    // Scratch — neighbor/target resolution output
    private ChunkInstance scratchNeighborChunk;
    private SubChunkInstance scratchNeighborSubChunk;
    private ChunkInstance scratchTargetChunk;
    private SubChunkInstance scratchTargetSubChunk;

    // Scratch — bounded basin flood-fill probe, reused by every
    // attemptFall() call that needs one. Sized to
    // EngineSetting.LIQUID_BASIN_SCAN_LIMIT — the probe bails the instant
    // it would need to grow past that, so it never needs to be larger.
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

        // Settings
        this.worldHeight = EngineSetting.WORLD_HEIGHT;

        // Scratch
        this.processed = new boolean[ChunkCoordinate3Int.BLOCK_COORDINATE_COUNT];
        this.touchedChunks = new ObjectArrayList<>();
        this.touchedSubChunkY = new IntArrayList();
        this.candidatePacked = new int[CARDINAL_DIRECTIONS.length];
        this.candidateChunk = new ChunkInstance[CARDINAL_DIRECTIONS.length];
        this.candidateSubChunk = new SubChunkInstance[CARDINAL_DIRECTIONS.length];

        // Basin Probe
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
                blocks.setBlock(packed, airBlockId);
                levels.setBlock(packed, EngineSetting.LIQUID_LEVEL_EMPTY);
            } else {
                levels.setBlock(packed, (short) remaining);
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

        belowSubChunk.getBlockPaletteHandle().setBlock(belowPacked, blockID);
        belowSubChunk.getLiquidLevelPaletteHandle().setBlock(belowPacked, (short) (existingLevel + transfer));

        if (belowSubChunk == subChunkInstance)
            processed[ChunkCoordinate3Int.getIndex(belowPacked)] = true;

        markTouched(belowChunk, belowSubChunk);

        return amount - transfer;
    }

    // Spread \\

    private int attemptSpread(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int packed, short blockID,
            int amount) {

        if (amount < EngineSetting.LIQUID_LEVEL_MIN_PERSIST)
            return amount;

        int candidateCount = 0;

        for (int d = 0; d < CARDINAL_DIRECTIONS.length; d++) {

            int targetPacked = resolveSpreadTarget(chunkInstance, subChunkInstance, packed, blockID,
                    CARDINAL_DIRECTIONS[d]);

            if (targetPacked == -1)
                continue;

            candidatePacked[candidateCount] = targetPacked;
            candidateChunk[candidateCount] = scratchTargetChunk;
            candidateSubChunk[candidateCount] = scratchTargetSubChunk;
            candidateCount++;
        }

        if (candidateCount == 0)
            return amount;

        // Sideways movement always dissipates a fraction of what's moving —
        // unlike a fall, which stays fully intact. This is what lets a
        // puddle thin out and disappear after a handful of flat hops
        // instead of persisting at a fixed volume forever.
        int dissipated = amount - (int) (amount * EngineSetting.LIQUID_SPREAD_RETENTION_RATIO);
        int spreadable = amount - dissipated;

        if (spreadable < EngineSetting.LIQUID_LEVEL_MIN_PERSIST)
            return 0;

        // Never offer more recipients than the spreadable amount can give
        // EngineSetting.LIQUID_LEVEL_MIN_PERSIST each — an even split
        // across every open neighbor would otherwise floor to zero long
        // before the source runs dry, freezing the puddle in place
        // permanently. This is the fix for the stall.
        int maxRecipients = spreadable / EngineSetting.LIQUID_LEVEL_MIN_PERSIST;
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

            targetSubChunk.getBlockPaletteHandle().setBlock(targetPacked, blockID);
            targetSubChunk.getLiquidLevelPaletteHandle().setBlock(targetPacked, (short) (existingLevel + transfer));

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
     * liquid neighbor already at LIQUID_LEVEL_MAX is treated as solid and
     * the cell directly above it is tried instead — plain air only, no
     * further stacking. Returns -1 when the direction is fully blocked.
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
     * Called only when a single destination cell can't accept a valid
     * fall transfer on its own — the offered amount would seed a
     * brand-new puddle below EngineSetting.LIQUID_LEVEL_MIN_PERSIST.
     * Rather than always discarding that amount, this walks the
     * connected pocket the destination sits in (see probeBasin()) and
     * either distributes the whole amount across that pocket when it can
     * genuinely hold it, or dissolves the amount entirely when the
     * pocket can't. The source always ends up spent either way — a
     * container is either capable or the water is gone, never left
     * half-committed.
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
     * Bounded flood fill of the air/open-same-liquid pocket that
     * startPacked sits in, walking the four cardinal directions plus
     * down — never up, since a pocket's capacity is what it can hold
     * under and beside it, not the open air above it. Stays entirely
     * within subChunk's own local coordinate space: touching that
     * subchunk's edge, or needing more than
     * EngineSetting.LIQUID_BASIN_SCAN_LIMIT cells to fully enclose, both
     * mean the pocket can't be bounded cheaply, and the probe reports
     * failure. On success, basinCells/basinCellCount/basinTotalCapacity
     * describe exactly what was found for fillBasin() to use.
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
     * Distributes amount across every cell probeBasin() just found, in
     * the order they were discovered, filling each to its own capacity
     * before moving to the next. Only ever called once basinTotalCapacity
     * has already been confirmed sufficient, so this always fully spends
     * amount.
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

            subChunk.getBlockPaletteHandle().setBlock(cellPacked, blockID);
            subChunk.getLiquidLevelPaletteHandle().setBlock(cellPacked, (short) (existingLevel + transfer));

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