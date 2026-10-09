package application.bootstrap.worldpipeline.chunkstreammanager;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
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
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.world.WorldEditRegionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
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
     * streaming never outruns the pipeline or stalls a frame. An upload the
     * budget turns away keeps its place at the front of the round robin, so a
     * ready chunk reaches the GPU next frame instead of a full pass later.
     * Pooled chunks are reused only under their own lock, and unloading a chunk
     * invalidates any mega it fed. A live edit restreams only the grid's chunks
     * its region reaches, nearest first, and leaves every other chunk standing.
     * Every chunk leaves through clearChunk(), which takes back all it handed
     * the renderers, its world items included. The moment a grid moves, every
     * chunk it left behind is queued to unload and every load it no longer
     * wants is dropped; unloads drain on their own budget, a chunk with an
     * async task still reserved waiting for it, and no fresh chunk is made
     * while the grid already holds more than it streams, so travelling far
     * and fast never holds more than the grid around the player. A removed
     * grid keeps draining until its last chunk is pooled. A chunk's trees are
     * a layer of their own: refreshTrees() redraws them alone, at once for a
     * change the player made and through the stream for one the world made,
     * never touching the chunk's terrain or its neighbors.
     */

    // Internal
    private BlockManager blockManager;
    private TreeManager treeManager;
    private WorldStreamManager worldStreamManager;
    private ChunkStreamManager chunkStreamManager;
    private WorldRenderManager worldRenderManager;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private ThreadHandle worldStreamingThreadHandle;

    // Branches
    private GenerationBranch generationBranch;
    private AssessmentBranch assessmentBranch;
    private BuildBranch buildBranch;
    private MergeBranch mergeBranch;
    private ItemLoadBranch itemLoadBranch;
    private ItemRenderBranch itemRenderBranch;
    private TreeBuildBranch treeBuildBranch;
    private TreeRenderBranch treeRenderBranch;
    private BatchBranch batchBranch;
    private RenderBranch renderBranch;
    private DumpBranch dumpBranch;

    // Block IDs
    private short airBlockId;

    // Queue
    private QueueInstance chunkQueue;
    private Int2ObjectOpenHashMap<ChunkQueueItem> id2QueueItem;

    // Retired — removed grids still holding chunks that could not unload yet
    private ObjectArrayList<GridInstance> retiredGrids;

    // Pool — shared across all grids
    private ObjectArrayList<ChunkInstance> chunkPool;
    private int chunkPoolMaxOverflow;

    // Streaming
    private int maxChunkStreamPerBatch;
    private int maxChunkUnloadsPerFrame;

    // Settings
    private int itemRenderDataIndex;
    private int treeDataIndex;
    private int treeRenderDataIndex;

    // Admission Backpressure — paces scanning/loading against the pipeline's
    // actual throughput instead of letting either race ahead of it
    private int maxChunkAdmissionsPerFrame;
    private int maxQueuedLoadRequests;

    // GPU Upload Throttle
    private int chunkGpuUploadBudget;
    private int gpuUploadsThisFrame;
    private LongArrayList deferredUploads;

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
        this.treeBuildBranch = create(TreeBuildBranch.class);
        this.treeRenderBranch = create(TreeRenderBranch.class);
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

        // Retired
        this.retiredGrids = new ObjectArrayList<>();

        // Pool
        this.chunkPool = new ObjectArrayList<>();
        this.chunkPoolMaxOverflow = EngineSetting.CHUNK_POOL_MAX_OVERFLOW;

        // Streaming
        this.maxChunkStreamPerBatch = EngineSetting.MAX_CHUNK_STREAM_PER_BATCH;
        this.maxChunkUnloadsPerFrame = EngineSetting.MAX_CHUNK_UNLOADS_PER_FRAME;

        // Settings
        this.itemRenderDataIndex = ChunkData.ITEM_RENDER_DATA.index;
        this.treeDataIndex = ChunkData.TREE_DATA.index;
        this.treeRenderDataIndex = ChunkData.TREE_RENDER_DATA.index;

        // Admission Backpressure
        this.maxChunkAdmissionsPerFrame = EngineSetting.MAX_CHUNK_STREAM_PER_FRAME;
        this.maxQueuedLoadRequests = EngineSetting.MAX_CHUNK_STREAM_PER_QUEUE;

        // GPU Upload Throttle
        this.chunkGpuUploadBudget = EngineSetting.MAX_CHUNK_GPU_UPLOADS_PER_FRAME;
        this.deferredUploads = new LongArrayList();
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
        this.treeManager = get(TreeManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.worldRenderManager = get(WorldRenderManager.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.worldStreamingThreadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
    }

    @Override
    protected void awake() {
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    @Override
    protected void update() {
        this.gpuUploadsThisFrame = 0;
        drainRetiredGrids();
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

        if (!grid.getActiveChunks().isEmpty())
            retiredGrids.add(grid);
    }

    // Every chunk the grid left behind queued to unload, and every load it no longer wants dropped
    void onGridMoved(GridInstance grid) {

        LongLinkedOpenHashSet unloadRequests = grid.getUnloadRequests();
        var activeIterator = grid.getActiveChunks().keySet().iterator();

        while (activeIterator.hasNext()) {

            long chunkCoordinate = activeIterator.nextLong();

            if (grid.getGridSlotForChunk(chunkCoordinate) == null)
                unloadRequests.add(chunkCoordinate);
        }

        var loadIterator = grid.getLoadRequests().iterator();

        while (loadIterator.hasNext())
            if (grid.getGridSlotForChunk(loadIterator.nextLong()) == null)
                loadIterator.remove();
    }

    // Restream \\

    // Walked far to near so the nearest regenerated chunks load first
    void restreamRegion(GridInstance grid, WorldEditRegionStruct region) {

        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();
        LongLinkedOpenHashSet loadRequests = grid.getLoadRequests();
        WorldHandle worldHandle = grid.getWorldHandle();

        for (int i = grid.getTotalSlots() - 1; i >= 0; i--) {

            long chunkCoordinate = grid.getChunkCoordinateForSlot(grid.getGridCoordinate(i));

            if (!region.reachesChunk(worldHandle, chunkCoordinate))
                continue;

            ChunkInstance chunkInstance = activeChunks.remove(chunkCoordinate);

            if (chunkInstance == null)
                continue;

            discardChunk(grid, chunkCoordinate, chunkInstance);
            loadRequests.addAndMoveToFirst(chunkCoordinate);
        }
    }

    // Waits out any work in flight, since every chunk the edit reaches must regenerate from the edited world
    private void discardChunk(GridInstance grid, long chunkCoordinate, ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();
        syncContainer.acquire();

        try {
            grid.getUnloadRequests().remove(chunkCoordinate);
            clearChunk(chunkCoordinate, chunkInstance);
        } finally {
            syncContainer.release();
        }

        worldStreamManager.invalidateMegaForChunk(chunkCoordinate);
        recycleChunk(grid, chunkInstance);
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

                // A grid still holding chunks it left behind waits for them to unload before it grows
                if (activeChunks.size() >= grid.getTotalSlots() + chunkPoolMaxOverflow)
                    break;

                iterator.remove();

                ChunkInstance freshInstance = create(ChunkInstance.class);
                freshInstance.constructor(
                        worldRenderManager,
                        grid.getWorldHandle(),
                        chunkCoordinate,
                        chunkStreamManager.getChunkVAO(),
                        airBlockId,
                        blockManager,
                        treeManager,
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
                        treeManager,
                        activeChunks);
            } finally {
                syncContainer.release();
            }

            activeChunks.put(chunkCoordinate, pooledInstance);
            loaded++;
        }
    }

    // Assessment \\

    // Round robin from the front of the linked map; an assessed chunk moves to the back
    private void assessActiveChunks(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();

        unloadRequestedChunks(grid);

        if (activeChunks.isEmpty())
            return;

        int assessCount = Math.min(maxChunkStreamPerBatch, activeChunks.size());

        deferredUploads.clear();

        for (int assessed = 0; assessed < assessCount; assessed++) {

            long chunkCoordinate = activeChunks.firstLongKey();
            ChunkInstance chunkInstance = activeChunks.getAndMoveToLast(chunkCoordinate);

            if (grid.getUnloadRequests().contains(chunkCoordinate))
                continue;

            GridSlotHandle gridSlotHandle = grid.getGridSlotForChunk(chunkCoordinate);

            if (gridSlotHandle == null) {
                grid.getUnloadRequests().add(chunkCoordinate);
                continue;
            }

            executeOperation(grid, chunkCoordinate, chunkInstance, gridSlotHandle);
        }

        for (int i = deferredUploads.size() - 1; i >= 0; i--)
            grid.promoteChunk(deferredUploads.getLong(i));
    }

    private void executeOperation(
            GridInstance grid,
            long chunkCoordinate,
            ChunkInstance chunkInstance,
            GridSlotHandle gridSlotHandle) {

        switch (determineQueueOperation(grid, chunkInstance, gridSlotHandle)) {
            case LOAD -> generationBranch.getNewChunk(chunkInstance);
            case ASSESSMENT -> assessmentBranch.assessChunk(chunkInstance);
            case BUILD -> buildBranch.buildChunk(chunkInstance);
            case MERGE -> mergeBranch.mergeChunk(chunkInstance);
            case ITEM_LOAD -> itemLoadBranch.loadItems(chunkInstance);
            case ITEM_RENDER -> itemRenderBranch.renderItems(chunkInstance);
            case TREE_BUILD -> treeBuildBranch.buildTrees(chunkInstance);
            case TREE_RENDER -> {
                if (gpuUploadsThisFrame < chunkGpuUploadBudget) {
                    treeRenderBranch.renderTrees(chunkInstance);
                    gpuUploadsThisFrame++;
                } else
                    deferredUploads.add(chunkCoordinate);
            }
            case BATCH -> batchBranch.batchChunk(chunkInstance, grid);
            case RENDER -> {
                if (gpuUploadsThisFrame < chunkGpuUploadBudget) {
                    renderBranch.renderChunk(chunkInstance);
                    gpuUploadsThisFrame++;
                } else
                    deferredUploads.add(chunkCoordinate);
            }
            case DUMP -> dumpBranch.dumpChunkData(grid, chunkInstance, gridSlotHandle);
            case SKIP -> {
            }
        }
    }

    // Unload \\

    private void drainRetiredGrids() {

        for (int i = retiredGrids.size() - 1; i >= 0; i--) {

            GridInstance grid = retiredGrids.get(i);
            unloadRequestedChunks(grid);

            if (grid.getActiveChunks().isEmpty())
                retiredGrids.remove(i);
        }
    }

    // Oldest requests first, within the frame's budget; a chunk that cannot go yet keeps its place for the next frame
    private void unloadRequestedChunks(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();
        var iterator = grid.getUnloadRequests().iterator();
        int attempted = 0;

        while (iterator.hasNext() && attempted < maxChunkUnloadsPerFrame) {

            long chunkCoordinate = iterator.nextLong();
            ChunkInstance chunkInstance = activeChunks.get(chunkCoordinate);

            if (chunkInstance == null) {
                iterator.remove();
                continue;
            }

            attempted++;

            if (!unloadChunk(grid, chunkCoordinate, chunkInstance))
                continue;

            iterator.remove();
            activeChunks.remove(chunkCoordinate);
        }
    }

    // A chunk with an async task still reserved is left for a later frame, so no task ever runs on a pooled chunk
    private boolean unloadChunk(GridInstance grid, long chunkCoordinate, ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return false;

        try {

            if (syncContainer.hasWorkLocked())
                return false;

            clearChunk(chunkCoordinate, chunkInstance);
        } finally {
            syncContainer.release();
        }

        worldStreamManager.invalidateMegaForChunk(chunkCoordinate);
        recycleChunk(grid, chunkInstance);

        return true;
    }

    // Under the chunk's lock — everything the chunk handed the renderers taken back, and its data let go
    private void clearChunk(long chunkCoordinate, ChunkInstance chunkInstance) {

        if (chunkInstance.getChunkDataSyncContainer().getData()[itemRenderDataIndex])
            worldItemPlacementSystem.pullChunkFromRenderer(chunkCoordinate);

        worldRenderManager.removeChunkInstance(chunkCoordinate);
        worldRenderManager.removeChunkTrees(chunkCoordinate);
        chunkInstance.reset();
    }

    private void recycleChunk(GridInstance grid, ChunkInstance chunkInstance) {

        if (chunkPool.size() < grid.getTotalSlots() + chunkPoolMaxOverflow)
            chunkPool.push(chunkInstance);
        else
            chunkInstance.dispose();
    }

    // Flush \\

    private void flushActiveChunks(GridInstance grid) {

        LongLinkedOpenHashSet unloadRequests = grid.getUnloadRequests();
        var iterator = grid.getActiveChunks().long2ObjectEntrySet().iterator();

        while (iterator.hasNext()) {

            var entry = iterator.next();
            long chunkCoordinate = entry.getLongKey();

            if (unloadChunk(grid, chunkCoordinate, entry.getValue()))
                iterator.remove();
            else
                unloadRequests.add(chunkCoordinate);
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

            // A chunk's own trees hold on past its mega taking over only until the mega's stand-ins are drawn
            boolean treesDrawnIndividually = !coveredByMega
                    || (syncContainer.getData()[treeRenderDataIndex]
                            && !worldRenderManager.isMegaTreesDrawn(
                                    Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate)));

            ChunkData toDump = ChunkDataUtility.nextToDump(
                    syncContainer.getData(), slotLevel, needsIndividualRender, treesDrawnIndividually,
                    partOfMegaBlock);

            if (toDump != null)
                return QueueOperation.DUMP;

            ChunkData toLoad = ChunkDataUtility.nextToLoad(
                    syncContainer.getData(), slotLevel, needsIndividualRender, treesDrawnIndividually,
                    partOfMegaBlock);

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
                || operation == QueueOperation.TREE_BUILD
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
            case TREE_DATA -> QueueOperation.TREE_BUILD;
            case TREE_RENDER_DATA -> QueueOperation.TREE_RENDER;
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
            case TREE_BUILD -> syncContainer.beginWorkLocked(ChunkDataSyncContainer.WORK_TREE);
            case BATCH -> syncContainer.beginWorkLocked(ChunkDataSyncContainer.WORK_BATCH);
            default -> true;
        };
    }

    // Trees \\

    // A built chunk's trees drawn again after one of them changed — at once, upload included, for a change the
    // player made; otherwise handed back to the stream, the old trees showing until the new ones land. A chunk
    // whose trees were never built draws them fresh when the stream first does
    void refreshTrees(ChunkInstance chunk, boolean immediate) {

        ChunkDataSyncContainer syncContainer = chunk.getChunkDataSyncContainer();
        syncContainer.acquire();

        try {
            boolean[] data = syncContainer.getData();

            if (!data[treeDataIndex])
                return;

            if (!immediate || !treeBuildBranch.buildLocked(chunk)) {
                ChunkDataUtility.cascadeClear(ChunkData.TREE_DATA, data);
                return;
            }

            if (data[treeRenderDataIndex])
                data[treeRenderDataIndex] = treeRenderBranch.uploadLocked(chunk);
        } finally {
            syncContainer.release();
        }
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