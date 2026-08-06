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
     * Advances one subchunk's liquid by a single simulation step. Every
     * liquid cell first tries to fall straight down — free, and the source
     * cell is fully reconciled the instant any of it transfers below, so a
     * fall can never duplicate water. Whatever cannot fall resolves against
     * the local basin it rests in: a cost-bounded 0-1 BFS flood (through air
     * or the same liquid, never upward) capped at
     * EngineSetting.LIQUID_BASIN_SCAN_LIMIT cells. A horizontal hop is free
     * whenever it steps onto a cell that itself continues falling — water
     * spilling over a ledge costs nothing — and otherwise costs
     * EngineSetting.LIQUID_HORIZONTAL_MOVE_CONSISTENCY_LOSS, since that is
     * genuine sideways spread across level ground. A cell the water cannot
     * afford to reach is excluded from the basin outright and never expanded
     * from. The basin's total consistency — the settling amount plus every
     * same-liquid cell already inside the reached pocket — is split evenly
     * across every reached cell once the average clears
     * EngineSetting.LIQUID_BASIN_FILL_THRESHOLD; otherwise the whole
     * reachable pocket dissolves. Chunks and subchunks touched beyond the
     * one passed to flow() are collected for the caller to rebuild and
     * re-register with the renderer.
     */

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

    // Scratch — cost-bounded 0-1 BFS basin discovery, double ended so free
    // hops drain before costlier ones queued at the same distance
    private int[] basinDequePacked;
    private int[] basinDequeCost;
    private int basinDequeMid;
    private int[] basinCells;
    private boolean[] basinVisited;
    private int[] basinVisitedTouched;
    private int basinVisitedCount;

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
        this.basinDequeCost = new int[EngineSetting.LIQUID_BASIN_SCAN_LIMIT * 2];
        this.basinCells = new int[EngineSetting.LIQUID_BASIN_SCAN_LIMIT];

        // Sized to the subchunk's full cell count rather than
        // scanLimit*directions — every visit is unique, so that is the only
        // bound that is actually guaranteed never to overflow.
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

            if (resolveBasin(packed, blockID, remaining))
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

        ChunkInstance belowChunk = scratchNeighborChunk;
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

        int transfer = Math.min(amount, capacity);
        int remaining = amount - transfer;

        belowSubChunk.setBlock(belowPacked, blockID);
        belowSubChunk.setLiquidLevel(belowPacked, (short) (existingLevel + transfer));

        if (belowSubChunk == subChunkInstance)
            processed[ChunkCoordinate3Int.getIndex(belowPacked)] = true;

        if (remaining <= 0) {
            subChunkInstance.setBlock(packed, airBlockId);
            subChunkInstance.setLiquidLevel(packed, EngineSetting.LIQUID_LEVEL_EMPTY);
        } else {
            subChunkInstance.setLiquidLevel(packed, (short) remaining);
        }

        markTouched(belowChunk, belowSubChunk);

        return remaining;
    }

    // Basin Resolution \\

    private boolean resolveBasin(int sourcePacked, short blockID, int incomingAmount) {

        int cellCount = floodFillBasin(sourcePacked, blockID, incomingAmount);

        BlockPaletteHandle blocks = currentSubChunk.getBlockPaletteHandle();
        BlockPaletteHandle levels = currentSubChunk.getLiquidLevelPaletteHandle();

        int total = incomingAmount;

        for (int i = 0; i < cellCount; i++) {
            int cellPacked = basinCells[i];
            if (cellPacked != sourcePacked && blocks.getBlock(cellPacked) == blockID)
                total += levels.getBlock(cellPacked);
        }

        boolean settles = total >= EngineSetting.LIQUID_BASIN_FILL_THRESHOLD * cellCount;
        int share = settles ? total / cellCount : 0;
        int remainder = settles ? total % cellCount : 0;
        boolean changed = false;

        for (int i = 0; i < cellCount; i++) {

            int cellPacked = basinCells[i];
            boolean isSource = cellPacked == sourcePacked;
            int oldLevel = isSource
                    ? incomingAmount
                    : (blocks.getBlock(cellPacked) == blockID ? levels.getBlock(cellPacked) : 0);
            int newLevel = settles ? share + (i < remainder ? 1 : 0) : 0;

            processed[ChunkCoordinate3Int.getIndex(cellPacked)] = true;

            if (newLevel == oldLevel)
                continue;

            if (newLevel <= 0) {
                currentSubChunk.setBlock(cellPacked, airBlockId);
                currentSubChunk.setLiquidLevel(cellPacked, EngineSetting.LIQUID_LEVEL_EMPTY);
            } else {
                currentSubChunk.setBlock(cellPacked, blockID);
                currentSubChunk.setLiquidLevel(cellPacked, (short) newLevel);
            }

            changed = true;
        }

        return changed;
    }

    /*
     * Cost-bounded 0-1 BFS through air/same-liquid cells only, never upward,
     * capped at EngineSetting.LIQUID_BASIN_SCAN_LIMIT cells and never leaving
     * the current subchunk. A horizontal hop is free when it opens onto a
     * cell that itself continues falling (see opensDownward) — a genuine
     * spill over a ledge — and otherwise costs
     * EngineSetting.LIQUID_HORIZONTAL_MOVE_CONSISTENCY_LOSS. A cell whose
     * cumulative cost would meet or exceed incomingAmount is outside this
     * pour's reach and is excluded, and never expanded from.
     */
    private int floodFillBasin(int sourcePacked, short blockID, int incomingAmount) {

        BlockPaletteHandle blocks = currentSubChunk.getBlockPaletteHandle();

        int front = basinDequeMid;
        int back = basinDequeMid;
        int enqueued = 1;
        int cellCount = 0;

        basinDequePacked[back] = sourcePacked;
        basinDequeCost[back] = 0;
        back++;
        markBasinVisited(sourcePacked);

        while (front < back) {

            int currentPacked = basinDequePacked[front];
            int currentCost = basinDequeCost[front];
            front++;

            basinCells[cellCount++] = currentPacked;

            for (int d = 0; d < BASIN_FLOOD_DIRECTIONS.length
                    && enqueued < EngineSetting.LIQUID_BASIN_SCAN_LIMIT; d++) {

                Direction3Vector direction = BASIN_FLOOD_DIRECTIONS[d];
                int neighborPacked = ChunkCoordinate3Int.getNeighbor(currentPacked, direction);

                if (neighborPacked == -1 || isBasinVisited(neighborPacked))
                    continue;

                boolean free = direction == Direction3Vector.DOWN
                        || opensDownward(neighborPacked, blockID, blocks);

                int neighborCost = free
                        ? currentCost
                        : currentCost + EngineSetting.LIQUID_HORIZONTAL_MOVE_CONSISTENCY_LOSS;

                if (incomingAmount - neighborCost <= 0) {
                    markBasinVisited(neighborPacked);
                    continue;
                }

                short neighborBlockID = blocks.getBlock(neighborPacked);

                if (neighborBlockID != airBlockId && neighborBlockID != blockID)
                    continue;

                markBasinVisited(neighborPacked);
                enqueued++;

                if (free) {
                    front--;
                    basinDequePacked[front] = neighborPacked;
                    basinDequeCost[front] = neighborCost;
                } else {
                    basinDequePacked[back] = neighborPacked;
                    basinDequeCost[back] = neighborCost;
                    back++;
                }
            }
        }

        clearBasinVisited();
        return cellCount;
    }

    // True when packed's own downward neighbor is open (air or the same
    // liquid) within this subchunk — the lateral hop landing here is a
    // spill over a ledge rather than sideways spread, so it costs nothing.
    private boolean opensDownward(int packed, short blockID, BlockPaletteHandle blocks) {

        int belowPacked = ChunkCoordinate3Int.getNeighbor(packed, Direction3Vector.DOWN);

        if (belowPacked == -1)
            return false;

        short belowBlockID = blocks.getBlock(belowPacked);

        return belowBlockID == airBlockId || belowBlockID == blockID;
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