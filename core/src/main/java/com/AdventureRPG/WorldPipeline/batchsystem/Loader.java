package com.AdventureRPG.WorldPipeline.batchsystem;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.WorldPipeline.WorldPipeline;
import com.AdventureRPG.WorldPipeline.chunks.Chunk;
import com.AdventureRPG.WorldPipeline.queuesystem.QueueSystem;
import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.threadpipeline.ThreadSystem;
import com.AdventureRPG.core.util.Mathematics.Extras.Coordinate2Int;

public class Loader extends SystemPackage {

    // Root
    private ThreadSystem threadSystem;
    private BatchSystem batchSystem;

    // Async System
    private Queue<Chunk> addRequests;
    private Queue<Chunk> creationRequests;
    private Queue<Chunk> assessmentRequests;
    private Queue<Chunk> batchRequests;
    private Queue<Chunk> renderRequests;

    // Queue System
    private InternalBatchProcess[] batchProcess;
    private int queueBatch;
    private int processPerBatch;
    private int loadedChunksThisFrame;

    // Base \\

    @Override
    protected void create() {

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

    @Override
    protected void get() {

        // Root
        this.threadSystem = get(ThreadSystem.class);
        this.batchSystem = get(BatchSystem.class);
    }

    @Override
    protected void update() {

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
                loadedChunksThisFrame < EngineSetting.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private boolean totalProcessThisFrame() {
        return loadedChunksThisFrame >= EngineSetting.MAX_CHUNK_LOADS_PER_FRAME;
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
