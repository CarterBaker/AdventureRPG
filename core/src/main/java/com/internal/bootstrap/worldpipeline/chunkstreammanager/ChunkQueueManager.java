package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.util.queue.QueueInstance;
import com.internal.bootstrap.util.queue.QueueItemHandle;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.AssessmentBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.BuildBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.GenerationBranch;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

class ChunkQueueManager extends ManagerPackage {

    // Internal
    private GenerationBranch generationBranch;
    private AssessmentBranch assessmentBranch;
    private BuildBranch buildBranch;

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

        // Stream System
        this.chunkQueue = create(QueueInstance.class);
        this.id2QueueItem = new Int2ObjectOpenHashMap<>();

        for (ChunkQueueItem item : ChunkQueueItem.values()) {

            QueueItemHandle handle = chunkQueue.addQueueItem(item.name());

            id2QueueItem.put(handle.getQueueItemID(), item);
        }

        this.loadRequests = new LongLinkedOpenHashSet();
        this.unloadRequests = new LongLinkedOpenHashSet();
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
        chunkInstance.constructor(chunkCoordinate);

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

        // Process the chunk based on its state
        QueueOperation operation = chunkInstance.getChunkStateOperation();

        switch (operation) {
            case GENERATE -> generationBranch.generateChunk(chunkInstance);
            case NEIGHBOR_ASSESSMENT -> assessmentBranch.assessChunk(chunkInstance);
            case BUILD -> buildBranch.buildChunk(chunkInstance);
        }

        // Add back to end
        activeChunks.put(chunkCoordinate, chunkInstance);
    }

    private void unloadQueue() {

        var iterator = unloadRequests.iterator();
        if (!iterator.hasNext())
            return;

        long chunkCoordinate = iterator.nextLong();
        iterator.remove();

        ChunkInstance chunkInstance = activeChunks.remove(chunkCoordinate);

        if (chunkInstance != null)
            chunkInstance.dispose();
    }

    // Utility \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }
}