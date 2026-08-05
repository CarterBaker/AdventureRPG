package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.liquidsimulationsystem.LiquidSimulationSystem;
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
     * Dedicated execution path LIQUID-geometry blocks tick through. Each
     * firing advances to the next of four world quadrants and walks only that
     * quadrant's active chunks. A subchunk with no liquid, or whose liquid has
     * already settled (SubChunkInstance.isLiquidStable()), is skipped outright
     * — no palette scan. Otherwise, once its flow accumulator crosses its
     * fastest contained liquid's flow interval, LiquidSimulationSystem
     * redistributes its levels by one step; if nothing moved, the subchunk is
     * marked stable so future visits skip it until something touches it again.
     * Any neighboring subchunk the step touched is rebuilt, re-registered, and
     * un-stabilized the same way, since it will not otherwise come up for a
     * merge this frame.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;

    // Branches
    private LiquidSimulationSystem liquidSimulationSystem;

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
        this.liquidSimulationSystem = get(LiquidSimulationSystem.class);
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

        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();

        for (ChunkInstance chunk : activeChunks.values()) {

            if (TickQuadrant.fromChunkCoordinate(chunk.getCoordinate()) != quadrant)
                continue;

            tickChunk(chunk, delta);
        }
    }

    private void tickChunk(ChunkInstance chunk, float delta) {

        SubChunkInstance[] subChunks = chunk.getSubChunks();
        boolean chunkChanged = false;

        for (int i = 0; i < subChunks.length; i++)
            if (tickSubChunk(chunk, subChunks[i], delta))
                chunkChanged = true;

        if (!chunkChanged)
            return;

        chunk.merge();
        worldRenderManager.addChunkInstance(chunk);
        worldStreamManager.invalidateMegaForChunk(chunk.getCoordinate());
        worldStreamManager.invalidateChunkBatch(chunk.getCoordinate());
    }

    private boolean tickSubChunk(ChunkInstance chunk, SubChunkInstance subChunk, float delta) {

        if (!subChunk.hasBlockType(DynamicGeometryType.LIQUID) || subChunk.isLiquidStable())
            return false;

        subChunk.addLiquidFlowTime(delta);

        float interval = resolveFlowInterval(subChunk);

        if (subChunk.getLiquidFlowAccumulator() < interval)
            return false;

        subChunk.resetLiquidFlowAccumulator();

        if (!liquidSimulationSystem.flow(chunk, subChunk)) {
            subChunk.setLiquidStable(true);
            return false;
        }

        subChunk.getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, chunk, (int) subChunk.getCoordinate());

        rebuildTouchedNeighbors();

        return true;
    }

    /*
     * A flow step can write into a subchunk other than the one just ticked —
     * the column below it falling, or a neighbor chunk it spread into — and
     * that subchunk will not otherwise be merged this frame, so each one gets
     * its own full rebuild-merge-register-invalidate cycle here. Its stable
     * flag is already false from the write itself (SubChunkInstance.setBlock/
     * setLiquidLevel invalidate automatically) — this only handles the
     * geometry side.
     */
    private void rebuildTouchedNeighbors() {

        ObjectArrayList<ChunkInstance> touchedChunks = liquidSimulationSystem.getTouchedChunks();
        IntArrayList touchedSubChunkY = liquidSimulationSystem.getTouchedSubChunkY();

        for (int i = 0; i < touchedChunks.size(); i++) {

            ChunkInstance touchedChunk = touchedChunks.get(i);
            int subChunkY = touchedSubChunkY.getInt(i);
            SubChunkInstance touchedSubChunk = touchedChunk.getSubChunk(subChunkY);

            touchedSubChunk.getDynamicPacketInstance().clear();
            dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, touchedChunk, subChunkY);

            touchedChunk.merge();
            worldRenderManager.addChunkInstance(touchedChunk);
            worldStreamManager.invalidateMegaForChunk(touchedChunk.getCoordinate());
            worldStreamManager.invalidateChunkBatch(touchedChunk.getCoordinate());
        }
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