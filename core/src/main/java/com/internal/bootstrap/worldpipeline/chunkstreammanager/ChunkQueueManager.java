package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.biomemanager.BiomeManager;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataUtility;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.AssessmentBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.BatchBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.BuildBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.ChunkQueueItem;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.DumpBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.GenerationBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.ItemLoadBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.ItemRenderBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.MergeBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.RenderBranch;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotDetailLevel;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.queue.QueueInstance;
import com.internal.core.util.queue.QueueItemHandle;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import java.util.ArrayDeque;

/*
 * Drives the per-frame chunk queue: scans grid slots for load requests, loads
 * chunks from pool, and assesses active chunks to determine their next required
 * operation. All branch dispatch happens here; branches own the implementation.
 *
 * Load/dump decisions are fully delegated to ChunkDataUtility which walks the
 * requires/leadsTo graph declared on ChunkData. No stage ordering logic lives here.
 */
class ChunkQueueManager extends ManagerPackage {

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;
    private GridManager gridManager;
    private ChunkStreamManager chunkStreamManager;
    private WorldRenderManager worldRenderSystem;

    private GenerationBranch generationBranch;
    private AssessmentBranch assessmentBranch;
    private BuildBranch buildBranch;
    private MergeBranch mergeBranch;
    private ItemLoadBranch itemLoadBranch;
    private ItemRenderBranch itemRenderBranch;
    private BatchBranch batchBranch;
    private RenderBranch renderBranch;
    private DumpBranch dumpBranch;

    // Block Management
    private short airBlockId;
    private short defaultBiomeId;

    // Chunk Queue
    private QueueInstance chunkQueue;
    private Int2ObjectOpenHashMap<ChunkQueueItem> id2QueueItem;
    private LongLinkedOpenHashSet loadRequests;
    private LongLinkedOpenHashSet unloadRequests;
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;

    // Chunk Pool
    private ArrayDeque<ChunkInstance> chunkPool;
    private int CHUNK_POOL_MAX_OVERFLOW;

    // Internal \\

