package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.covering.CoveringHandle;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.ChunkCoordinateUtility;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.TickQuadrant;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CoverageTickBranch extends BranchPackage {

    /*
     * Grows the coverings of each grid's IMMEDIATE range in quadrant cycles,
     * visiting only subchunks that hold coverage and drawing a fixed number
     * of their cells each firing, so growth costs the same however much
     * ground is covered. A drawn cell whose block can no longer host its
     * covering goes bare, one that needs an open top and has a block over it
     * recedes a level, and one that needs moisture and finds no liquid near
     * it, a level above or below included, within its own chunk stands still.
     * Otherwise it may grow a level, and once it reaches its spread level it
     * may seed a bare host beside it, a level up or down included, at the
     * first level. A seed crossing into a neighbouring chunk only lands when
     * that chunk's lock is free. Every chunk ticks under its own lock. A
     * change only marks its subchunk for a rebuild when the cell has a face
     * the mesher could draw the covering on, since a cell boxed in by whole
     * blocks draws nothing either way. Marked subchunks are queued on
     * SubChunkRebuildBranch, which rebuilds them within its own budget, so a
     * tick never stalls a frame however much ground it grows.
     */

    private static final Direction3Vector[] LATERAL_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private CoveringManager coveringManager;
    private SubChunkRebuildBranch subChunkRebuildBranch;

    // Settings
    private int intervalFrames;
    private int samplesPerSubChunk;
    private int chunkSize;
    private int worldHeight;
    private int generationDataIndex;

    // State
    private int frameCounter;
    private int quadrantCursor;
    private long passCount;

    // Scratch — the subchunks a tick touched in the chunk it ticks, and in a neighbour a seed crossed into
    private boolean[] touchedSubChunks;
    private boolean[] touchedNeighborSubChunks;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.intervalFrames = EngineSetting.COVERAGE_TICK_INTERVAL_FRAMES;
        this.samplesPerSubChunk = EngineSetting.COVERAGE_TICK_SAMPLES_PER_SUBCHUNK;
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
        this.generationDataIndex = ChunkData.GENERATION_DATA.index;

        // State
        this.frameCounter = EngineSetting.COVERAGE_TICK_PHASE_FRAMES;
        this.quadrantCursor = 0;
        this.passCount = 0L;

        // Scratch
        this.touchedSubChunks = new boolean[worldHeight];
        this.touchedNeighborSubChunks = new boolean[worldHeight];
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.coveringManager = get(CoveringManager.class);
        this.subChunkRebuildBranch = get(SubChunkRebuildBranch.class);
    }

    // Schedule \\

    public boolean advance() {

        frameCounter++;

        if (frameCounter < intervalFrames)
            return false;

        frameCounter = 0;
        return true;
    }

    // Tick \\

    public void tick() {

        TickQuadrant quadrant = TickQuadrant.VALUES[quadrantCursor];
        quadrantCursor = (quadrantCursor + 1) % TickQuadrant.VALUES.length;
        passCount++;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            tickGrid((GridInstance) elements[i], quadrant);
    }

    private void tickGrid(GridInstance grid, TickQuadrant quadrant) {

        long[] loadOrder = grid.getLoadOrder();
        int immediateSlotCount = grid.getImmediateSlotCount();
        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();

        for (int i = 0; i < immediateSlotCount; i++) {

            long chunkCoordinate = grid.getChunkCoordinateForSlot(loadOrder[i]);

            if (TickQuadrant.fromChunkCoordinate(chunkCoordinate) != quadrant)
                continue;

            ChunkInstance chunk = activeChunks.get(chunkCoordinate);

            if (chunk != null)
                tickChunk(chunk);
        }
    }

    private void tickChunk(ChunkInstance chunk) {

        ChunkDataSyncContainer syncContainer = chunk.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {

            if (!syncContainer.getData()[generationDataIndex])
                return;

            SubChunkInstance[] subChunks = chunk.getSubChunks();

            for (int subChunkY = 0; subChunkY < subChunks.length; subChunkY++)
                if (subChunks[subChunkY].hasCoverage())
                    tickSubChunk(chunk, subChunkY);
        } finally {
            syncContainer.release();
        }

        queueTouched(chunk, touchedSubChunks);
    }

    private void tickSubChunk(ChunkInstance chunk, int subChunkY) {

        SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);
        int chunkSalt = (int) (chunk.getCoordinate() ^ (chunk.getCoordinate() >>> Integer.SIZE));

        for (int sample = 0; sample < samplesPerSubChunk; sample++) {

            float roll = roll(EngineSetting.COVERAGE_SAMPLE_SALT, sample, chunkSalt + subChunkY);
            int index = (int) (roll * ChunkCoordinateUtility.BLOCK_COORDINATE_COUNT);
            int packedXYZ = ChunkCoordinateUtility.getBlockCoordinate(index);
            short coverage = subChunk.getCoverage(packedXYZ);

            if (CoverageUtility.isCovered(coverage))
                tickCell(chunk, subChunkY, packedXYZ, coverage, chunkSalt);
        }
    }

    // Cell \\

    private void tickCell(ChunkInstance chunk, int subChunkY, int packedXYZ, short coverage, int chunkSalt) {

        SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);
        CoveringHandle coveringHandle = coveringManager.getCoveringHandleFromCoverage(coverage);
        int worldY = subChunkY * chunkSize + Coordinate3Int.unpackY(packedXYZ);
        int localX = Coordinate3Int.unpackX(packedXYZ);
        int localZ = Coordinate3Int.unpackZ(packedXYZ);
        int cellIndex = ChunkCoordinateUtility.getIndex(packedXYZ);
        int cellSalt = chunkSalt + worldY;

        if (!coveringHandle.canHost(subChunk.getBlock(packedXYZ))) {
            writeCoverage(chunk, subChunkY, packedXYZ, CoverageUtility.NONE, coveringHandle, touchedSubChunks);
            return;
        }

        if (coveringHandle.requiresOpenTop() && isSmothered(chunk, localX, worldY, localZ)) {
            writeCoverage(
                    chunk, subChunkY, packedXYZ, CoverageUtility.addLevels(coverage, -1), coveringHandle,
                    touchedSubChunks);
            return;
        }

        if (coveringHandle.requiresMoisture()
                && !hasMoisture(chunk, localX, worldY, localZ, coveringHandle.getMoistureRadius()))
            return;

        if (!CoverageUtility.isFull(coverage)
                && roll(EngineSetting.COVERAGE_GROWTH_SALT, cellIndex, cellSalt) < coveringHandle.getGrowthChance())
            writeCoverage(
                    chunk, subChunkY, packedXYZ, CoverageUtility.addLevels(coverage, 1), coveringHandle,
                    touchedSubChunks);

        if (CoverageUtility.getLevel(coverage) >= coveringHandle.getSpreadLevel()
                && roll(EngineSetting.COVERAGE_SPREAD_SALT, cellIndex, cellSalt) < coveringHandle.getSpreadChance())
            spread(chunk, packedXYZ, worldY, coveringHandle, cellIndex, cellSalt);
    }

    // A block of any kind over the cell, liquid included, shuts out what an open top needs
    private boolean isSmothered(ChunkInstance chunk, int localX, int worldY, int localZ) {

        if (worldY + 1 >= worldHeight * chunkSize)
            return false;

        return blockManager.getGeometryFromBlockID(readBlock(chunk, localX, worldY + 1, localZ))
                != DynamicGeometryType.NONE;
    }

    // Liquid a level below the cell up to a level above it, within the radius and the cell's own chunk
    private boolean hasMoisture(ChunkInstance chunk, int localX, int worldY, int localZ, int radius) {

        int minX = Math.max(localX - radius, 0);
        int maxX = Math.min(localX + radius, chunkSize - 1);
        int minZ = Math.max(localZ - radius, 0);
        int maxZ = Math.min(localZ + radius, chunkSize - 1);

        for (int y = Math.max(worldY - 1, 0); y <= Math.min(worldY + 1, worldHeight * chunkSize - 1); y++)
            for (int z = minZ; z <= maxZ; z++)
                for (int x = minX; x <= maxX; x++)
                    if (blockManager.getGeometryFromBlockID(readBlock(chunk, x, y, z)) == DynamicGeometryType.LIQUID)
                        return true;

        return false;
    }

    // Spread \\

    // One bare host beside the cell, a level up, level or down, seeded at the first level
    private void spread(
            ChunkInstance chunk,
            int packedXYZ,
            int worldY,
            CoveringHandle coveringHandle,
            int cellIndex,
            int cellSalt) {

        float directionRoll = roll(EngineSetting.COVERAGE_SPREAD_DIRECTION_SALT, cellIndex, cellSalt);
        float heightRoll = roll(EngineSetting.COVERAGE_SPREAD_HEIGHT_SALT, cellIndex, cellSalt);
        Direction3Vector direction = LATERAL_DIRECTIONS[(int) (directionRoll * LATERAL_DIRECTIONS.length)];
        int reach = EngineSetting.COVERAGE_SPREAD_REACH_Y;
        int targetY = worldY + (int) (heightRoll * (reach * 2 + 1)) - reach;

        if (targetY < 0 || targetY >= worldHeight * chunkSize)
            return;

        int lateralXYZ = ChunkCoordinateUtility.getNeighborAndWrap(packedXYZ, direction);
        short seed = CoverageUtility.pack(coveringHandle.getCoveringID(), 1);

        if (!ChunkCoordinateUtility.isAtEdge(packedXYZ, direction)) {
            seedCell(chunk, lateralXYZ, targetY, coveringHandle, seed, touchedSubChunks);
            return;
        }

        ChunkInstance neighbor = chunk.getChunkNeighbors().getNeighborChunk(direction.to2D().index);

        if (neighbor != null)
            seedNeighbor(neighbor, lateralXYZ, targetY, coveringHandle, seed);
    }

    // A seed landing across the chunk border, only when that chunk is free and generated
    private void seedNeighbor(
            ChunkInstance neighbor,
            int lateralXYZ,
            int targetY,
            CoveringHandle coveringHandle,
            short seed) {

        ChunkDataSyncContainer syncContainer = neighbor.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {

            if (!syncContainer.getData()[generationDataIndex])
                return;

            seedCell(neighbor, lateralXYZ, targetY, coveringHandle, seed, touchedNeighborSubChunks);
        } finally {
            syncContainer.release();
        }

        queueTouched(neighbor, touchedNeighborSubChunks);
    }

    private void seedCell(
            ChunkInstance chunk,
            int lateralXYZ,
            int targetY,
            CoveringHandle coveringHandle,
            short seed,
            boolean[] touched) {

        int subChunkY = targetY / chunkSize;
        int localX = Coordinate3Int.unpackX(lateralXYZ);
        int localZ = Coordinate3Int.unpackZ(lateralXYZ);
        int targetXYZ = Coordinate3Int.pack(localX, targetY % chunkSize, localZ);
        SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);

        if (CoverageUtility.isCovered(subChunk.getCoverage(targetXYZ))
                || !coveringHandle.canHost(subChunk.getBlock(targetXYZ)))
            return;

        if (coveringHandle.requiresOpenTop() && isSmothered(chunk, localX, targetY, localZ))
            return;

        writeCoverage(chunk, subChunkY, targetXYZ, seed, coveringHandle, touched);
    }

    // Write \\

    // The cell's coverage written, its subchunk marked for a rebuild only when the change can be seen
    private void writeCoverage(
            ChunkInstance chunk,
            int subChunkY,
            int packedXYZ,
            short coverage,
            CoveringHandle coveringHandle,
            boolean[] touched) {

        chunk.getSubChunk(subChunkY).setCoverage(packedXYZ, coverage);

        if (showsCoverage(chunk, subChunkY, packedXYZ, coveringHandle))
            touched[subChunkY] = true;
    }

    // Visibility \\

    // True when any face the covering is drawn on could be open — its upper face, and its upright faces when the
    // covering has side tiles. A face against a whole FULL block is never drawn, and one at the chunk's edge is
    // taken as open, since the cell beside it lies under another chunk's lock
    private boolean showsCoverage(ChunkInstance chunk, int subChunkY, int packedXYZ, CoveringHandle coveringHandle) {

        int localX = Coordinate3Int.unpackX(packedXYZ);
        int localZ = Coordinate3Int.unpackZ(packedXYZ);
        int worldY = subChunkY * chunkSize + Coordinate3Int.unpackY(packedXYZ);

        if (!isWholeFull(chunk, localX, worldY, localZ))
            return true;

        if (worldY + 1 < worldHeight * chunkSize && !isWholeFull(chunk, localX, worldY + 1, localZ))
            return true;

        if (!coveringHandle.hasSide())
            return false;

        for (Direction3Vector direction : LATERAL_DIRECTIONS) {

            if (ChunkCoordinateUtility.isAtEdge(packedXYZ, direction))
                return true;

            int lateralXYZ = ChunkCoordinateUtility.getNeighborAndWrap(packedXYZ, direction);

            if (!isWholeFull(chunk, Coordinate3Int.unpackX(lateralXYZ), worldY, Coordinate3Int.unpackZ(lateralXYZ)))
                return true;
        }

        return false;
    }

    private boolean isWholeFull(ChunkInstance chunk, int localX, int worldY, int localZ) {

        SubChunkInstance subChunk = chunk.getSubChunk(worldY / chunkSize);
        int packedXYZ = Coordinate3Int.pack(localX, worldY % chunkSize, localZ);

        return blockManager.getGeometryFromBlockID(subChunk.getBlock(packedXYZ)) == DynamicGeometryType.FULL
                && !SubBlockUtility.isSubdivided(subChunk.getSubBlockMask(packedXYZ));
    }

    // Rebuild \\

    // Every subchunk the tick touched queued against its chunk, the touched flags cleared for the next tick
    private void queueTouched(ChunkInstance chunk, boolean[] touched) {

        long pending = 0L;

        for (int subChunkY = 0; subChunkY < worldHeight; subChunkY++) {

            if (!touched[subChunkY])
                continue;

            touched[subChunkY] = false;
            pending |= SubChunkRebuildBranch.toSubChunkBit(subChunkY);
        }

        if (pending != 0L)
            subChunkRebuildBranch.queueRebuild(chunk, pending);
    }

    // Utility \\

    private float roll(long salt, int a, int b) {
        return BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(passCount ^ salt, a, b));
    }

    private short readBlock(ChunkInstance chunk, int localX, int worldY, int localZ) {
        return chunk.getSubChunk(worldY / chunkSize)
                .getBlock(Coordinate3Int.pack(localX, worldY % chunkSize, localZ));
    }
}
