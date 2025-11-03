package com.AdventureRPG.WorldSystem.QueueSystem.BatchSystem;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.ThreadManager.ThreadManager;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;

public class Loader {

    // Game Manager
    private final ThreadManager threadManager;
    private final BatchSystem batchSystem;

    // Async System
    private final Queue<Chunk> addRequests;

    // Queue System
    private final InternalBatchProcess[] batchProcess;
    private int queueBatch;
    private final int processPerBatch;
    private int loadedChunksThisFrame;

    // Base \\

    public Loader(WorldSystem worldSystem, BatchSystem batchSystem) {

        // Game Manager
        this.threadManager = worldSystem.threadManager;
        this.batchSystem = batchSystem;

        // Async System
        this.addRequests = new ConcurrentLinkedQueue<>();

        // Queue System
        this.batchProcess = new InternalBatchProcess[] {
                InternalBatchProcess.Add
        };
        this.queueBatch = 0;
        this.processPerBatch = 32;
        this.loadedChunksThisFrame = 0;
    }

    public void update() {
        processData();
    }

    // Async System \\

    public void requestAdd(Chunk chunk) {
        addRequests.add(chunk);
    }

    // Queue System \\

    public void processData() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        // If there is no queue we don't need to do anything
        if (!hasQueueData())
            return;

        // Alternating queue update
        for (int i = 0; i < batchProcess.length; i++) {

            if (batchProcess[queueBatch].process(this))
                return;

            queueBatch = (queueBatch + 1) % batchProcess.length;
        }
    }

    private boolean hasQueueData() {
        return addRequests.size() > 0;
    }

    enum InternalBatchProcess {

        Add {
            boolean process(Loader loader) {
                return loader.processAddData();
            }
        };

        abstract boolean process(Loader loader);
    }

    // Add \\

    private boolean processAddData() {

        int processed = 0;

        while (!addRequests.isEmpty() && processIsSafe(processed)) {

            Chunk chunk = addRequests.poll();

            if (chunk == null || !chunk.verify())
                continue;

            batchSystem.addToLoadedChunks(chunk);

            processed = incrementQueueTotal(processed);
        }

        return totalProcessThisFrame();
    }

    // Queue Utility \\

    private boolean processIsSafe(int processed) {
        return processed < processPerBatch &&
                loadedChunksThisFrame < GlobalConstant.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private boolean totalProcessThisFrame() {
        return loadedChunksThisFrame >= GlobalConstant.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private int incrementQueueTotal(int processed) {

        loadedChunksThisFrame++;

        return ++processed;
    }
}
