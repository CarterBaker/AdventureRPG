package application.bootstrap.worldpipeline.chunkstreammanager;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayDeque;

import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkDataUtility;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.queue.QueueInstance;
import engine.util.queue.QueueItemHandle;

class ChunkQueueManager extends ManagerPackage {

    /*
     * Drives the per-frame chunk queue across all active grids. Each grid owns
     * its own active chunks, load requests, and unload requests. The chunk pool
     * is shared across all grids for efficiency. All branch dispatch is
     * per-grid — branches own the implementation. Both loadQueue() and
     * assessActiveChunks() advance up to their own per-frame budget rather than
     * one, so a full render distance worth of chunks can actually stream in at
     * a playable rate instead of one chunk per pass. RENDER dispatch inside
     * assessActiveChunks() is bounded separately by chunkGpuUploadBudget —
     * glBufferData/VAO creation is a synchronous driver call, so letting
     * maxChunkStreamPerBatch alone govern it let two dozen uploads land in a
     * single frame during heavy streaming, which is what actually produced the
     * visible stutter. Chunks that miss the upload budget simply retry next
     * frame — nothing downstream depends on RENDER_DATA landing this frame
     * specifically. LOAD/BUILD/MERGE/ITEM_LOAD/BATCH dispatch is additionally
     * gated on the WorldStreaming pool's own in-flight capacity (see
     * ThreadHandle.hasCapacity()) — this is what keeps the executor's internal
     * task queue from growing without bound under sustained load; a chunk that
     * misses this gate is simply reassessed next frame, same as one that
     * misses the GPU upload budget.
     *
     * Admission is gated by the exact same signal as dispatch. scanGridSlots()
     * stops discovering new load candidates once loadRequests already holds
     * maxQueuedLoadRequests pending coordinates, and loadQueue() refuses to
     * pull new coordinates out of that set at all while the WorldStreaming
     * pool reports itself saturated — previously scanning and loading ran
     * unconditionally every frame regardless of how far behind the async
     * pipeline already was, which meant a fresh session (empty chunk pool, so
     * every admission is a brand-new ChunkInstance — 64 SubChunkInstances plus
     * their handles) could allocate its entire render distance's worth of
     * chunk object graphs within the first few seconds, long before dispatch
     * could ever start processing more than 48 of them at once. That spike is
     * pure waste — it grows the heap and working set far past what the
     * pipeline can use, and the JVM does not give that memory back — which is
     * why performance degraded a few seconds in and then stayed degraded.
     * Neither maxChunkAdmissionsPerFrame nor maxQueuedLoadRequests caps the
     * eventual size of activeChunks itself — that would permanently starve
     * distant chunks once total slot count exceeds the cap. They only pace
     * how fast new chunk graphs get allocated, tied to how fast the pipeline
     * can actually retire the ones already admitted.
     */

    // Internal
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;
    private ChunkStreamManager chunkStreamManager;
    private WorldRenderManager worldRenderManager;
    private ThreadHandle worldStreamingThreadHandle;

    // Branches
    private GenerationBranch generationBranch;
    private AssessmentBranch assessmentBranch;
    private BuildBranch buildBranch;
    private MergeBranch mergeBranch;
    private ItemLoadBranch itemLoadBranch;
    private ItemRenderBranch itemRenderBranch;
    private BatchBranch batchBranch;
    private RenderBranch renderBranch;
    private DumpBranch dumpBranch;

    // Block IDs
    private short airBlockId;

    // Queue
    private QueueInstance chunkQueue;
    private Int2ObjectOpenHashMap<ChunkQueueItem> id2QueueItem;

    // Pool — shared across all grids
    private ArrayDeque<ChunkInstance> chunkPool;
    private int chunkPoolMaxOverflow;

    // Streaming
    private int maxChunkStreamPerBatch;

    // Admission Backpressure — paces scanning/loading against the pipeline's
    // actual throughput instead of letting either race ahead of it
    private int maxChunkAdmissionsPerFrame;
    private int maxQueuedLoadRequests;

    // GPU Upload Throttle
    private int chunkGpuUploadBudget;
    private int gpuUploadsThisFrame;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.generationBranch = create(GenerationBranch.class);
        this.assessmentBranch = create(AssessmentBranch.class);
        this.buildBranch = create(BuildBranch.class);
        this.mergeBranch = create(MergeBranch.class);
        this.itemLoadBranch = create(ItemLoadBranch.class);
        this.itemRenderBranch = create(ItemRenderBranch.class);
        this.batchBranch = create(BatchBranch.class);
        this.renderBranch = create(RenderBranch.class);
        this.dumpBranch = create(DumpBranch.class);