    @Override
    protected void create() {

        this.generationBranch = create(GenerationBranch.class);
        this.assessmentBranch = create(AssessmentBranch.class);
        this.buildBranch = create(BuildBranch.class);
        this.mergeBranch = create(MergeBranch.class);
        this.itemLoadBranch = create(ItemLoadBranch.class);
        this.itemRenderBranch = create(ItemRenderBranch.class);
        this.batchBranch = create(BatchBranch.class);
        this.renderBranch = create(RenderBranch.class);
        this.dumpBranch = create(DumpBranch.class);

        this.chunkQueue = create(QueueInstance.class);
        this.id2QueueItem = new Int2ObjectOpenHashMap<>();

        for (ChunkQueueItem item : ChunkQueueItem.values()) {
            QueueItemHandle handle = chunkQueue.addQueueItem(item.name());
            id2QueueItem.put(handle.getQueueItemID(), item);
        }

        this.loadRequests = new LongLinkedOpenHashSet();
        this.unloadRequests = new LongLinkedOpenHashSet();
        this.chunkPool = new ArrayDeque<>();
        this.CHUNK_POOL_MAX_OVERFLOW = EngineSetting.CHUNK_POOL_MAX_OVERFLOW;
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.gridManager = get(GridManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.worldRenderSystem = get(WorldRenderManager.class);
    }

    @Override
    protected void awake() {
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName("TerraArcana/Air");
        this.defaultBiomeId = biomeManager.getBiomeIDFromBiomeName(EngineSetting.DEFAULT_BIOME_NAME);
    }

    @Override
    protected void update() {
        executeQueue();
    }

    // Grid Rebuild \\

    void onGridRebuilt() {
        loadRequests.clear();
        unloadRequests.clear();
        flushActiveChunks();
    }

    private void flushActiveChunks() {
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
                worldRenderSystem.removeChunkInstance(chunkCoordinate);
                chunkInstance.reset();
            } finally {
                sync.release();
            }
            int newMax = gridManager.getGrid().getTotalSlots() + CHUNK_POOL_MAX_OVERFLOW;
            if (chunkPool.size() < newMax)
                chunkPool.push(chunkInstance);
            else
                chunkInstance.dispose();
        }
    }

    // Chunk Queue System \\

    private void executeQueue() {
        while (true) {
            QueueItemHandle nextItem = chunkQueue.getNextQueueItem();
            if (nextItem == null)
                break;
            ChunkQueueItem queueItem = id2QueueItem.get(nextItem.getQueueItemID());
            switch (queueItem) {
                case SCAN_GRID_SLOTS -> scanGridSlots();
                case LOAD -> loadQueue();
                case ASSESS_ACTIVE_CHUNKS -> assessActiveChunks();
            }
        }
    }

    // Grid Scan \\

    private void scanGridSlots() {
        GridInstance grid = gridManager.getGrid();
        for (int i = 0; i < EngineSetting.GRID_SLOTS_SCAN_PER_FRAME; i++) {
            GridSlotHandle slot = grid.getNextScanSlot();
            long chunkCoordinate = slot.getChunkCoordinate();
            if (!activeChunks.containsKey(chunkCoordinate))
                loadRequests.add(chunkCoordinate);
        }
    }

    // Load Queue \\

    private void loadQueue() {
        var iterator = loadRequests.iterator();
        if (!iterator.hasNext())
            return;
        long chunkCoordinate = iterator.nextLong();
        iterator.remove();
        ChunkInstance chunkInstance = chunkPool.isEmpty()
                ? create(ChunkInstance.class)
                : chunkPool.poll();
        chunkInstance.constructor(
                worldRenderSystem,
                chunkStreamManager.getActiveWorldHandle(),
                chunkCoordinate,
                chunkStreamManager.getChunkVAO(),
                airBlockId,
                defaultBiomeId,
                activeChunks);
        activeChunks.put(chunkCoordinate, chunkInstance);
    }

    // Assess Active Chunks \\

    private void assessActiveChunks() {

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
                worldRenderSystem.removeChunkInstance(chunkCoordinate);
                chunkInstance.reset();
            } finally {
                syncContainer.release();
            }
            GridInstance grid = gridManager.getGrid();
            if (chunkPool.size() < grid.getTotalSlots() + CHUNK_POOL_MAX_OVERFLOW)
                chunkPool.push(chunkInstance);
            else
                chunkInstance.dispose();
            return;
        }

        GridSlotHandle gridSlotHandle = gridManager.getGrid().getGridSlotForChunk(chunkCoordinate);

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
            case BATCH -> batchBranch.batchChunk(chunkInstance);
            case RENDER -> renderBranch.renderChunk(chunkInstance);
            case DUMP -> dumpBranch.dumpChunkData(chunkInstance, gridSlotHandle);
            case SKIP -> {
            }
        }

        activeChunks.put(chunkCoordinate, chunkInstance);
    }

    // Queue Operation \\

    private QueueOperation determineQueueOperation(
            ChunkInstance chunkInstance,
            GridSlotHandle gridSlotHandle) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();
        if (!syncContainer.tryAcquire())
            return QueueOperation.SKIP;

        try {
            GridSlotDetailLevel slotLevel = gridSlotHandle.getDetailLevel();

            ChunkData toDump = ChunkDataUtility.nextToDump(syncContainer.data, slotLevel);
            if (toDump != null)
                return QueueOperation.DUMP;

            ChunkData toLoad = ChunkDataUtility.nextToLoad(syncContainer.data, slotLevel);
            if (toLoad != null)
                return toOperation(toLoad);

            return QueueOperation.SKIP;

        } finally {
            syncContainer.release();
        }
    }

    private QueueOperation toOperation(ChunkData stage) {
        switch (stage) {
            case LOAD_DATA:
                return QueueOperation.LOAD;
            case ESSENTIAL_DATA:
                return QueueOperation.LOAD;
            case GENERATION_DATA:
                return QueueOperation.LOAD;
            case NEIGHBOR_DATA:
                return QueueOperation.ASSESSMENT;
            case BUILD_DATA:
                return QueueOperation.BUILD;
            case MERGE_DATA:
                return QueueOperation.MERGE;
            case RENDER_DATA:
                return QueueOperation.RENDER;
            case BATCH_DATA:
                return QueueOperation.BATCH;
            case ITEM_DATA:
                return QueueOperation.ITEM_LOAD;
            case ITEM_RENDER_DATA:
                return QueueOperation.ITEM_RENDER;
            default:
                return QueueOperation.SKIP;
        }
    }

    // Utility \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }
}