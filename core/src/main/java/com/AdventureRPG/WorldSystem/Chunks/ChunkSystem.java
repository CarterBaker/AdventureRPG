package com.AdventureRPG.WorldSystem.Chunks;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.ThreadSystem.ThreadManager;
import com.AdventureRPG.WorldSystem.WorldGenerator;
import com.AdventureRPG.WorldSystem.WorldSystem;

public class ChunkSystem {

    // Game Manager
    private final Settings settings;
    private final ThreadManager threadManager;
    private final ChunkData chunkData;
    private final WorldSystem worldSystem;
    private WorldGenerator worldGenerator;

    // Settings
    private final int MAX_CHUNK_LOADS_PER_FRAME;

    // Chunk System
    public final ChunkBuilder chunkBuilder;

    // Async System
    private final Queue<Long> loadRequests;
    private final Queue<Chunk> generationRequests;
    private final Queue<Chunk> buildRequests;

    private final Queue<Chunk> loadedResults;
    private final Queue<Chunk> builtResults;

    // Queue System
    private final QueueProcess[] queueProcess;
    private int queueBatch;
    private final int processPerBatch;
    private int loadedChunksThisFrame;

    // Base \\

    public ChunkSystem(GameManager gameManager, WorldSystem worldSystem) {

        // Game Manager
        this.settings = gameManager.settings;
        this.threadManager = gameManager.threadManager;
        this.chunkData = gameManager.saveSystem.chunkData;
        this.worldSystem = worldSystem;

        // Settings
        this.MAX_CHUNK_LOADS_PER_FRAME = settings.MAX_CHUNK_LOADS_PER_FRAME;

        // Chunk System
        this.chunkBuilder = new ChunkBuilder(worldSystem);

        // Queue System
        this.loadRequests = new ConcurrentLinkedQueue<>();
        this.generationRequests = new ConcurrentLinkedQueue<>();
        this.buildRequests = new ConcurrentLinkedQueue<>();

        this.loadedResults = new ConcurrentLinkedQueue<>();
        this.builtResults = new ConcurrentLinkedQueue<>();

        // Queue System
        this.queueProcess = new QueueProcess[] {
                QueueProcess.Load,
                QueueProcess.Generate,
                QueueProcess.Build
        };
        this.queueBatch = 0;
        this.processPerBatch = 32;
        this.loadedChunksThisFrame = 0;
    }

    public void awake() {
        this.worldGenerator = worldSystem.worldGenerator;
    }

    // Async System \\

    public void requestLoad(long chunkCoordinate) {
        loadRequests.add(chunkCoordinate);
    }

    public void requestGenerate(Chunk chunk) {
        generationRequests.add(chunk);
    }

    public void requestBuild(Chunk chunk) {
        buildRequests.add(chunk);
    }

    public Chunk pollLoadedChunk() {
        return loadedResults.poll();
    }

    public Chunk pollBuiltChunk() {
        return builtResults.poll();
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

    enum QueueProcess {

        Load {
            boolean process(ChunkSystem system) {
                return system.processLoadData();
            }
        },

        Generate {
            boolean process(ChunkSystem system) {
                return system.processGenerationData();
            }
        },

        Build {
            boolean process(ChunkSystem system) {
                return system.processBuildData();
            }
        };

        abstract boolean process(ChunkSystem system);
    }

    // Load \\

    private boolean processLoadData() {

        int index = 0;

        while (!loadRequests.isEmpty() && processIsSafe(index)) {

            Long chunkCoordinate = loadRequests.poll();

            // Run load in another thread
            threadManager.submitGeneral(() -> {
                Chunk chunk = chunkData.readChunk(chunkCoordinate);

                if (chunk == null)
                    chunk = new Chunk(worldSystem, chunkCoordinate);

                // Thread-safe add to results
                loadedResults.add(chunk);
            });

            // Increment counters on main thread
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Generate \\

    private boolean processGenerationData() {

        int index = 0;

        while (!generationRequests.isEmpty() && processIsSafe(index)) {

            Chunk loadedChunk = generationRequests.poll();

            // Increment counters on main thread
            index = incrementQueueTotal(index);

            if (loadedChunk == null)
                continue;

            // Run load in another thread
            threadManager.submitGeneration(() -> {

                worldGenerator.generateChunk(loadedChunk);
            });
        }

        return totalProcessThisFrame();
    }

    // Build \\

    private boolean processBuildData() {

        int index = 0;

        while (!buildRequests.isEmpty() && processIsSafe(index)) {

            Chunk loadedChunk = buildRequests.poll();

            // Run load in another thread
            threadManager.submitGeneral(() -> {

                loadedChunk.build();
                builtResults.add(loadedChunk);
            });

            // Increment counters on main thread
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Queue Utility \\

    private boolean processIsSafe(int index) {
        return index < processPerBatch &&
                loadedChunksThisFrame < MAX_CHUNK_LOADS_PER_FRAME;
    }

    private boolean totalProcessThisFrame() {
        return loadedChunksThisFrame >= MAX_CHUNK_LOADS_PER_FRAME;
    }

    private int incrementQueueTotal(int index) {

        loadedChunksThisFrame++;

        return ++index;
    }

    public boolean hasQueueData() {
        return loadRequests.size() > 0 ||
                generationRequests.size() > 0 ||
                buildRequests.size() > 0;
    }

    public boolean hasReturnData() {
        return loadedResults.size() > 0;
    }
}