        // Queue
        this.chunkQueue = create(QueueInstance.class);
        this.id2QueueItem = new Int2ObjectOpenHashMap<>();

        for (ChunkQueueItem item : ChunkQueueItem.values()) {
            QueueItemHandle handle = chunkQueue.addQueueItem(item.name());
            id2QueueItem.put(handle.getQueueItemID(), item);
        }

        // Pool
        this.chunkPool = new ArrayDeque<>();
        this.chunkPoolMaxOverflow = EngineSetting.CHUNK_POOL_MAX_OVERFLOW;

        // Streaming
        this.maxChunkStreamPerBatch = EngineSetting.MAX_CHUNK_STREAM_PER_BATCH;

        // Admission Backpressure
        this.maxChunkAdmissionsPerFrame = EngineSetting.MAX_CHUNK_STREAM_PER_FRAME;
        this.maxQueuedLoadRequests = EngineSetting.MAX_CHUNK_STREAM_PER_QUEUE;

        // GPU Upload Throttle
        this.chunkGpuUploadBudget = EngineSetting.MAX_CHUNK_GPU_UPLOADS_PER_FRAME;
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.worldRenderManager = get(WorldRenderManager.class);
        this.worldStreamingThreadHandle = getThreadHandleFromThreadName("WorldStreaming");
    }

    @Override
    protected void awake() {
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    @Override
    protected void update() {
        this.gpuUploadsThisFrame = 0;
        executeQueue();
    }

    // Grid Events \\

    void onGridRebuilt(GridInstance grid) {
        grid.getLoadRequests().clear();
        grid.getUnloadRequests().clear();
        flushActiveChunks(grid);
    }

    void onGridRemoved(GridInstance grid) {
        onGridRebuilt(grid);
    }

    // Queue Execution \\

    private void executeQueue() {

        while (true) {

            QueueItemHandle nextItem = chunkQueue.getNextQueueItem();

            if (nextItem == null)
                break;

            ChunkQueueItem queueItem = id2QueueItem.get(nextItem.getQueueItemID());

            ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
            Object[] elements = grids.elements();
            int size = grids.size();

            switch (queueItem) {
                case SCAN_GRID_SLOTS -> {
                    for (int i = 0; i < size; i++)
                        scanGridSlots((GridInstance) elements[i]);
                }
                case LOAD -> {
                    for (int i = 0; i < size; i++)
                        loadQueue((GridInstance) elements[i]);
                }
                case ASSESS_ACTIVE_CHUNKS -> {
                    for (int i = 0; i < size; i++)
                        assessActiveChunks((GridInstance) elements[i]);
                }
            }
        }
    }

    // Grid Scan \\

    /*
     * Stops discovering new load candidates once loadRequests already holds
     * maxQueuedLoadRequests pending coordinates. Without this, the scan
     * cursor sweeps the entire grid in totalSlots / GRID_SLOTS_SCAN_PER_FRAME
     * frames regardless of whether anything downstream could ever act on the
     * result, front-loading a huge, useless backlog. The cursor simply
     * pauses here and resumes exactly where it left off once loadQueue()
     * drains enough of the backlog to make room again.
     */
    private void scanGridSlots(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();
        LongLinkedOpenHashSet loadRequests = grid.getLoadRequests();

        if (loadRequests.size() >= maxQueuedLoadRequests)
            return;

        for (int i = 0; i < EngineSetting.GRID_SLOTS_SCAN_PER_FRAME; i++) {
            GridSlotHandle slot = grid.getNextScanSlot();
            long chunkCoordinate = slot.getChunkCoordinate();
            if (!activeChunks.containsKey(chunkCoordinate))
                loadRequests.add(chunkCoordinate);
        }
    }

    // Load \\

    /*
     * Admits pending load requests into activeChunks, allocating or pooling
     * a ChunkInstance for each. Gated on the WorldStreaming pool's own
     * hasCapacity() — the same signal reserveAsyncWork already trusts for
     * dispatch — so admission can never outpace what the pipeline can
     * actually process. When the pool is saturated this is a no-op for the
     * frame; the requests stay queued and are picked up the moment capacity
     * frees up. maxChunkAdmissionsPerFrame caps the burst size once capacity
     * is available, distinct from the heavier per-frame dispatch budget used
     * in assessActiveChunks().
     */
    private void loadQueue(GridInstance grid) {

        if (!worldStreamingThreadHandle.hasCapacity())
            return;

        LongLinkedOpenHashSet loadRequests = grid.getLoadRequests();
        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();

        var iterator = loadRequests.iterator();
        int loaded = 0;

        while (iterator.hasNext() && loaded < maxChunkAdmissionsPerFrame) {

            long chunkCoordinate = iterator.nextLong();
            iterator.remove();

            ChunkInstance chunkInstance = chunkPool.isEmpty()
                    ? create(ChunkInstance.class)
                    : chunkPool.poll();

            chunkInstance.constructor(
                    worldRenderManager,
                    grid.getWorldHandle(),
                    chunkCoordinate,
                    chunkStreamManager.getChunkVAO(),
                    airBlockId,
                    activeChunks);

            activeChunks.put(chunkCoordinate, chunkInstance);
            loaded++;
        }
    }

    // Assessment \\

    private void assessActiveChunks(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();
        LongLinkedOpenHashSet unloadRequests = grid.getUnloadRequests();

        if (activeChunks.isEmpty())
            return;

        var iterator = activeChunks.long2ObjectEntrySet().iterator();
        int assessed = 0;

        while (iterator.hasNext() && assessed < maxChunkStreamPerBatch) {

            var entry = iterator.next();
            long chunkCoordinate = entry.getLongKey();
            ChunkInstance chunkInstance = entry.getValue();
            iterator.remove();

            if (unloadRequests.contains(chunkCoordinate)) {

                ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

                if (!syncContainer.tryAcquire()) {
                    activeChunks.put(chunkCoordinate, chunkInstance);
                    assessed++;
                    continue;
                }

                try {
                    unloadRequests.remove(chunkCoordinate);
                    worldRenderManager.removeChunkInstance(chunkCoordinate);
                    chunkInstance.reset();
                } finally {
                    syncContainer.release();
                }

                if (chunkPool.size() < grid.getTotalSlots() + chunkPoolMaxOverflow)
                    chunkPool.push(chunkInstance);
                else
                    chunkInstance.dispose();

                assessed++;
                continue;
            }

            GridSlotHandle gridSlotHandle = grid.getGridSlotForChunk(chunkCoordinate);

            if (gridSlotHandle == null) {
                unloadRequests.add(chunkCoordinate);
                activeChunks.put(chunkCoordinate, chunkInstance);
                assessed++;
                continue;
            }

            QueueOperation operation = determineQueueOperation(grid, chunkInstance, gridSlotHandle);

            switch (operation) {
                case LOAD -> generationBranch.getNewChunk(chunkInstance);
                case ASSESSMENT -> assessmentBranch.assessChunk(chunkInstance);
                case BUILD -> buildBranch.buildChunk(chunkInstance);
                case MERGE -> mergeBranch.mergeChunk(chunkInstance);
                case ITEM_LOAD -> itemLoadBranch.loadItems(chunkInstance);
                case ITEM_RENDER -> itemRenderBranch.renderItems(chunkInstance);
                case BATCH -> batchBranch.batchChunk(chunkInstance, grid);
                case RENDER -> {
                    if (gpuUploadsThisFrame < chunkGpuUploadBudget) {
                        renderBranch.renderChunk(chunkInstance);
                        gpuUploadsThisFrame++;
                    }
                }
                case DUMP -> dumpBranch.dumpChunkData(grid, chunkInstance, gridSlotHandle);
                case SKIP -> {
                }
            }

            activeChunks.put(chunkCoordinate, chunkInstance);
            assessed++;
        }
    }

    // Flush \\

    private void flushActiveChunks(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();
        LongLinkedOpenHashSet unloadRequests = grid.getUnloadRequests();

        var iterator = activeChunks.long2ObjectEntrySet().iterator();

        while (iterator.hasNext()) {

            var entry = iterator.next();
            long chunkCoordinate = entry.getLongKey();
            ChunkInstance chunkInstance = entry.getValue();
            iterator.remove();

            ChunkDataSyncContainer sync = chunkInstance.getChunkDataSyncContainer();

            if (!sync.tryAcquire()) {
                activeChunks.put(chunkCoordinate, chunkInstance);
                unloadRequests.add(chunkCoordinate);
                continue;
            }

            try {
                worldRenderManager.removeChunkInstance(chunkCoordinate);
                chunkInstance.reset();
            } finally {
                sync.release();
            }

            if (chunkPool.size() < grid.getTotalSlots() + chunkPoolMaxOverflow)
                chunkPool.push(chunkInstance);
            else
                chunkInstance.dispose();
        }
    }

    // Operation \\

    private QueueOperation determineQueueOperation(
            GridInstance grid,
            ChunkInstance chunkInstance,
            GridSlotHandle gridSlotHandle) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return QueueOperation.SKIP;

        try {
            GridSlotDetailLevel slotLevel = gridSlotHandle.getDetailLevel();

            // Live, not level-derived: a NEAR/DISTANT chunk only needs its own
            // individual GPU upload if the grid's own render queue is actually
            // going to draw it that way — i.e. its mega can never fully
            // assemble (render-distance boundary). Every other NEAR/DISTANT
            // chunk is drawn exclusively through its mega and should never
            // touch RENDER_DATA at all. See ChunkDataUtility.
            boolean needsIndividualRender = grid.getChunkRenderQueue().containsKey(chunkInstance.getCoordinate());

            ChunkData toDump = ChunkDataUtility.nextToDump(syncContainer.getData(), slotLevel, needsIndividualRender);

            if (toDump != null)
                return QueueOperation.DUMP;

            ChunkData toLoad = ChunkDataUtility.nextToLoad(syncContainer.getData(), slotLevel, needsIndividualRender);

            if (toLoad != null) {

                QueueOperation operation = toOperation(toLoad);

                // Pool-wide backpressure. Checked before reserving this chunk's
                // per-stage work flag so a saturated pool never marks a stage
                // in-progress with nothing actually dispatched to run it — the
                // chunk just gets reassessed next frame, same as a RENDER that
                // misses the GPU upload budget above.
                if (isAsyncOperation(operation) && !worldStreamingThreadHandle.hasCapacity())
                    return QueueOperation.SKIP;

                if (!reserveAsyncWork(syncContainer, operation))
                    return QueueOperation.SKIP;

                return operation;
            }

            return QueueOperation.SKIP;
        } finally {
            syncContainer.release();
        }
    }

    private boolean isAsyncOperation(QueueOperation operation) {
        return operation == QueueOperation.LOAD
                || operation == QueueOperation.BUILD
                || operation == QueueOperation.MERGE
                || operation == QueueOperation.ITEM_LOAD
                || operation == QueueOperation.BATCH;
    }

    private QueueOperation toOperation(ChunkData stage) {
        return switch (stage) {
            case LOAD_DATA -> QueueOperation.LOAD;
            case ESSENTIAL_DATA -> QueueOperation.LOAD;
            case GENERATION_DATA -> QueueOperation.LOAD;
            case NEIGHBOR_DATA -> QueueOperation.ASSESSMENT;
            case BUILD_DATA -> QueueOperation.BUILD;
            case MERGE_DATA -> QueueOperation.MERGE;
            case RENDER_DATA -> QueueOperation.RENDER;
            case BATCH_DATA -> QueueOperation.BATCH;
            case ITEM_DATA -> QueueOperation.ITEM_LOAD;
            case ITEM_RENDER_DATA -> QueueOperation.ITEM_RENDER;
            default -> QueueOperation.SKIP;
        };
    }

    private boolean reserveAsyncWork(
            ChunkDataSyncContainer syncContainer,
            QueueOperation operation) {
        return switch (operation) {
            case LOAD -> syncContainer.beginWorkLocked(ChunkDataSyncContainer.WORK_LOAD);
            case BUILD -> syncContainer.beginWorkLocked(ChunkDataSyncContainer.WORK_BUILD);
            case MERGE -> syncContainer.beginWorkLocked(ChunkDataSyncContainer.WORK_MERGE);
            case ITEM_LOAD -> syncContainer.beginWorkLocked(ChunkDataSyncContainer.WORK_ITEM_LOAD);
            case BATCH -> syncContainer.beginWorkLocked(ChunkDataSyncContainer.WORK_BATCH);
            default -> true;
        };
    }

    // Invalidation \\

    void invalidateChunkBatch(ChunkInstance chunk) {

        ChunkDataSyncContainer sync = chunk.getChunkDataSyncContainer();

        if (!sync.tryAcquire())
            return;

        try {
            sync.getData()[ChunkData.BATCH_DATA.index] = false;
        } finally {
            sync.release();
        }
    }
}