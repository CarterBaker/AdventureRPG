package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.liquidmanager.LiquidManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.TickQuadrant;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class LiquidTickBranch extends BranchPackage {

    /*
     * Schedules liquid flow over each grid's IMMEDIATE range in quadrant
     * cycles, visiting only subchunks with active liquid. Each chunk ticks
     * under its own lock. Each firing also re-levels nearby ocean chunks whose
     * water lags the live tide. Touched subchunks are queued on
     * SubChunkRebuildBranch rather than rebuilt in the tick: flowing water
     * ahead of the queue, since the player watches it move, and the tide
     * behind it, so a tide sweeping many chunks never stalls a frame.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private LiquidManager liquidManager;
    private TideManager tideManager;
    private SubChunkRebuildBranch subChunkRebuildBranch;

    // Settings
    private int intervalFrames;
    private float tideRangeSquared;

    // State
    private int frameCounter;
    private int quadrantCursor;
    private long[] lastQuadrantTickMillis;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.intervalFrames = EngineSetting.LIQUID_TICK_INTERVAL_FRAMES;
        this.tideRangeSquared = EngineSetting.OCEAN_TIDE_RANGE_CHUNKS * EngineSetting.OCEAN_TIDE_RANGE_CHUNKS;

        // State
        this.frameCounter = EngineSetting.LIQUID_TICK_PHASE_FRAMES;
        this.quadrantCursor = 0;
        this.lastQuadrantTickMillis = new long[TickQuadrant.VALUES.length];
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.liquidManager = get(LiquidManager.class);
        this.tideManager = get(TideManager.class);
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

        liquidManager.retryDeferredWakes();
        tickTide();

        TickQuadrant quadrant = TickQuadrant.VALUES[quadrantCursor];
        quadrantCursor = (quadrantCursor + 1) % TickQuadrant.VALUES.length;

        long now = internal.getTime();
        long lastTick = lastQuadrantTickMillis[quadrant.ordinal()];
        lastQuadrantTickMillis[quadrant.ordinal()] = now;

        if (lastTick == 0L)
            return;

        float delta = (now - lastTick) / EngineSetting.MILLIS_PER_SECOND_FLOAT;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            tickGrid((GridInstance) elements[i], delta, quadrant);
    }

    private void tickGrid(GridInstance grid, float delta, TickQuadrant quadrant) {

        long[] loadOrder = grid.getLoadOrder();
        int immediateSlotCount = grid.getImmediateSlotCount();
        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();

        for (int i = 0; i < immediateSlotCount; i++) {

            long chunkCoordinate = grid.getChunkCoordinateForSlot(loadOrder[i]);

            if (TickQuadrant.fromChunkCoordinate(chunkCoordinate) != quadrant)
                continue;

            ChunkInstance chunk = activeChunks.get(chunkCoordinate);

            if (chunk != null)
                tickChunk(chunk, delta);
        }
    }

    private void tickChunk(ChunkInstance chunk, float delta) {

        ChunkDataSyncContainer syncContainer = chunk.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        liquidManager.beginChunkTick(chunk);

        try {
            SubChunkInstance[] subChunks = chunk.getSubChunks();

            for (int i = 0; i < subChunks.length; i++)
                tickSubChunk(chunk, subChunks[i], delta);

            queueTouched(true);
        } finally {
            liquidManager.endChunkTick();
            syncContainer.release();
        }
    }

    private void tickSubChunk(ChunkInstance chunk, SubChunkInstance subChunk, float delta) {

        if (!subChunk.hasActiveLiquid())
            return;

        subChunk.addLiquidFlowTime(delta);

        if (subChunk.getLiquidFlowAccumulator() < resolveFlowInterval(subChunk))
            return;

        subChunk.resetLiquidFlowAccumulator();
        liquidManager.flow(chunk, subChunk);
    }

    // Tide \\

    private void tickTide() {

        int surfaceLevels = tideManager.getSurfaceLevels();
        int budget = EngineSetting.OCEAN_TIDE_CHUNKS_PER_TICK;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size && budget > 0; i++)
            budget = tideGrid((GridInstance) elements[i], surfaceLevels, budget);
    }

    private int tideGrid(GridInstance grid, int surfaceLevels, int budget) {

        long[] loadOrder = grid.getLoadOrder();
        int totalSlots = grid.getTotalSlots();
        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();

        for (int i = 0; i < totalSlots && budget > 0; i++) {

            GridSlotHandle slot = grid.getGridSlot(loadOrder[i]);

            if (slot.getChunkDistanceFromCenter() > tideRangeSquared)
                break;

            ChunkInstance chunk = activeChunks.get(grid.getChunkCoordinateForSlot(loadOrder[i]));

            if (chunk != null && chunk.getTideSurfaceLevels() != surfaceLevels && tideChunk(chunk, surfaceLevels))
                budget--;
        }

        return budget;
    }

    private boolean tideChunk(ChunkInstance chunk, int surfaceLevels) {

        ChunkDataSyncContainer syncContainer = chunk.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return false;

        boolean tidal = false;

        liquidManager.beginChunkTick(chunk);

        try {
            if (syncContainer.getData()[ChunkData.GENERATION_DATA.index]
                    && chunk.getTideSurfaceLevels() != surfaceLevels) {
                tidal = liquidManager.tide(chunk, surfaceLevels);
                queueTouched(false);
            }
        } finally {
            liquidManager.endChunkTick();
            syncContainer.release();
        }

        return tidal;
    }

    // Rebuild \\

    // Every subchunk the chunk tick touched queued for a rebuild — urgent for flow, behind the rest for the tide
    private void queueTouched(boolean urgent) {

        ObjectArrayList<ChunkInstance> touchedChunks = liquidManager.getTouchedChunks();
        IntArrayList touchedSubChunkY = liquidManager.getTouchedSubChunkY();

        for (int i = 0; i < touchedChunks.size(); i++) {

            long subChunkBit = SubChunkRebuildBranch.toSubChunkBit(touchedSubChunkY.getInt(i));

            if (urgent)
                subChunkRebuildBranch.queueUrgentRebuild(touchedChunks.get(i), subChunkBit);
            else
                subChunkRebuildBranch.queueRebuild(touchedChunks.get(i), subChunkBit);
        }
    }

    private float resolveFlowInterval(SubChunkInstance subChunk) {

        ShortOpenHashSet liquidBlockIDs = subChunk.getContainedLiquidBlockIDs();
        float fastestInterval = Float.MAX_VALUE;

        ShortIterator iterator = liquidBlockIDs.iterator();

        while (iterator.hasNext()) {

            BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(iterator.nextShort());

            float interval = Math.max(
                    EngineSetting.LIQUID_FLOW_INTERVAL_MIN_SECONDS,
                    Math.min(
                            EngineSetting.LIQUID_FLOW_INTERVAL_MAX_SECONDS,
                            blockHandle.getViscosity() * EngineSetting.LIQUID_VISCOSITY_TO_FLOW_SECONDS));

            if (interval < fastestInterval)
                fastestInterval = interval;
        }

        return fastestInterval;
    }
}