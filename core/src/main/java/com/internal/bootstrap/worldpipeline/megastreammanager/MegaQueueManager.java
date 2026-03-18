package com.internal.bootstrap.worldpipeline.megastreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.grid.GridInstance;
import com.internal.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import com.internal.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataUtility;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayDeque;

class MegaQueueManager extends ManagerPackage {

    /*
     * Drives the per-frame mega chunk pipeline across all active grids. Each
     * grid owns its own activeMegaChunks map. The mega pool is shared across
     * all grids for efficiency.
     */

    // Internal
    private WorldRenderManager worldRenderManager;
    private WorldStreamManager worldStreamManager;
    private ChunkStreamManager chunkStreamManager;

    // Branches
    private MegaMergeBranch mergeBranch;
    private MegaAssessBranch assessBranch;
    private MegaRenderBranch renderBranch;
    private MegaDumpBranch dumpBranch;

    // Pool — shared across all grids
    private ArrayDeque<MegaChunkInstance> megaPool;

    // Settings
    private int megaPoolMaxOverflow;
    private int megaChunkSize;
    private int megaScale;
    private int megaAssessPerFrame;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.mergeBranch = create(MegaMergeBranch.class);
        this.assessBranch = create(MegaAssessBranch.class);
        this.renderBranch = create(MegaRenderBranch.class);
        this.dumpBranch = create(MegaDumpBranch.class);

        // Pool
        this.megaPool = new ArrayDeque<>();

