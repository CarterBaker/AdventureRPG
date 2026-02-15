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
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotDetailLevel;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.queue.QueueInstance;
import com.internal.core.util.queue.QueueItemHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

class ChunkQueueManager extends ManagerPackage {

    // Internal
    private BlockManager blockManager;
    private ChunkStreamManager chunkStreamManager;
    private ChunkPositionSystem chunkPositionSystem;
    private WorldRenderSystem worldRenderSystem;
    private GenerationBranch generationBranch;
    private AssessmentBranch assessmentBranch;
    private BuildBranch buildBranch;
    private MergeBranch mergeBranch;
    private BatchBranch batchBranch;
    private DumpBranch dumpBranch;

    // Block Management
    private short airBlockId;

    // Chunk Queue
    private QueueInstance chunkQueue;
    private Int2ObjectOpenHashMap<ChunkQueueItem> id2QueueItem;

    private LongLinkedOpenHashSet loadRequests;
    private LongLinkedOpenHashSet unloadRequests;
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.generationBranch = create(GenerationBranch.class);
        this.assessmentBranch = create(AssessmentBranch.class);
        this.buildBranch = create(BuildBranch.class);
        this.mergeBranch = create(MergeBranch.class);
        this.batchBranch = create(BatchBranch.class);
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

        this.assessmentBranch.setActiveChunks(activeChunks);
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
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

        // Loop until we hit the frame limit
        while (true) {

            QueueItemHandle nextItem = chunkQueue.getNextQueueItem();

            if (nextItem == null)
                break; // Frame limit reached

            ChunkQueueItem queueItem = id2QueueItem.get(nextItem.getQueueItemID());

            switch (queueItem) {
                case LOAD -> loadQueue();
                case ASSESS_ACTIVE_CHUNKS -> assessActiveChunks();
                case UNLOAD -> unloadQueue();
            }
        }
    }

    private void loadQueue() {

        var iterator = loadRequests.iterator();
        if (!iterator.hasNext())
            return;

        long chunkCoordinate = iterator.nextLong();
        iterator.remove();

        ChunkInstance chunkInstance = create(ChunkInstance.class);
        chunkInstance.constructor(
                worldRenderSystem,
                chunkStreamManager.getActiveWorldHandle(),
                chunkCoordinate,
                chunkStreamManager.getChunkVAO(),
                airBlockId);

        GridSlotHandle gridSlotHandle = chunkPositionSystem.getGridSlotHandleForChunk(chunkCoordinate);
        chunkInstance.setGridSlotHandle(gridSlotHandle);

        activeChunks.put(chunkCoordinate, chunkInstance);
    }

    private void assessActiveChunks() {

        if (activeChunks.isEmpty())
            return;

        // Get first entry
        var iterator = activeChunks.long2ObjectEntrySet().iterator();
        if (!iterator.hasNext())
            return;

        var entry = iterator.next();
        long chunkCoordinate = entry.getLongKey();
        ChunkInstance chunkInstance = entry.getValue();

        // Remove from front
        iterator.remove();

        // Determine what operation this chunk needs
        QueueOperation operation = determineQueueOperation(chunkInstance);

        switch (operation) {
            case LOAD -> generationBranch.getNewChunk(chunkInstance);
            case ASSESSMENT -> assessmentBranch.assessChunk(chunkInstance);
            case BUILD -> buildBranch.buildChunk(chunkInstance);
            case MERGE -> mergeBranch.mergeChunk(chunkInstance);
            case BATCH -> batchBranch.batchChunk(chunkInstance);
            case DUMP -> dumpBranch.dumpChunkData(chunkInstance);
            case SKIP -> {
            } // No-op, chunk is correct or processing
        }

        // Add back to end
        activeChunks.put(chunkCoordinate, chunkInstance);
    }

    private QueueOperation determineQueueOperation(ChunkInstance chunkInstance) {

        GridSlotHandle gridSlotHandle = chunkInstance.getGridSlotHandle();
        GridSlotDetailLevel targetLevel = gridSlotHandle.getDetailLevel();

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        // Skip if locked
        if (syncContainer.isLocked())
            return QueueOperation.SKIP;

        // Try to acquire lock to read data
        if (!syncContainer.tryAcquire())
            return QueueOperation.SKIP;

        try {

            // Check if we need to progress
            ChunkData nextRequired = targetLevel.getNextRequiredData(syncContainer.data);
            if (nextRequired != null) {
                return switch (nextRequired) {
                    case LOAD_DATA, GENERATION_DATA -> QueueOperation.LOAD;
                    case NEIGHBOR_DATA -> QueueOperation.ASSESSMENT;
                    case BUILD_DATA -> QueueOperation.BUILD;
                    case MERGE_DATA -> QueueOperation.MERGE;
                    case BATCH_DATA -> QueueOperation.BATCH;
                    case RENDER_DATA -> QueueOperation.SKIP;
                };
            }

            // Check if we need to dump (remove data)
            ChunkData nextToDump = targetLevel.getNextDataToDump(syncContainer.data);
            if (nextToDump != null)
                return QueueOperation.DUMP;

            // Chunk is at correct detail level
            return QueueOperation.SKIP;

        }

        finally {
            syncContainer.release();
        }
    }

    private void unloadQueue() {

        var iterator = unloadRequests.iterator();
        if (!iterator.hasNext())
            return;

        long chunkCoordinate = iterator.nextLong();
        iterator.remove();

        worldRenderSystem.removeWorldInstance(chunkCoordinate);

        ChunkInstance chunkInstance = activeChunks.remove(chunkCoordinate);
        if (chunkInstance != null)
            chunkInstance.dispose();
    }

    // Utility \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }
}