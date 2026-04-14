package application.bootstrap.worldpipeline.chunkstreammanager;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayDeque;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
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
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.queue.QueueInstance;
import engine.util.queue.QueueItemHandle;

class ChunkQueueManager extends ManagerPackage {

    /*
     * Drives the per-frame chunk queue across all active grids. Each grid owns
     * its own active chunks, load requests, and unload requests. The chunk pool
     * is shared across all grids for efficiency. All branch dispatch is
     * per-grid — branches own the implementation.
     */

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;
    private WorldStreamManager worldStreamManager;
    private ChunkStreamManager chunkStreamManager;
    private WorldRenderManager worldRenderManager;

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
    private short defaultBiomeId;

    // Queue
    private QueueInstance chunkQueue;
    private Int2ObjectOpenHashMap<ChunkQueueItem> id2QueueItem;

    // Pool — shared across all grids
    private ArrayDeque<ChunkInstance> chunkPool;
    private int chunkPoolMaxOverflow;

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
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    @Override
    protected void awake() {
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
        this.defaultBiomeId = biomeManager.getBiomeIDFromBiomeName(EngineSetting.DEFAULT_BIOME_NAME);
    }

    @Override
    protected void update() {
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

        for (int i = 0; i < EngineSetting.GRID_SLOTS_SCAN_PER_FRAME; i++) {
            GridSlotHandle slot = grid.getNextScanSlot();
            long chunkCoordinate = slot.getChunkCoordinate();
            if (!activeChunks.containsKey(chunkCoordinate))
                loadRequests.add(chunkCoordinate);
        }
    }

    // Load \\

    private void loadQueue(GridInstance grid) {

        LongLinkedOpenHashSet loadRequests = grid.getLoadRequests();
        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();

        var iterator = loadRequests.iterator();

        if (!iterator.hasNext())
            return;

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
                defaultBiomeId,
                activeChunks);

        activeChunks.put(chunkCoordinate, chunkInstance);
    }

    // Assessment \\

    private void assessActiveChunks(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = grid.getActiveChunks();
        LongLinkedOpenHashSet unloadRequests = grid.getUnloadRequests();

        if (activeChunks.isEmpty())
            return;

        var iterator = activeChunks.long2ObjectEntrySet().iterator();

        if (!iterator.hasNext())
            return;

        var entry = iterator.next();
        long chunkCoordinate = entry.getLongKey();
        ChunkInstance chunkInstance = entry.getValue();
        iterator.remove();

        if (unloadRequests.contains(chunkCoordinate)) {

            ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

            if (!syncContainer.tryAcquire()) {
                activeChunks.put(chunkCoordinate, chunkInstance);
                return;
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

            return;
        }

        GridSlotHandle gridSlotHandle = grid.getGridSlotForChunk(chunkCoordinate);

        if (gridSlotHandle == null) {
            unloadRequests.add(chunkCoordinate);
            activeChunks.put(chunkCoordinate, chunkInstance);
            return;
        }

        QueueOperation operation = determineQueueOperation(chunkInstance, gridSlotHandle);

        switch (operation) {
            case LOAD -> generationBranch.getNewChunk(chunkInstance);
            case ASSESSMENT -> assessmentBranch.assessChunk(chunkInstance);
            case BUILD -> buildBranch.buildChunk(chunkInstance);
            case MERGE -> mergeBranch.mergeChunk(chunkInstance);
            case ITEM_LOAD -> itemLoadBranch.loadItems(chunkInstance);
            case ITEM_RENDER -> itemRenderBranch.renderItems(chunkInstance);
            case BATCH -> batchBranch.batchChunk(chunkInstance, grid);
            case RENDER -> renderBranch.renderChunk(chunkInstance);
            case DUMP -> dumpBranch.dumpChunkData(chunkInstance, gridSlotHandle);
            case SKIP -> {
            }
        }

        activeChunks.put(chunkCoordinate, chunkInstance);
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
            ChunkInstance chunkInstance,
            GridSlotHandle gridSlotHandle) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return QueueOperation.SKIP;

        try {
            GridSlotDetailLevel slotLevel = gridSlotHandle.getDetailLevel();

            ChunkData toDump = ChunkDataUtility.nextToDump(syncContainer.getData(), slotLevel);

            if (toDump != null)
                return QueueOperation.DUMP;

            ChunkData toLoad = ChunkDataUtility.nextToLoad(syncContainer.getData(), slotLevel);

            if (toLoad != null) {
                QueueOperation operation = toOperation(toLoad);

                if (!reserveAsyncWork(syncContainer, operation))
                    return QueueOperation.SKIP;

                return operation;
            }

            return QueueOperation.SKIP;
        } finally {
            syncContainer.release();
        }
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