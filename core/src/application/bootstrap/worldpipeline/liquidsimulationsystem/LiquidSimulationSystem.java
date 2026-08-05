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
     * send downward, split evenly across every open cardinal direction. A
     * destination that would land below EngineSetting.LIQUID_LEVEL_MIN_PERSIST
     * is discarded rather than deposited — this is the sole mechanism by
     * which liquid disappears, and is what makes topping off a large body
     * from a small one fail by design. A same-liquid neighbor already at
     * EngineSetting.LIQUID_LEVEL_MAX is treated as solid for this step,
     * redirecting that share one cell upward instead, mirroring water
     * overflowing a full container. Every cell is visited at most once per
     * call as a source, so a column falls exactly one step per flow tick
     * rather than cascading the whole way down in one pass. Mutations are
     * applied directly against the live palettes — this runs on the main
     * thread only, the same as block placement and breaking, so no
     * additional synchronization is required. Chunks and subchunks touched
     * beyond the one passed to flow() are collected for the caller to
     * rebuild and re-register with the renderer.
     */

    private static final Direction3Vector[] CARDINAL_DIRECTIONS = {
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
    private int[] candidatePacked;
    private ChunkInstance[] candidateChunk;
    private SubChunkInstance[] candidateSubChunk;

    // Scratch — neighbor/target resolution output
    private ChunkInstance scratchNeighborChunk;
    private SubChunkInstance scratchNeighborSubChunk;
    private ChunkInstance scratchTargetChunk;
    private SubChunkInstance scratchTargetSubChunk;

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
            return amount - transfer;

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

        int share = amount / candidateCount;

        if (share <= 0)
            return amount;

        int remaining = amount;

        for (int i = 0; i < candidateCount; i++) {

            int targetPacked = candidatePacked[i];
            SubChunkInstance targetSubChunk = candidateSubChunk[i];
            ChunkInstance targetChunk = candidateChunk[i];

            short existingLevel = targetSubChunk.getLiquidLevelPaletteHandle().getBlock(targetPacked);
            int capacity = EngineSetting.LIQUID_LEVEL_MAX - existingLevel;
            int transfer = Math.min(share, capacity);

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

        return remaining;
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