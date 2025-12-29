package com.AdventureRPG.WorldPipeline.queuesystem;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.WorldPipeline.WorldPipeline;
import com.AdventureRPG.WorldPipeline.chunks.Chunk;
import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOManager;
import com.AdventureRPG.core.scenepipeline.worldenginesystem.WorldEngineSystem;
import com.AdventureRPG.core.threadpipeline.ThreadSystem;
import com.AdventureRPG.savemanager.ChunkData;
import com.AdventureRPG.savemanager.SaveManager;

public class Loader extends SystemPackage {

    // Root
    private ThreadSystem threadSystem;
    private VAOManager vaoManager;
    private ModelManager modelManager;
    private ChunkData chunkData;
    private WorldEngineSystem worldEngineSystem;
    private WorldPipeline worldPipeline;

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
        this.threadSystem = internal.get(ThreadSystem.class);
        this.vaoManager = internal.get(VAOManager.class);
        this.modelManager = internal.get(ModelManager.class);
        this.chunkData = internal.get(ChunkData.class);
        this.worldEngineSystem = internal.get(WorldEngineSystem.class);
        this.worldPipeline = internal.get(WorldPipeline.class);
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
                loadedChunksThisFrame < EngineSetting.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private boolean totalProcessThisFrame() {
        return loadedChunksThisFrame >= EngineSetting.MAX_CHUNK_LOADS_PER_FRAME;
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

                Chunk chunk = chunkData.readChunk(chunkCoordinate);

                if (chunk == null)
                    chunk = new Chunk(
                            vaoManager,
                            modelManager,
                            worldEngineSystem,
                            worldPipeline,
                            chunkCoordinate);

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
