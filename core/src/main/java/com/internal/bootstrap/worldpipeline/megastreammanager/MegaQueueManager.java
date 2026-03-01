package com.internal.bootstrap.worldpipeline.megastreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaAssessBranch;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaDumpBranch;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaMergeBranch;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaQueueOperation;
import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaRenderBranch;
import com.internal.bootstrap.worldpipeline.worldrendermanager.RenderType;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.kernel.threadmanager.ThreadHandle;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.ArrayDeque;

/*
 * Drives the per-frame mega chunk pipeline. Megas are created when chunks
 * batch into them, capped at the expected mega slot count plus overflow so
 * total active + pooled instances are bounded exactly like chunks.
 * Assessment is cursor-driven per frame using the natural ordering of
 * Long2ObjectLinkedOpenHashMap, mirroring the chunk queue contract exactly.
 * Detail level renderMode is read directly from the grid slot — no hardcoded
 * enum values. BATCHED → full mega pipeline. INDIVIDUAL → dump if rendered.
 * null slot → unload entirely into pool.
 */
class MegaQueueManager extends ManagerPackage {

    // Internal
    private ThreadHandle threadHandle;
    private WorldRenderManager worldRenderSystem;
    private ChunkStreamManager chunkStreamManager;
    private GridManager gridManager;

    // Branches
    private MegaAssessBranch assessBranch;
    private MegaMergeBranch mergeBranch;
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

    // Cached indices
    private int batchDataIndex;
    // Internal \\

    @Override
    protected void create() {
        this.assessBranch = create(MegaAssessBranch.class);
        this.mergeBranch = create(MegaMergeBranch.class);
        this.renderBranch = create(MegaRenderBranch.class);
        this.dumpBranch = create(MegaDumpBranch.class);
        this.megaPool = new ArrayDeque<>();
    }

    @Override
    protected void get() {
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.worldRenderSystem = get(WorldRenderManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.gridManager = get(GridManager.class);
        this.MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaScale = MEGA_CHUNK_SIZE * MEGA_CHUNK_SIZE;
        this.MEGA_POOL_MAX_OVERFLOW = EngineSetting.MEGA_POOL_MAX_OVERFLOW;
        this.MEGA_ASSESS_PER_FRAME = EngineSetting.MEGA_ASSESS_PER_FRAME;
        this.batchDataIndex = ChunkData.BATCH_DATA.index;
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
        MegaChunkInstance mega = activeMegaChunks.computeIfAbsent(megaCoord, this::createMega);
        if (mega == null) {
            activeMegaChunks.remove(megaCoord);
            return;
        }
        MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
        if (sync.isLocked())
            return;
        if (!mega.batchChunk(chunkInstance))
            return;
        ChunkDataSyncContainer chunkSync = chunkInstance.getChunkDataSyncContainer();
        if (!chunkSync.tryAcquire())
            return;
        try {
            chunkSync.data[batchDataIndex] = true;
        } finally {
            chunkSync.release();
        }
    }

    private MegaChunkInstance createMega(long megaCoord) {
        if (activeMegaChunks.size() + megaPool.size() >= megaMax)
            return null;
        if (!megaPool.isEmpty())
            return configureMega(megaPool.poll(), megaCoord);
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

            if (sync.isLocked()) {
                activeMegaChunks.put(megaCoord, mega);
                assessed++;
                continue;
            }

            GridSlotHandle gridSlotHandle = gridManager.getGrid().getGridSlotForChunk(megaCoord);

            if (gridSlotHandle == null) {
                unloadMega(mega, megaCoord);
                assessed++;
                continue;
            }

            MegaQueueOperation op = determineOperation(sync, gridSlotHandle);
            switch (op) {
                case ASSESS -> assessBranch.assessMega(mega);
                case MERGE -> mergeBranch.mergeMega(mega, threadHandle);
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
            boolean[] data = sync.data;

            // Use renderMode from detail level — no hardcoded enum value matching
            if (gridSlotHandle.getDetailLevel().renderMode == RenderType.INDIVIDUAL) {
                if (data[MegaData.RENDER_DATA.index])
                    return MegaQueueOperation.DUMP;
                return MegaQueueOperation.SKIP;
            }

            // BATCHED — iterate MegaData entries, return operation for first unset flag
            for (MegaData megaData : MegaData.VALUES) {
                if (!data[megaData.index])
                    return megaData.queueOperation;
            }

            return MegaQueueOperation.SKIP;
        } finally {
            sync.release();
        }
    }

    // Unload \\

    private void unloadMega(MegaChunkInstance mega, long megaCoord) {
        worldRenderSystem.removeMegaInstance(megaCoord);
        mega.reset();
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
            if (sync.data[MegaData.RENDER_DATA.index])
                worldRenderSystem.removeMegaInstance(megaCoord);
            sync.data[MegaData.MERGE_DATA.index] = false;
            sync.data[MegaData.RENDER_DATA.index] = false;
        } finally {
            sync.release();
        }
    }

    void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        this.activeMegaChunks = activeMegaChunks;
    }
}