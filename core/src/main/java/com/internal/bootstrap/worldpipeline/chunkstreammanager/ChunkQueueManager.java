package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.AssessmentBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.BatchBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.BuildBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.ChunkQueueItem;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.DumpBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.GenerationBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.MergeBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.RenderBranch;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotDetailLevel;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.queue.QueueInstance;
import com.internal.core.util.queue.QueueItemHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import java.util.ArrayDeque;

class ChunkQueueManager extends ManagerPackage {

    // Internal
    private BlockManager blockManager;
    private GridManager gridManager;
    private ChunkStreamManager chunkStreamManager;
    private ChunkPositionSystem chunkPositionSystem;
    private WorldRenderSystem worldRenderSystem;

    private GenerationBranch generationBranch;
    private AssessmentBranch assessmentBranch;
    private BuildBranch buildBranch;
    private MergeBranch mergeBranch;
    private BatchBranch batchBranch;
    private RenderBranch renderBranch;
    private DumpBranch dumpBranch;

    // Block Management
    private short airBlockId;

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

        // Internal
        this.generationBranch = create(GenerationBranch.class);
        this.assessmentBranch = create(AssessmentBranch.class);
        this.buildBranch = create(BuildBranch.class);
        this.mergeBranch = create(MergeBranch.class);
        this.batchBranch = create(BatchBranch.class);
        this.renderBranch = create(RenderBranch.class);
        this.dumpBranch = create(DumpBranch.class);

        // Stream System
        this.chunkQueue = create(QueueInstance.class);
        this.id2QueueItem = new Int2ObjectOpenHashMap<>();

        for (ChunkQueueItem item : ChunkQueueItem.values()) {
            QueueItemHandle handle = chunkQueue.addQueueItem(item.name());
            id2QueueItem.put(handle.getQueueItemID(), item);
        }

        this.loadRequests = new LongLinkedOpenHashSet();
        this.unloadRequests = new LongLinkedOpenHashSet();

        // Pool
        this.chunkPool = new ArrayDeque<>();
        this.CHUNK_POOL_MAX_OVERFLOW = EngineSetting.CHUNK_POOL_MAX_OVERFLOW;
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
        this.gridManager = get(GridManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.chunkPositionSystem = get(ChunkPositionSystem.class);
        this.worldRenderSystem = get(WorldRenderSystem.class);
    }

    @Override
    protected void awake() {

        // Block Management
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName("Air");
    }

    @Override
    protected void update() {
        ExecuteQueue();
    }

    // Chunk Position System \\

    void requestLoad(long chunkCoordinate) {

        if (activeChunks.containsKey(chunkCoordinate))
            return;

        unloadRequests.remove(chunkCoordinate);
        loadRequests.add(chunkCoordinate);
    }

    void requestUnload(long chunkCoordinate) {

        loadRequests.remove(chunkCoordinate);

        if (activeChunks.containsKey(chunkCoordinate))
            unloadRequests.add(chunkCoordinate);
    }

    // Chunk Queue System \\

    private void ExecuteQueue() {

        while (true) {

            QueueItemHandle nextItem = chunkQueue.getNextQueueItem();

            if (nextItem == null)
                break;

            ChunkQueueItem queueItem = id2QueueItem.get(nextItem.getQueueItemID());

            switch (queueItem) {
                case LOAD -> loadQueue();
                case ASSESS_ACTIVE_CHUNKS -> assessActiveChunks();
            }
        }
    }

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
                activeChunks);

        GridSlotHandle gridSlotHandle = chunkPositionSystem.getGridSlotHandleForChunk(chunkCoordinate);
        chunkInstance.setGridSlotHandle(gridSlotHandle);

        activeChunks.put(chunkCoordinate, chunkInstance);
    }

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

        // Pending unload - try to recycle if no thread is holding it
        if (unloadRequests.contains(chunkCoordinate)) {

            ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

            if (syncContainer.isLocked() || !syncContainer.tryAcquire()) {
                // Thread still active - put back, try again next cycle
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

            // Pool if under cap, otherwise dispose
            if (chunkPool.size() < gridManager.getGrid().getTotalSlots() + CHUNK_POOL_MAX_OVERFLOW)
                chunkPool.push(chunkInstance);
            else
                chunkInstance.dispose();

            return;
        }

        // Normal assessment path
        QueueOperation operation = determineQueueOperation(chunkInstance);

        switch (operation) {
            case LOAD -> generationBranch.getNewChunk(chunkInstance);
            case ASSESSMENT -> assessmentBranch.assessChunk(chunkInstance);
            case BUILD -> buildBranch.buildChunk(chunkInstance);
            case MERGE -> mergeBranch.mergeChunk(chunkInstance);
            case BATCH -> batchBranch.batchChunk(chunkInstance);
            case RENDER -> renderBranch.renderChunk(chunkInstance);
            case DUMP -> dumpBranch.dumpChunkData(chunkInstance);
            case SKIP -> {
            }
        }

        // Return to back of map
        activeChunks.put(chunkCoordinate, chunkInstance);
    }

    private QueueOperation determineQueueOperation(ChunkInstance chunkInstance) {

        GridSlotHandle gridSlotHandle = chunkInstance.getGridSlotHandle();
        if (gridSlotHandle == null)
            return QueueOperation.SKIP;

        GridSlotDetailLevel targetLevel = gridSlotHandle.getDetailLevel();
        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (syncContainer.isLocked())
            return QueueOperation.SKIP;

        if (!syncContainer.tryAcquire())
            return QueueOperation.SKIP;

        try {

            ChunkData nextRequired = targetLevel.getNextRequiredData(syncContainer.data);
            if (nextRequired != null) {
                return switch (nextRequired) {
                    case LOAD_DATA, ESSENTIAL_DATA, GENERATION_DATA -> QueueOperation.LOAD;
                    case NEIGHBOR_DATA -> QueueOperation.ASSESSMENT;
                    case BUILD_DATA -> QueueOperation.BUILD;
                    case MERGE_DATA -> QueueOperation.MERGE;
                    case BATCH_DATA -> QueueOperation.BATCH;
                    case RENDER_DATA -> QueueOperation.RENDER;
                };
            }

            ChunkData nextToDump = targetLevel.getNextDataToDump(syncContainer.data);
            if (nextToDump != null)
                return QueueOperation.DUMP;

            return QueueOperation.SKIP;

        } finally {
            syncContainer.release();
        }
    }

    // Utility \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }
}