package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkDataUtility;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.fluidsimulationsystem.FluidSimulationSystem;
import application.bootstrap.worldpipeline.grid.GridInstance;
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
     * Drives per-block liquid flow, scoped to each grid's IMMEDIATE range only
     * (GridInstance.getImmediateSlotCount()) and quadrant-cycled so a full
     * sweep happens every four firings rather than all at once. A bounded
     * connectivity scan (FluidSimulationSystem.isConnectedBodyPermanent) runs
     * once per unstable subchunk to check whether it belongs to a body large
     * enough to be marked permanent, so that scan is never repeated for the
     * same body — permanent only skips that scan, it does not skip flow.
     * Flow still runs every interval regardless of permanence, and the
     * subchunk only goes stable (skipped entirely) once a pass produces no
     * change, exactly like a small body — the difference is that a permanent
     * body never dissolves for lack of room (see FluidSimulationSystem), so a
     * touched ocean settles back to stable water instead of eroding away. Any
     * real change rebuilds only the affected subchunk's own geometry inline,
     * then cascade-clears the owning chunk's MERGE_DATA so the existing
     * async, GPU-upload-budgeted ChunkQueueManager pipeline handles the
     * re-merge and re-upload on its own schedule instead of that work ever
     * running synchronously here.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;

    // Branches
    private FluidSimulationSystem fluidSimulationSystem;

    // Settings
    private int intervalFrames;

    // State
    private int frameCounter;
    private int quadrantCursor;
    private long[] lastQuadrantTickMillis;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.intervalFrames = EngineSetting.LIQUID_TICK_INTERVAL_FRAMES;

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
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
        this.fluidSimulationSystem = get(FluidSimulationSystem.class);
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

        long now = internal.getTime();
        long lastTick = lastQuadrantTickMillis[quadrant.ordinal()];
        lastQuadrantTickMillis[quadrant.ordinal()] = now;

        if (lastTick == 0L)
            return;

        float delta = (now - lastTick) / 1000f;

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

    /*
     * Holds the chunk's own sync lock across every subchunk tick. On any
     * change, cascade-clears MERGE_DATA so the streaming pipeline re-merges
     * and re-uploads this chunk on its own async/budgeted schedule instead
     * of that work happening here. Skips the whole chunk for this pass if
     * the async pipeline currently owns it.
     */
    private void tickChunk(ChunkInstance chunk, float delta) {

        ChunkDataSyncContainer syncContainer = chunk.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        boolean chunkChanged;

        try {
            SubChunkInstance[] subChunks = chunk.getSubChunks();
            chunkChanged = false;

            for (int i = 0; i < subChunks.length; i++)
                if (tickSubChunk(chunk, subChunks[i], delta))
                    chunkChanged = true;

            if (chunkChanged)
                ChunkDataUtility.cascadeClear(ChunkData.MERGE_DATA, syncContainer.getData());
        } finally {
            syncContainer.release();
        }

        if (!chunkChanged)
            return;

        worldStreamManager.invalidateMegaForChunk(chunk.getCoordinate());
    }

    private boolean tickSubChunk(ChunkInstance chunk, SubChunkInstance subChunk, float delta) {

        if (!subChunk.hasBlockType(DynamicGeometryType.LIQUID) || subChunk.isLiquidStable())
            return false;

        subChunk.addLiquidFlowTime(delta);

        float interval = resolveFlowInterval(subChunk);

        if (subChunk.getLiquidFlowAccumulator() < interval)
            return false;

        subChunk.resetLiquidFlowAccumulator();

        if (!subChunk.isLiquidPermanent())
            assessPermanence(chunk, subChunk);

        if (!fluidSimulationSystem.flow(chunk, subChunk)) {
            subChunk.setLiquidStable(true);
            return false;
        }

        rebuildSubChunkGeometry(chunk, (int) subChunk.getCoordinate());
        rebuildTouchedNeighbors(chunk);

        return true;
    }

    /*
     * Runs the bounded connectivity scan for every distinct liquid type this
     * subchunk contains (almost always exactly one), only ever called once
     * per unstable subchunk since the caller skips this entirely once
     * isLiquidPermanent() is already true. Marks the subchunk permanent the
     * moment any one of them turns out to be part of a body meeting
     * LIQUID_PERMANENCE_THRESHOLD — this does not by itself stop flow() from
     * running this tick or any future one.
     */
    private void assessPermanence(ChunkInstance chunk, SubChunkInstance subChunk) {

        ShortOpenHashSet liquidBlockIDs = subChunk.getContainedLiquidBlockIDs();
        ShortIterator iterator = liquidBlockIDs.iterator();

        while (iterator.hasNext()) {

            short liquidBlockID = iterator.nextShort();
            int seedPacked = subChunk.findLiquidCell(liquidBlockID);

            if (seedPacked == -1)
                throwException("Subchunk's tallied liquid block ID has no matching cell in its own palette — "
                        + "geometry tally is out of sync with block storage.");

            if (fluidSimulationSystem.isConnectedBodyPermanent(chunk, subChunk, seedPacked)) {
                subChunk.setLiquidPermanent(true);
                return;
            }
        }
    }

    /*
     * A flow step can write into a subchunk other than the one just ticked —
     * the column below it falling, or a neighbor chunk it spread into — and
     * that subchunk's geometry needs to reflect it. lockedChunk is the chunk
     * tickChunk() already holds the lock for, so its own touched subchunks
     * are rebuilt inline without a second lock cycle; every other touched
     * chunk gets its own tryAcquire and, on success, has its MERGE_DATA
     * cascade-cleared the same way the locked chunk's is.
     */
    private void rebuildTouchedNeighbors(ChunkInstance lockedChunk) {

        ObjectArrayList<ChunkInstance> touchedChunks = fluidSimulationSystem.getTouchedChunks();
        IntArrayList touchedSubChunkY = fluidSimulationSystem.getTouchedSubChunkY();

        for (int i = 0; i < touchedChunks.size(); i++) {

            ChunkInstance touchedChunk = touchedChunks.get(i);
            int subChunkY = touchedSubChunkY.getInt(i);

            if (touchedChunk == lockedChunk) {
                rebuildSubChunkGeometry(touchedChunk, subChunkY);
                continue;
            }

            ChunkDataSyncContainer touchedSync = touchedChunk.getChunkDataSyncContainer();

            if (!touchedSync.tryAcquire())
                continue;

            try {
                rebuildSubChunkGeometry(touchedChunk, subChunkY);
                ChunkDataUtility.cascadeClear(ChunkData.MERGE_DATA, touchedSync.getData());
            } finally {
                touchedSync.release();
            }

            worldStreamManager.invalidateMegaForChunk(touchedChunk.getCoordinate());
        }
    }

    private void rebuildSubChunkGeometry(ChunkInstance targetChunk, int subChunkY) {

        SubChunkInstance touchedSubChunk = targetChunk.getSubChunk(subChunkY);

        touchedSubChunk.getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, targetChunk, subChunkY);
    }

    /*
     * Converts each contained liquid's own viscosity (Pa·s) into the
     * real-seconds interval its geometry is allowed to redraw at, clamped
     * between LIQUID_FLOW_INTERVAL_MIN_SECONDS and _MAX_SECONDS. Returns the
     * fastest interval among every liquid this subchunk contains.
     */
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