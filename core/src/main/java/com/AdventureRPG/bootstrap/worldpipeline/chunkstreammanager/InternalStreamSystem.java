package com.AdventureRPG.bootstrap.worldpipeline.chunkstreammanager;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.bootstrap.geometrypipeline.buildManager.BuildManager;
import com.AdventureRPG.bootstrap.threadpipeline.ThreadSystem;
import com.AdventureRPG.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.AdventureRPG.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.util.queuesystem.QueueInstance;
import com.AdventureRPG.core.util.queuesystem.QueueItemHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class InternalStreamSystem extends SystemPackage {

    // Internal
    private ThreadSystem threadSystem;
    private BuildManager buildManager;
    private WorldGenerationManager worldGenerationManager;
    private QueueInstance queueInstance;

    // Stream System
    private ConcurrentLinkedQueue<Long> loadRequests;
    private ConcurrentLinkedQueue<ChunkInstance> generationRequests;
    private ConcurrentLinkedQueue<ChunkInstance> assessmentRequests;
    private ConcurrentLinkedQueue<ChunkInstance> buildRequests;
    private ConcurrentLinkedQueue<ChunkInstance> batchRequests;

    private ConcurrentLinkedQueue<ChunkInstance> loadedChunks;

    // Queue Utility
    private ChunkStreamQueue activeQueueItem;
    private Int2ObjectOpenHashMap<ChunkStreamQueue> queueItemMap;

    private int processPerBatch;
    private int loadedChunksThisFrame;

    private int WORLD_HEIGHT;

    // Internal \\

    @Override
    protected void create() {

        // Queue Utility - Initialize FIRST before using
        this.queueItemMap = new Int2ObjectOpenHashMap<>();
        this.activeQueueItem = ChunkStreamQueue.LOAD;

        // Internal
        this.queueInstance = create(QueueInstance.class);

        for (ChunkStreamQueue queue : ChunkStreamQueue.values()) {
            QueueItemHandle queueItemHandle = queueInstance.addQueueItem(queue.name(), queue.priority);
            queueItemMap.putIfAbsent(queueItemHandle.getQueueItemID(), queue);
        }

        // Stream System
        this.loadRequests = new ConcurrentLinkedQueue<>();
        this.generationRequests = new ConcurrentLinkedQueue<>();
        this.assessmentRequests = new ConcurrentLinkedQueue<>();
        this.buildRequests = new ConcurrentLinkedQueue<>();
        this.batchRequests = new ConcurrentLinkedQueue<>();

        // Stream Return
        this.loadedChunks = new ConcurrentLinkedQueue<>();

        this.processPerBatch = 32;
        this.loadedChunksThisFrame = 0;

        this.WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;
    }

    @Override
    protected void get() {

        // Internal
        this.threadSystem = create(ThreadSystem.class);
        this.buildManager = create(BuildManager.class);
    }

    @Override
    protected void update() {
        processData();
    }

    // Stream Queue \\

    private void processData() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        while (!totalProcessesThisFrame() && hasQueueData()) {

            QueueItemHandle queueItemHandle = queueInstance.getNextQueueItem();
            activeQueueItem = queueItemMap.get(queueItemHandle.getQueueItemID());

            if (activeQueueItem == null)
                continue;

            switch (activeQueueItem) {
                case LOAD -> processLoadData();
                case GENERATE -> processGenerationData();
                case ASSESSMENT -> {
                }
                case BUILD -> {
                }
                case BATCH -> {
                }
                case UNLOAD -> {
                }
            }
        }

    }

    private boolean hasQueueData() {
        return !loadRequests.isEmpty() ||
                !generationRequests.isEmpty() ||
                !assessmentRequests.isEmpty() ||
                !buildRequests.isEmpty() ||
                !batchRequests.isEmpty();
    }

    // Load \\

    private boolean processLoadData() {

        int index = 0;

        while (!loadRequests.isEmpty() && processIsSafe(index)) {

            Long chunkCoordinate = loadRequests.poll();

            if (chunkCoordinate == null)
                continue;

            // Run load in General-Thread
            threadSystem.submitGeneral(() -> {

                ChunkInstance chunkInstance = create(ChunkInstance.class);
                chunkInstance.constructor(chunkCoordinate);

                loadedChunks.add(chunkInstance);
            });

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessesThisFrame();
    }

    // Generate \\

    private boolean processGenerationData() {

        int index = 0;

        while (!generationRequests.isEmpty() && processIsSafe(index)) {

            ChunkInstance loadedChunk = generationRequests.poll();

            if (loadedChunk == null)
                continue;

            // Run load in dedicated Generation-Thread
            threadSystem.submitGeneration(() -> {

                boolean successfulBuild = true;

                for (int i = 0; i < WORLD_HEIGHT; i++)
                    if (!worldGenerationManager.generateSubChunk(
                            loadedChunk.getChunkCoordinate(),
                            loadedChunk.getSubChunkInstance(i),
                            i))
                        successfulBuild = false;
            });

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessesThisFrame();
    }

    // Utility \\

    private boolean processIsSafe(int index) {
        return index < processPerBatch &&
                loadedChunksThisFrame < EngineSetting.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private boolean totalProcessesThisFrame() {
        return loadedChunksThisFrame <= EngineSetting.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private int incrementQueueTotal(int index) {

        loadedChunksThisFrame++;

        return ++index;
    }

    // Accessible \\

    void requestLoad(long chunkCoordinate) {
        loadRequests.add(chunkCoordinate);
    }

    void requestGenerate(ChunkInstance chunk) {
        generationRequests.add(chunk);
    }

    void requestAssessment(ChunkInstance chunk) {
        assessmentRequests.add(chunk);
    }

    void requestBuild(ChunkInstance chunk) {
        buildRequests.add(chunk);
    }

    void requestBatch(ChunkInstance chunk) {
        batchRequests.add(chunk);
    }

    ChunkInstance pollLoadedChunk() {
        return loadedChunks.poll();
    }
}