package application.bootstrap.worldpipeline.chunkstreammanager;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkDataUtility;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.queue.QueueInstance;
import engine.util.queue.QueueItemHandle;

class ChunkQueueManager extends ManagerPackage {

    /*
     * Drives the per-frame chunk queue for every grid: scan, load, assess.
     * Admission and dispatch are both paced by the WorldStreaming pool's
     * capacity and per-frame budgets, and GPU uploads by their own budget, so
     * streaming never outruns the pipeline or stalls a frame. Pooled chunks are
     * reused only under their own lock, and unloading a chunk invalidates any
     * mega it fed.
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
    private ObjectArrayList<ChunkInstance> chunkPool;
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

        for (ChunkQueueItem item : ChunkQueueItem.VALUES) {
            QueueItemHandle handle = chunkQueue.addQueueItem(item.name());
            id2QueueItem.put(handle.getQueueItemID(), item);
        }

        // Pool
        this.chunkPool = new ObjectArrayList<>();
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
        this.worldStreamingThreadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
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

    private void loadQueue(GridInstance grid) {

        if (!worldStreamingThreadHandle.hasCapacity())
            return;

        LongLinkedOpenHashSet loadRequests = grid.getLoadRequests();
        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();

        var iterator = loadRequests.iterator();
        int loaded = 0;

        while (iterator.hasNext() && loaded < maxChunkAdmissionsPerFrame) {

            long chunkCoordinate = iterator.nextLong();

            if (chunkPool.isEmpty()) {

                iterator.remove();

                ChunkInstance freshInstance = create(ChunkInstance.class);
                freshInstance.constructor(
                        worldRenderManager,
                        grid.getWorldHandle(),
                        chunkCoordinate,
                        chunkStreamManager.getChunkVAO(),
                        airBlockId,
                        blockManager,
                        activeChunks);

                activeChunks.put(chunkCoordinate, freshInstance);
                loaded++;
                continue;
            }

            ChunkInstance pooledInstance = chunkPool.top();
            ChunkDataSyncContainer syncContainer = pooledInstance.getChunkDataSyncContainer();

            if (!syncContainer.tryAcquire())
                break;

            try {
                chunkPool.pop();
                iterator.remove();

                pooledInstance.constructor(
                        worldRenderManager,
                        grid.getWorldHandle(),
                        chunkCoordinate,
                        chunkStreamManager.getChunkVAO(),
                        airBlockId,
                        blockManager,
                        activeChunks);
            } finally {
                syncContainer.release();
            }

            activeChunks.put(chunkCoordinate, pooledInstance);
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

                worldStreamManager.invalidateMegaForChunk(chunkCoordinate);

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

            worldStreamManager.invalidateMegaForChunk(chunkCoordinate);

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

            // A chunk covered by a mega keeps rendering on its own until that mega is on the GPU
            long chunkCoordinate = chunkInstance.getCoordinate();
            boolean coveredByMega = !grid.getChunkRenderQueue().containsKey(chunkCoordinate);
            boolean needsIndividualRender = !coveredByMega
                    || !worldRenderManager.isMegaRendered(Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate));

            // Contribution to the mega never waits on the mega being rendered, since it produces that state
            boolean partOfMegaBlock = coveredByMega && slotLevel.renderMode == RenderType.BATCHED;

            ChunkData toDump = ChunkDataUtility.nextToDump(
                    syncContainer.getData(), slotLevel, needsIndividualRender, partOfMegaBlock);

            if (toDump != null)
                return QueueOperation.DUMP;

            ChunkData toLoad = ChunkDataUtility.nextToLoad(
                    syncContainer.getData(), slotLevel, needsIndividualRender, partOfMegaBlock);

            if (toLoad != null) {

                QueueOperation operation = toOperation(toLoad);

                // A saturated pool leaves the chunk for next frame before any work flag is reserved
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