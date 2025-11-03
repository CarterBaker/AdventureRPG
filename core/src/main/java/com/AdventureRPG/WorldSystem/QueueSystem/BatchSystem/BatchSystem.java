package com.AdventureRPG.WorldSystem.QueueSystem.BatchSystem;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.QueueSystem.QueueSystem;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class BatchSystem {

    // Debug
    private final boolean debug = false; // TODO: Debug line

    // Game Manager
    private final WorldTick worldTick;
    private final Loader loader;
    private final QueueSystem queueSystem;

    // Settings
    private final int MAX_CHUNK_LOADS_PER_FRAME;
    private final int MAX_CHUNK_LOADS_PER_TICK;

    // Async System
    private final Queue<Chunk> batchRequests;

    // Chunk Tracking
    private Long2ObjectOpenHashMap<Chunk> loadedChunks;

    // Queue System
    private int loadedChunksThisFrame;
    private int loadedChunksThisTick;

    // Base \\

    public BatchSystem(WorldSystem worldSystem, QueueSystem queueSystem) {

        // Game Manager
        this.worldTick = worldSystem.worldTick;
        this.loader = new Loader(worldSystem, this);
        this.queueSystem = queueSystem;

        // Settings
        this.MAX_CHUNK_LOADS_PER_FRAME = GlobalConstant.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = GlobalConstant.MAX_CHUNK_LOADS_PER_TICK;

        // Async System
        this.batchRequests = new ConcurrentLinkedQueue<>();

        // Chunk Tracking
        this.loadedChunks = new Long2ObjectOpenHashMap<>(queueSystem.totalChunks());

        // Queue System
        this.loadedChunksThisFrame = 0;
        this.loadedChunksThisTick = 0;
    }

    public void awake() {

    }

    public void start() {

    }

    public void update() {

        updateQueue();
        loader.update();
    }

    public void render() {

    }

    public void dispose() {

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
        batchRequests.add(chunk);
    }

    public void addToLoadedChunks(Chunk chunk) {
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

            case Add ->
                System.out.println("Working 2");

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

        return switch (process) {

            case Add ->
                BatchProcess.Add.bundle.size();

            case Batch ->
                BatchProcess.Batch.bundle.size();
        };
    }

    // Debug \\

    private void debug(String input) {
        System.out.println("[BatchSystem] " + input);
    }
}
