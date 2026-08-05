package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.physicspipeline.util.LiquidPhysicsUtility;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class LiquidTickBranch extends BranchPackage {

    /*
     * Dedicated execution path LIQUID-geometry blocks tick through.
     * Scheduling lives entirely in WorldTickManager — this branch only fires
     * when its slot comes up. Each firing measures the real elapsed seconds
     * since its previous firing and advances every active liquid-containing
     * subchunk's own flow accumulator by that amount. Once a subchunk's
     * accumulator reaches the flow interval of its fastest contained liquid
     * — resolved from that liquid's own viscosity via LiquidPhysicsUtility —
     * its geometry is rebuilt and re-registered for rendering, exactly like a
     * manual block edit would be. Level redistribution between neighboring
     * liquid blocks is driven by a later stage; this branch owns the timing
     * that decides when a redraw is due.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;

    // Timing
    private long lastTickTimeMillis;

    // Internal \\

    @Override
    protected void create() {
        this.lastTickTimeMillis = 0L;
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

    // Tick \\

    public void tick(int frameCount) {

        long now = internal.getTime();

        if (lastTickTimeMillis == 0L) {
            lastTickTimeMillis = now;
            return;
        }

        float delta = (now - lastTickTimeMillis) / 1000f;
        lastTickTimeMillis = now;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            tickGrid((GridInstance) elements[i], delta);
    }

    private void tickGrid(GridInstance grid, float delta) {

        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();

        for (ChunkInstance chunk : activeChunks.values())
            tickChunk(chunk, delta);
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

    private float resolveFlowInterval(SubChunkInstance subChunk) {

        ShortOpenHashSet liquidBlockIDs = subChunk.getContainedLiquidBlockIDs();
        float fastestInterval = Float.MAX_VALUE;

        ShortIterator iterator = liquidBlockIDs.iterator();

        while (iterator.hasNext()) {
            BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(iterator.nextShort());
            float interval = LiquidPhysicsUtility.getFlowIntervalSeconds(blockHandle.getViscosity());
            if (interval < fastestInterval)
                fastestInterval = interval;
        }

        return fastestInterval;
    }
}