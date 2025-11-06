package com.AdventureRPG.WorldSystem.BatchSystem;

import com.AdventureRPG.Core.GameManager;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.QueueSystem.QueueSystem;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class BatchSystem extends GameManager {

    // Root
    private WorldTick worldTick;
    private Loader loader;
    private QueueSystem queueSystem;

    // Settings
    private int MAX_CHUNK_LOADS_PER_FRAME;
    private int MAX_CHUNK_LOADS_PER_TICK;

    // Chunk Tracking
    private Long2ObjectOpenHashMap<Chunk> loadedChunks;

    // Queue System
    private int loadedChunksThisFrame;
    private int loadedChunksThisTick;

    // Base \\

    @Override
    public void init() {

        // Root
        this.worldTick = rootManager.worldSystem.worldTick;
        this.loader = (Loader) register(new Loader());

        // Settings
        this.MAX_CHUNK_LOADS_PER_FRAME = GlobalConstant.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = GlobalConstant.MAX_CHUNK_LOADS_PER_TICK;

        // Chunk Tracking
        this.loadedChunks = new Long2ObjectOpenHashMap<>(queueSystem.totalChunks());

        // Queue System
        this.loadedChunksThisFrame = 0;
        this.loadedChunksThisTick = 0;
    }

    @Override
    public void awake() {

        // Root
        this.queueSystem = rootManager.worldSystem.queueSystem;
    }

    @Override
    public void update() {

        updateQueue();
        loader.update();
    }

    // Update \\

    private void updateQueue() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        // Reset every tick
        if (worldTick.tick())
            loadedChunksThisTick = 0;

        while (!totalProcessThisFrame() && hasQueue()) {

            BatchProcess next = pickNextQueue();

            if (next == null)
                break;

            while (!totalProcessThisFrame() && getQueueSize(next) > 0)
                if (!next.process(this))
                    break;
        }
    }

    private BatchProcess pickNextQueue() {

        BatchProcess priorityQueue = null;
        int priorityScore = 0;

        for (BatchProcess queue : BatchProcess.values()) {

            int queueScore = getQueueScore(queue);

            if (queueScore > priorityScore) {
                priorityQueue = queue;
                priorityScore = queueScore;
            }
        }

        return priorityQueue;
    }

    private int getQueueScore(BatchProcess process) {

        int queueSize = getQueueSize(process);

        if (queueSize == 0)
            return 0;

        return queueSize + process.priority * 100;
    }

    // Main \\

    public void requestBatch(Chunk chunk) {
        loader.requestAdd(chunk);
    }

    public void addChunkToBatch(Chunk chunk) {
        loadedChunks.putIfAbsent(chunk.coordinate, chunk);
    }

    // Queue \\

    public boolean processQueue(BatchProcess batchProcess) {

        int index = 0;
        int size = batchProcess.bundle.size();

        while (index < size && processIsSafe()) {

            long chunkCoordinate = batchProcess.bundle.dequeue();

            dispatchQueueWork(batchProcess, chunkCoordinate);

            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    private void dispatchQueueWork(BatchProcess batchProcess, long chunkCoordinate) {

        Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

        if (loadedChunk == null) {

            loadedChunks.remove(chunkCoordinate);
            return;
        }

        switch (batchProcess) {

            default -> {
                // Unload queue is handled seperate
            }
        }
    }

    // Queue Utility \\

    private boolean processIsSafe() {
        return loadedChunksThisFrame < MAX_CHUNK_LOADS_PER_FRAME &&
                loadedChunksThisTick < MAX_CHUNK_LOADS_PER_TICK;
    }

    private boolean totalProcessThisFrame() {
        return loadedChunksThisFrame >= MAX_CHUNK_LOADS_PER_FRAME ||
                loadedChunksThisTick >= MAX_CHUNK_LOADS_PER_TICK;
    }

    private int incrementQueueTotal(int process) {

        loadedChunksThisFrame++;
        loadedChunksThisTick++;

        return ++process;
    }

    // External Queueing \\

    // Accessible \\

    public boolean hasQueue() {

        // Check primitive bundle-backed queues first
        for (BatchProcess batch : BatchProcess.values())
            if (batch.bundle != null && batch.bundle.size() > 0)
                return true;

        return false;
    }

    public int getQueueSize(BatchProcess process) {
        return (process.bundle != null) ? process.bundle.size() : 0;
    }
}
