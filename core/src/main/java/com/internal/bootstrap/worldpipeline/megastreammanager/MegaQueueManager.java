package com.internal.bootstrap.worldpipeline.megastreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotDetailLevel;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataUtility;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaAssessBranch;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaDumpBranch;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaMergeBranch;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaQueueOperation;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaRenderBranch;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.ArrayDeque;

/*
 * Drives the per-frame mega chunk pipeline. Owns lifecycle (create, pool,
 * flush, unload) and assessment dispatch. All per-operation logic is
 * delegated to branches — MegaMergeBranch owns the batch→merge→flag sequence.
 */
class MegaQueueManager extends ManagerPackage {

    // Internal
    private WorldRenderManager worldRenderSystem;
    private ChunkStreamManager chunkStreamManager;
    private GridManager gridManager;

    // Branches
    private MegaMergeBranch mergeBranch;
    private MegaAssessBranch assessBranch;
    private MegaRenderBranch renderBranch;
    private MegaDumpBranch dumpBranch;

    // Active Megas
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;

    // Mega Pool
    private ArrayDeque<MegaChunkInstance> megaPool;
    private int megaMax;
    private int MEGA_POOL_MAX_OVERFLOW;
    private int MEGA_CHUNK_SIZE;
    private int megaScale;
    private int MEGA_ASSESS_PER_FRAME;
    // Internal \\

    @Override
    protected void create() {
        this.mergeBranch = create(MegaMergeBranch.class);
        this.assessBranch = create(MegaAssessBranch.class);
        this.renderBranch = create(MegaRenderBranch.class);
        this.dumpBranch = create(MegaDumpBranch.class);
        this.megaPool = new ArrayDeque<>();
    }

    @Override
    protected void get() {
        this.worldRenderSystem = get(WorldRenderManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.gridManager = get(GridManager.class);
        this.MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaScale = MEGA_CHUNK_SIZE * MEGA_CHUNK_SIZE;
        this.MEGA_POOL_MAX_OVERFLOW = EngineSetting.MEGA_POOL_MAX_OVERFLOW;
        this.MEGA_ASSESS_PER_FRAME = EngineSetting.MEGA_ASSESS_PER_FRAME;
    }

    @Override
    protected void start() {
        computeMegaMax();
    }

    @Override
    protected void update() {
        assessActiveMegas();
    }

    // Grid Rebuild \\

    void onGridRebuilt() {
        computeMegaMax();
        flushActiveMegas();
    }

    private void computeMegaMax() {
        int megaSlots = gridManager.getGrid().getTotalSlots() / megaScale;
        this.megaMax = megaSlots + MEGA_POOL_MAX_OVERFLOW;
    }

    private void flushActiveMegas() {
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
                worldRenderSystem.removeMegaInstance(megaCoord);
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

    // Batching (bridge from chunk pipeline) \\

    void batchChunk(ChunkInstance chunkInstance) {
        long megaCoord = Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate());

        MegaChunkInstance mega = activeMegaChunks.get(megaCoord);
        if (mega == null) {
            mega = createMega(megaCoord);
            if (mega == null)
                return;
            activeMegaChunks.put(megaCoord, mega);
        }

        mergeBranch.mergeChunkIntoMega(chunkInstance, mega);
    }

    private MegaChunkInstance createMega(long megaCoord) {
        if (!megaPool.isEmpty())
            return configureMega(megaPool.poll(), megaCoord);
        if (activeMegaChunks.size() >= megaMax)
            return null;
        return configureMega(create(MegaChunkInstance.class), megaCoord);
    }

    private MegaChunkInstance configureMega(MegaChunkInstance mega, long megaCoord) {
        mega.constructor(
                worldRenderSystem,
                chunkStreamManager.getActiveWorldHandle(),
                megaCoord,
                chunkStreamManager.getChunkVAO(),
                megaScale);
        return mega;
    }

    // Assessment \\

    private void assessActiveMegas() {
        if (activeMegaChunks.isEmpty())
            return;

        int assessed = 0;

        var iterator = activeMegaChunks.long2ObjectEntrySet().iterator();
        while (iterator.hasNext() && assessed < MEGA_ASSESS_PER_FRAME) {
            var entry = iterator.next();
            long megaCoord = entry.getLongKey();
            MegaChunkInstance mega = entry.getValue();
            iterator.remove();

            MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
            GridSlotHandle gridSlotHandle = gridManager.getGrid().getGridSlotForChunk(megaCoord);

            if (gridSlotHandle == null) {
                unloadMega(mega, megaCoord);
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

            MegaData toDump = MegaDataUtility.nextToDump(sync.data, slotLevel);
            if (toDump != null)
                return MegaQueueOperation.DUMP;

            MegaData toLoad = MegaDataUtility.nextToLoad(sync.data, slotLevel);
            if (toLoad != null)
                return toOperation(toLoad);

            return MegaQueueOperation.SKIP;
        } finally {
            sync.release();
        }
    }

    private MegaQueueOperation toOperation(MegaData stage) {
        switch (stage) {
            case BATCH_DATA:
                return MegaQueueOperation.ASSESS;
            case RENDER_DATA:
                return MegaQueueOperation.RENDER;
            default:
                return MegaQueueOperation.SKIP;
        }
    }

    // Unload \\

    private void unloadMega(MegaChunkInstance mega, long megaCoord) {
        MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
        if (!sync.tryAcquire()) {
            activeMegaChunks.put(megaCoord, mega);
            return;
        }
        try {
            worldRenderSystem.removeMegaInstance(megaCoord);
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
        MegaChunkInstance mega = activeMegaChunks.get(megaCoord);
        if (mega == null)
            return;
        MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
        if (!sync.tryAcquire())
            return;
        try {
            worldRenderSystem.removeMegaInstance(megaCoord);
            clearChunkBatchFlags(mega);
            mega.reset();
        } finally {
            sync.release();
        }
    }

    private void clearChunkBatchFlags(MegaChunkInstance mega) {
        for (ChunkInstance chunk : mega.getBatchedChunks().values()) {
            ChunkDataSyncContainer chunkSync = chunk.getChunkDataSyncContainer();
            if (!chunkSync.tryAcquire())
                continue;
            try {
                chunkSync.data[ChunkData.BATCH_DATA.index] = false;
            } finally {
                chunkSync.release();
            }
        }
    }

    void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        this.activeMegaChunks = activeMegaChunks;
    }
}