        // Settings
        this.megaPoolMaxOverflow = EngineSetting.MEGA_POOL_MAX_OVERFLOW;
        this.megaChunkSize = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaScale = megaChunkSize * megaChunkSize;
        this.megaAssessPerFrame = EngineSetting.MEGA_ASSESS_PER_FRAME;
    }

    @Override
    protected void get() {

        // Internal
        this.worldRenderManager = get(WorldRenderManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
    }

    @Override
    protected void update() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            assessActiveMegas((GridInstance) elements[i]);
    }

    // Grid Events \\

    void onGridRebuilt(GridInstance grid) {
        flushActiveMegas(grid);
    }

    void onGridRemoved(GridInstance grid) {
        flushActiveMegas(grid);
    }

    // Mega Max \\

    private int computeMegaMax(GridInstance grid) {
        return (grid.getTotalSlots() / megaScale) + megaPoolMaxOverflow;
    }

    // Flush \\

    private void flushActiveMegas(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks = grid.getActiveMegaChunks();
        int megaMax = computeMegaMax(grid);

        var iterator = activeMegaChunks.long2ObjectEntrySet().iterator();

        while (iterator.hasNext()) {

            var entry = iterator.next();
            long megaCoord = entry.getLongKey();
            MegaChunkInstance mega = entry.getValue();
            iterator.remove();

            MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();

            if (!sync.tryAcquire()) {
                activeMegaChunks.put(megaCoord, mega);
                continue;
            }

            try {
                worldRenderManager.removeMegaInstance(megaCoord);
                mega.reset();
            } finally {
                sync.release();
            }

            if (megaPool.size() < megaMax)
                megaPool.push(mega);
            else
                mega.dispose();
        }
    }

    // Batching \\

    void batchChunk(ChunkInstance chunkInstance, GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks = grid.getActiveMegaChunks();
        int megaMax = computeMegaMax(grid);

        long megaCoord = Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate());
        MegaChunkInstance mega = activeMegaChunks.get(megaCoord);

        if (mega == null) {
            mega = createMega(megaCoord, grid, megaMax, activeMegaChunks);
            if (mega == null)
                return;
            activeMegaChunks.put(megaCoord, mega);
        }

        mergeBranch.mergeChunkIntoMega(chunkInstance, mega);
    }

    private MegaChunkInstance createMega(
            long megaCoord,
            GridInstance grid,
            int megaMax,
            Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {

        if (!megaPool.isEmpty())
            return configureMega(megaPool.poll(), megaCoord, grid);

        if (activeMegaChunks.size() >= megaMax)
            return null;

        return configureMega(create(MegaChunkInstance.class), megaCoord, grid);
    }

    private MegaChunkInstance configureMega(
            MegaChunkInstance mega,
            long megaCoord,
            GridInstance grid) {

        mega.constructor(
                worldRenderManager,
                grid.getWorldHandle(),
                megaCoord,
                chunkStreamManager.getChunkVAO(),
                megaScale);

        return mega;
    }

    // Assessment \\

    private void assessActiveMegas(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks = grid.getActiveMegaChunks();

        if (activeMegaChunks.isEmpty())
            return;

        int megaMax = computeMegaMax(grid);
        int assessed = 0;
        var iterator = activeMegaChunks.long2ObjectEntrySet().iterator();

        while (iterator.hasNext() && assessed < megaAssessPerFrame) {

            var entry = iterator.next();
            long megaCoord = entry.getLongKey();
            MegaChunkInstance mega = entry.getValue();
            iterator.remove();

            MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
            GridSlotHandle gridSlotHandle = grid.getGridSlotForChunk(megaCoord);

            if (gridSlotHandle == null) {
                unloadMega(mega, megaCoord, megaMax);
                assessed++;
                continue;
            }

            MegaQueueOperation op = determineOperation(sync, gridSlotHandle);

            switch (op) {
                case ASSESS -> assessBranch.assessMega(mega);
                case RENDER -> renderBranch.renderMega(mega, sync);
                case DUMP -> dumpBranch.dumpMega(mega, sync, megaCoord);
                case SKIP -> {
                }
            }

            activeMegaChunks.put(megaCoord, mega);
            assessed++;
        }
    }

    private MegaQueueOperation determineOperation(
            MegaDataSyncContainer sync,
            GridSlotHandle gridSlotHandle) {

        if (!sync.tryAcquire())
            return MegaQueueOperation.SKIP;

        try {
            GridSlotDetailLevel slotLevel = gridSlotHandle.getDetailLevel();

            MegaData toDump = MegaDataUtility.nextToDump(sync.getData(), slotLevel);

            if (toDump != null)
                return MegaQueueOperation.DUMP;

            MegaData toLoad = MegaDataUtility.nextToLoad(sync.getData(), slotLevel);

            if (toLoad != null)
                return toOperation(toLoad);

            return MegaQueueOperation.SKIP;
        } finally {
            sync.release();
        }
    }

    private MegaQueueOperation toOperation(MegaData stage) {
        return switch (stage) {
            case BATCH_DATA -> MegaQueueOperation.ASSESS;
            case RENDER_DATA -> MegaQueueOperation.RENDER;
            default -> MegaQueueOperation.SKIP;
        };
    }

    // Unload \\

    private void unloadMega(MegaChunkInstance mega, long megaCoord, int megaMax) {

        MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();

        if (!sync.tryAcquire())
            return;

        try {
            worldRenderManager.removeMegaInstance(megaCoord);
            mega.reset();
        } finally {
            sync.release();
        }

        if (megaPool.size() < megaMax)
            megaPool.push(mega);
        else
            mega.dispose();
    }

    // Invalidation \\

    void invalidateMegaForChunk(long chunkCoordinate) {

        long megaCoord = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];
            MegaChunkInstance mega = grid.getActiveMegaChunks().get(megaCoord);

            if (mega == null)
                continue;

            MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();

            if (!sync.tryAcquire())
                continue;

            try {
                worldRenderManager.removeMegaInstance(megaCoord);
                clearChunkBatchFlags(mega);
                mega.reset();
            } finally {
                sync.release();
            }

            return;
        }
    }

    private void clearChunkBatchFlags(MegaChunkInstance mega) {

        ObjectArrayList<ChunkInstance> list = mega.getBatchedChunkList();
        Object[] elements = list.elements();
        int size = list.size();

        for (int i = 0; i < size; i++) {

            ChunkInstance chunk = (ChunkInstance) elements[i];
            ChunkDataSyncContainer chunkSync = chunk.getChunkDataSyncContainer();

            if (!chunkSync.tryAcquire())
                continue;

            try {
                chunkSync.getData()[ChunkData.BATCH_DATA.index] = false;
            } finally {
                chunkSync.release();
            }
        }
    }
}