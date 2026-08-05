package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.TickQuadrant;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class LiquidTickBranch extends BranchPackage {

    /*
     * Dedicated execution path LIQUID-geometry blocks tick through. Owns its
     * own frame interval, counter, and quadrant cursor — each firing
     * advances to the next of four world quadrants (split by chunk X/Z sign)
     * and walks only that quadrant's active chunks, so a single firing never
     * scans every liquid chunk at once. Real elapsed seconds since that same
     * quadrant was last visited drive each contained subchunk's flow
     * accumulator; once it reaches the flow interval of the subchunk's
     * fastest liquid — derived from that liquid's own viscosity — its
     * geometry rebuilds and re-registers for rendering. Level redistribution
     * between neighboring liquid blocks is a later stage; this branch only
     * owns redraw timing.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;

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

        if (!subChunk.hasBlockType(DynamicGeometryType.LIQUID))
            return false;

        subChunk.addLiquidFlowTime(delta);

        float interval = resolveFlowInterval(subChunk);

        if (subChunk.getLiquidFlowAccumulator() < interval)
            return false;

        subChunk.resetLiquidFlowAccumulator();

        subChunk.getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, chunk, (int) subChunk.getCoordinate());

        return true;
    }

    /*
     * Converts each contained liquid's own viscosity (Pa·s) into the
     * real-seconds interval its geometry is allowed to redraw at, clamped
     * between LIQUID_FLOW_INTERVAL_MIN_SECONDS and _MAX_SECONDS. Returns the
     * fastest interval among every liquid this subchunk contains, since any
     * one of them redrawing is reason enough for the whole subchunk to
     * rebuild.
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