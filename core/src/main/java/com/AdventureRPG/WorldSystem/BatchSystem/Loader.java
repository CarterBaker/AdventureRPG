package com.AdventureRPG.WorldSystem.BatchSystem;

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
    private final Queue<Chunk> creationRequests;
    private final Queue<Chunk> assessmentRequests;
    private final Queue<Chunk> batchRequests;
    private final Queue<Chunk> renderRequests;

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
        this.creationRequests = new ConcurrentLinkedQueue<>();
        this.assessmentRequests = new ConcurrentLinkedQueue<>();
        this.batchRequests = new ConcurrentLinkedQueue<>();
        this.renderRequests = new ConcurrentLinkedQueue<>();

        // Queue System
        this.batchProcess = new InternalBatchProcess[] {
                InternalBatchProcess.Add,
                InternalBatchProcess.Create,
                InternalBatchProcess.Assessment,
                InternalBatchProcess.Batch,
                InternalBatchProcess.Render
        };
        this.queueBatch = 0;
        this.processPerBatch = 32;
        this.loadedChunksThisFrame = 0;
    }

    public void update() {
        processData();
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
        return !addRequests.isEmpty() ||
                !creationRequests.isEmpty() ||
                !assessmentRequests.isEmpty() ||
                !batchRequests.isEmpty() ||
                !renderRequests.isEmpty();
    }

    enum InternalBatchProcess {

        Add {
            boolean process(Loader loader) {
                return loader.processAddData();
            }
        },

        Create {
            boolean process(Loader loader) {
                return loader.processCreationData();
            }
        },

        Assessment {
            boolean process(Loader loader) {
                return loader.processAssessmentData();
            }
        },

        Batch {
            boolean process(Loader loader) {
                return loader.processBatchData();
            }
        },

        Render {
            boolean process(Loader loader) {
                return loader.processRenderData();
            }
        };

        abstract boolean process(Loader loader);
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

    // External Queueing \\

    public void requestAdd(Chunk chunk) {
        addRequests.add(chunk);
    }

    public void requestCreate(Chunk chunk) {
        creationRequests.add(chunk);
    }

    public void requestAssessment(Chunk chunk) {
        assessmentRequests.add(chunk);
    }

    public void requestBatch(Chunk chunk) {
        batchRequests.add(chunk);
    }

    public void requestRender(Chunk chunk) {
        renderRequests.add(chunk);
    }

    // Add \\

    private boolean processAddData() {

        int processed = 0;

        while (!addRequests.isEmpty() && processIsSafe(processed)) {

            Chunk loadedChunk = addRequests.poll();

            if (loadedChunk == null)
                continue;

            batchSystem.addChunkToBatch(loadedChunk);

            // Increment counters
            processed = incrementQueueTotal(processed);
        }

        return totalProcessThisFrame();
    }

    // Creation \\

    private boolean processCreationData() {

        int processed = 0;

        while (!creationRequests.isEmpty() && processIsSafe(processed)) {

            Chunk chunk = creationRequests.poll();

            if (chunk == null)
                continue;

            processed = incrementQueueTotal(processed);
        }

        return totalProcessThisFrame();
    }

    // Assessment \\

    private boolean processAssessmentData() {

        int processed = 0;

        while (!assessmentRequests.isEmpty() && processIsSafe(processed)) {

            Chunk chunk = assessmentRequests.poll();
            if (chunk == null)
                continue;

            processed = incrementQueueTotal(processed);
        }

        return totalProcessThisFrame();
    }

    // Batch \\

    private boolean processBatchData() {

        int processed = 0;

        while (!batchRequests.isEmpty() && processIsSafe(processed)) {

            Chunk chunk = batchRequests.poll();
            if (chunk == null)
                continue;

            processed = incrementQueueTotal(processed);
        }

        return totalProcessThisFrame();
    }

    // Render \\

    private boolean processRenderData() {

        int processed = 0;

        while (!renderRequests.isEmpty() && processIsSafe(processed)) {

            Chunk chunk = renderRequests.poll();
            if (chunk == null)
                continue;

            processed = incrementQueueTotal(processed);
        }

        return totalProcessThisFrame();
    }
}
