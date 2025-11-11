package com.AdventureRPG.WorldManager.QueueSystem;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.Core.Root.SystemFrame;
import com.AdventureRPG.Core.ThreadSystem.ThreadSystem;
import com.AdventureRPG.Core.Util.GlobalConstant;
import com.AdventureRPG.SaveManager.ChunkData;
import com.AdventureRPG.SaveManager.SaveManager;
import com.AdventureRPG.WorldManager.WorldManager;
import com.AdventureRPG.WorldManager.Chunks.Chunk;

public class Loader extends SystemFrame {

    // Root
    private ThreadSystem threadSystem;
    private ChunkData chunkData;
    private WorldManager worldManager;

    // Async System
    private Queue<Long> loadRequests;
    private Queue<Chunk> addRequests;
    private Queue<Chunk> generationRequests;
    private Queue<Chunk> assessmentRequests;
    private Queue<Chunk> buildRequests;
    private Queue<Chunk> batchRequests;

    // Queue System
    private InternalQueueProcess[] queueProcess;
    private int queueBatch;
    private int processPerBatch;
    private int loadedChunksThisFrame;

    // Base \\

    @Override
    protected void create() {

        // Async System
        this.loadRequests = new ConcurrentLinkedQueue<>();
        this.addRequests = new ConcurrentLinkedQueue<>();
        this.generationRequests = new ConcurrentLinkedQueue<>();
        this.assessmentRequests = new ConcurrentLinkedQueue<>();
        this.buildRequests = new ConcurrentLinkedQueue<>();
        this.batchRequests = new ConcurrentLinkedQueue<>();

        // Queue System
        this.queueProcess = new InternalQueueProcess[] {
                InternalQueueProcess.Load,
                InternalQueueProcess.Add,
                InternalQueueProcess.Generate,
                InternalQueueProcess.Assessment,
                InternalQueueProcess.Build,
                InternalQueueProcess.Batch
        };
        this.queueBatch = 0;
        this.processPerBatch = 32;
        this.loadedChunksThisFrame = 0;
    }

    @Override
    protected void init() {

        // Root
        this.threadSystem = rootManager.get(ThreadSystem.class);
        this.chunkData = rootManager.get(SaveManager.class).get(ChunkData.class);
        this.worldManager = rootManager.get(WorldManager.class);
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
        for (int i = 0; i < queueProcess.length; i++) {

            if (queueProcess[queueBatch].process(this))
                return;

            queueBatch = (queueBatch + 1) % queueProcess.length;
        }
    }

    private boolean hasQueueData() {
        return !loadRequests.isEmpty() ||
                !addRequests.isEmpty() ||
                !generationRequests.isEmpty() ||
                !assessmentRequests.isEmpty() ||
                !buildRequests.isEmpty() ||
                !batchRequests.isEmpty();
    }

    enum InternalQueueProcess {

        Load {
            boolean process(Loader loader) {
                return loader.processLoadData();
            }
        },

        Add {
            boolean process(Loader loader) {
                return loader.processAddData();
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

    // External Queueing \\

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

    // Load \\

    private boolean processLoadData() {

        int index = 0;

        while (!loadRequests.isEmpty() && processIsSafe(index)) {

            Long chunkCoordinate = loadRequests.poll();

            // Run load in General-Thread
            threadSystem.submitGeneral(() -> {

                // TODO: debug("Working");

                Chunk chunk = chunkData.readChunk(chunkCoordinate);

                if (chunk == null)
                    chunk = new Chunk(worldManager, chunkCoordinate);

                addRequests.add(chunk);
            });

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Add \\

    private boolean processAddData() {

        int index = 0;

        while (!addRequests.isEmpty() && processIsSafe(index)) {

            // TODO: debug("Working");

            Chunk loadedChunk = addRequests.poll();

            if (loadedChunk == null)
                continue;

            loadedChunk.addChunkToQueue();

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Generate \\

    private boolean processGenerationData() {

        int index = 0;

        while (!generationRequests.isEmpty() && processIsSafe(index)) {

            // TODO: debug("Working");

            Chunk loadedChunk = generationRequests.poll();

            if (loadedChunk == null)
                continue;

            // Run load in dedicated Generation-Thread
            threadSystem.submitGeneration(() -> {
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

            // TODO: debug("Working");

            Chunk loadedChunk = assessmentRequests.poll();

            if (loadedChunk == null)
                continue;

            // Run load in dedicated Generation-Thread
            threadSystem.submitGeneral(() -> {
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

            // TODO: debug("Working");

            Chunk loadedChunk = buildRequests.poll();

            if (loadedChunk == null)
                continue;

            // Run load in another thread
            threadSystem.submitGeneral(() -> {
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

            debug("This does not print");

            Chunk loadedChunk = batchRequests.poll();

            if (loadedChunk == null)
                continue;

            // Run load in another thread
            threadSystem.submitGeneral(() -> {
                loadedChunk.queueProcess(QueueProcess.Batch);
            });

            // Increment counters on main thread
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }
}
