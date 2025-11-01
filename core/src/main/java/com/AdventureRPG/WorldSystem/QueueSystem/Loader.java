package com.AdventureRPG.WorldSystem.QueueSystem;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.ThreadManager.ThreadManager;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.QueueSystem.Queue.QueueProcess;

public class Loader {

    // Game Manager
    private final ThreadManager threadManager;
    private final ChunkData chunkData;
    private final WorldSystem worldSystem;

    // Async System
    private final Queue<Long> loadRequests;
    private final Queue<Chunk> generationRequests;
    private final Queue<Chunk> assessmentRequests;
    private final Queue<Chunk> buildRequests;
    private final Queue<Chunk> batchRequests;

    // Queue System
    private final InternalQueueProcess[] queueProcess;
    private int queueBatch;
    private final int processPerBatch;
    private int loadedChunksThisFrame;

    // Base \\

    public Loader(WorldSystem worldSystem) {

        // Game Manager
        this.threadManager = worldSystem.threadManager;
        this.chunkData = worldSystem.saveSystem.chunkData;
        this.worldSystem = worldSystem;

        // Queue System
        this.loadRequests = new ConcurrentLinkedQueue<>();
        this.generationRequests = new ConcurrentLinkedQueue<>();
        this.assessmentRequests = new ConcurrentLinkedQueue<>();
        this.buildRequests = new ConcurrentLinkedQueue<>();
        this.batchRequests = new ConcurrentLinkedQueue<>();

        // Queue System
        this.queueProcess = new InternalQueueProcess[] {
                InternalQueueProcess.Load,
                InternalQueueProcess.Generate,
                InternalQueueProcess.Assessment,
                InternalQueueProcess.Build,
                InternalQueueProcess.Batch
        };
        this.queueBatch = 0;
        this.processPerBatch = 32;
        this.loadedChunksThisFrame = 0;
    }

    public void update() {
        processData();
    }

    // Async System \\

    public void requestLoad(long chunkCoordinate) {
        loadRequests.add(chunkCoordinate);
    }

    public void requestGenerate(Chunk chunk) {
        generationRequests.add(chunk);
    }

    public void requestAssessment(Chunk chunk) {
        assessmentRequests.add(chunk);
    }

    public void requestBuild(Chunk chunk) {
        buildRequests.add(chunk);
    }

    public void requestBatch(Chunk chunk) {
        batchRequests.add(chunk);
    }

    // Queue System \\

    public void processData() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        // If there is no queue we don't need to do anything
        if (!hasQueueData())
            return;

        // Alternating queue update
        for (int i = 0; i < queueProcess.length; i++) {

            if (queueProcess[queueBatch].process(this))
                return;

            queueBatch = (queueBatch + 1) % queueProcess.length;
        }
    }

    private boolean hasQueueData() {
        return loadRequests.size() > 0 ||
                generationRequests.size() > 0 ||
                buildRequests.size() > 0;
    }

    enum InternalQueueProcess {

        Load {
            boolean process(Loader loader) {
                return loader.processLoadData();
            }
        },

        Generate {
            boolean process(Loader loader) {
                return loader.processGenerationData();
            }
        },

        Assessment {
            boolean process(Loader loader) {
                return loader.processAssessmentData();
            }
        },

        Build {
            boolean process(Loader loader) {
                return loader.processBuildData();
            }
        },

        Batch {
            boolean process(Loader loader) {
                return loader.processBatchData();
            }
        };

        abstract boolean process(Loader loader);
    }

    // Load \\

    private boolean processLoadData() {

        int index = 0;

        while (!loadRequests.isEmpty() && processIsSafe(index)) {

            Long chunkCoordinate = loadRequests.poll();

            // Run load in General-Thread
            threadManager.submitGeneral(() -> {

                Chunk chunk = chunkData.readChunk(chunkCoordinate);

                if (chunk == null)
                    chunk = new Chunk(worldSystem, chunkCoordinate);
            });

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Generate \\

    private boolean processGenerationData() {

        int index = 0;

        while (!generationRequests.isEmpty() && processIsSafe(index)) {

            Chunk loadedChunk = generationRequests.poll();

            if (loadedChunk == null)
                continue;

            // Run load in dedicated Generation-Thread
            threadManager.submitGeneration(() -> {
                loadedChunk.queueProcess(QueueProcess.Generate);
            });

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Assessment \\

    private boolean processAssessmentData() {

        int index = 0;

        while (!assessmentRequests.isEmpty() && processIsSafe(index)) {

            Chunk loadedChunk = assessmentRequests.poll();

            if (loadedChunk == null)
                continue;

            // Run load in dedicated Generation-Thread
            threadManager.submitGeneral(() -> {
                loadedChunk.queueProcess(QueueProcess.Assessment);
            });

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Build \\

    private boolean processBuildData() {

        int index = 0;

        while (!buildRequests.isEmpty() && processIsSafe(index)) {

            Chunk loadedChunk = buildRequests.poll();

            if (loadedChunk == null)
                continue;

            // Run load in another thread
            threadManager.submitGeneral(() -> {
                loadedChunk.queueProcess(QueueProcess.Build);
            });

            // Increment counters on main thread
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Batch \\

    private boolean processBatchData() {

        int index = 0;

        while (!batchRequests.isEmpty() && processIsSafe(index)) {

            Chunk loadedChunk = batchRequests.poll();

            if (loadedChunk == null)
                continue;

            // Run load in another thread
            threadManager.submitGeneral(() -> {
                loadedChunk.queueProcess(QueueProcess.Batch);
            });

            // Increment counters on main thread
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Queue Utility \\

    private boolean processIsSafe(int index) {
        return index < processPerBatch &&
                loadedChunksThisFrame < GlobalConstant.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private boolean totalProcessThisFrame() {
        return loadedChunksThisFrame >= GlobalConstant.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private int incrementQueueTotal(int index) {

        loadedChunksThisFrame++;

        return ++index;
    }
}
