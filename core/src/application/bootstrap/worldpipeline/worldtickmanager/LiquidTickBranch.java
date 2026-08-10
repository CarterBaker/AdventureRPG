package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.fluidsimulationsystem.FluidSimulationSystem;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.TickQuadrant;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
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
     * Drives per-block liquid flow. Scoped to each grid's IMMEDIATE range
     * only (GridInstance.getImmediateSlotCount(), a fixed-size prefix of the
     * nearest-first load order) — liquid physics is a "close to the player"
     * concern, not a whole-render-distance one, so cost stays constant
     * regardless of view distance instead of scaling with total loaded
     * chunks. Each firing advances through one of four quadrants (by chunk
     * coordinate parity) within that range, so a full sweep of the immediate
     * area happens every four firings rather than all at once. A subchunk
     * with no liquid, already-settled liquid, or liquid that's still an
     * untouched uniform fill is skipped without any palette access. Any
     * neighboring chunk a flow step touches outside the locked chunk is
     * rebuilt and re-registered the same way, guarded by its own
     * ChunkDataSyncContainer since the async chunk pipeline can be rebuilding
     * it at the same moment.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;

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
        this.worldRenderManager = get(WorldRenderManager.class);
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
     * Holds the chunk's own sync lock across every subchunk tick plus the
     * final merge/register, mirroring MergeBranch and RenderBranch. Skips
     * the whole chunk for this pass if the async pipeline currently owns it.
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

            if (chunkChanged) {
                chunk.merge();
                worldRenderManager.addChunkInstance(chunk);
            }
        } finally {
            syncContainer.release();
        }

        if (!chunkChanged)
            return;

        worldStreamManager.invalidateMegaForChunk(chunk.getCoordinate());
        worldStreamManager.invalidateChunkBatch(chunk.getCoordinate());
    }

    private boolean tickSubChunk(ChunkInstance chunk, SubChunkInstance subChunk, float delta) {

        if (!subChunk.hasBlockType(DynamicGeometryType.LIQUID) || subChunk.isLiquidStable())
            return false;

        if (subChunk.isUniformFill() && !subChunk.isPopulated()) {
            subChunk.setLiquidStable(true);
            return false;
        }

        subChunk.addLiquidFlowTime(delta);

        float interval = resolveFlowInterval(subChunk);

        if (subChunk.getLiquidFlowAccumulator() < interval)
            return false;

        subChunk.resetLiquidFlowAccumulator();

        if (!fluidSimulationSystem.flow(chunk, subChunk)) {
            subChunk.setLiquidStable(true);
            return false;
        }

        subChunk.getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, chunk, (int) subChunk.getCoordinate());

        rebuildTouchedNeighbors(chunk);

        return true;
    }

    /*
     * A flow step can write into a subchunk other than the one just ticked —
     * the column below it falling, or a neighbor chunk it spread into — and
     * that subchunk will not otherwise be merged this frame, so each one gets
     * its own full rebuild-merge-register-invalidate cycle here. lockedChunk
     * is the chunk tickChunk() already holds the lock for.
     */
    private void rebuildTouchedNeighbors(ChunkInstance lockedChunk) {

        ObjectArrayList<ChunkInstance> touchedChunks = fluidSimulationSystem.getTouchedChunks();
        IntArrayList touchedSubChunkY = fluidSimulationSystem.getTouchedSubChunkY();

        for (int i = 0; i < touchedChunks.size(); i++) {

            ChunkInstance touchedChunk = touchedChunks.get(i);
            int subChunkY = touchedSubChunkY.getInt(i);

            if (touchedChunk == lockedChunk) {
                rebuildChunkGeometry(touchedChunk, subChunkY);
                continue;
            }

            ChunkDataSyncContainer touchedSync = touchedChunk.getChunkDataSyncContainer();

            if (!touchedSync.tryAcquire())
                continue;

            try {
                rebuildChunkGeometry(touchedChunk, subChunkY);
            } finally {
                touchedSync.release();
            }

            worldStreamManager.invalidateMegaForChunk(touchedChunk.getCoordinate());
            worldStreamManager.invalidateChunkBatch(touchedChunk.getCoordinate());
        }
    }

    private void rebuildChunkGeometry(ChunkInstance targetChunk, int subChunkY) {

        SubChunkInstance touchedSubChunk = targetChunk.getSubChunk(subChunkY);

        touchedSubChunk.getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, targetChunk, subChunkY);

        targetChunk.merge();
        worldRenderManager.addChunkInstance(targetChunk);
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