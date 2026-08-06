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
     * Advances one subchunk's liquid by a single step: every source cell first
     * tries to fall straight down, then — if it still holds any amount —
     * resolves against its local basin, a bounded flood fill (air or same
     * liquid only, never upward) of the pocket it sits in. The basin's total
     * consistency, including everything already inside it, is split evenly
     * across every cell in that pocket once the average clears
     * EngineSetting.LIQUID_BASIN_FILL_THRESHOLD; otherwise the whole pocket
     * dissolves. Falling never costs consistency — settling into or
     * redistributing across a basin is the only place levels change other
     * than by moving downward. Chunks and subchunks touched beyond the one
     * passed to flow() are collected for the caller to rebuild and
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

    // Scratch — bounded basin flood fill, sized to
    // EngineSetting.LIQUID_BASIN_SCAN_LIMIT
    private int[] basinQueue;
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

        belowSubChunk.setBlock(belowPacked, blockID);
        belowSubChunk.setLiquidLevel(belowPacked, (short) (existingLevel + transfer));

        if (belowSubChunk == subChunkInstance)
            processed[ChunkCoordinate3Int.getIndex(belowPacked)] = true;

        markTouched(belowChunk, belowSubChunk);

        return amount - transfer;
    }

    // Basin Resolution \\

    /*
     * Floods the pocket sourcePacked sits in, sums its total consistency
     * (incomingAmount plus every same-liquid cell already inside the pocket),
     * and either writes an even split across every cell in the pocket or
     * clears the whole pocket to air, depending on whether the per-cell
     * average clears EngineSetting.LIQUID_BASIN_FILL_THRESHOLD.
     */
    private boolean resolveBasin(int sourcePacked, short blockID, int incomingAmount) {

        int cellCount = floodFillBasin(sourcePacked, blockID);

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
     * Bounded BFS through air/same-liquid cells only, never upward, capped at
     * EngineSetting.LIQUID_BASIN_SCAN_LIMIT cells and never leaving the
     * current subchunk — a chunk-local edge simply blocks that direction
     * rather than aborting the scan.
     */
    private int floodFillBasin(int sourcePacked, short blockID) {

        BlockPaletteHandle blocks = currentSubChunk.getBlockPaletteHandle();

        int head = 0;
        int tail = 0;
        int cellCount = 0;

        basinQueue[tail++] = sourcePacked;
        markBasinVisited(sourcePacked);

        while (head < tail) {

            int currentPacked = basinQueue[head++];
            basinCells[cellCount++] = currentPacked;

            for (int d = 0; d < BASIN_FLOOD_DIRECTIONS.length && tail < EngineSetting.LIQUID_BASIN_SCAN_LIMIT; d++) {

                int neighborPacked = ChunkCoordinate3Int.getNeighbor(currentPacked, BASIN_FLOOD_DIRECTIONS[d]);

                if (neighborPacked == -1 || isBasinVisited(neighborPacked))
                    continue;

                short neighborBlockID = blocks.getBlock(neighborPacked);

                if (neighborBlockID != airBlockId && neighborBlockID != blockID)
                    continue;

                markBasinVisited(neighborPacked);
                basinQueue[tail++] = neighborPacked;
            }
        }

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