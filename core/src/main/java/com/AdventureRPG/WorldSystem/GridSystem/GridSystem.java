package com.AdventureRPG.WorldSystem.GridSystem;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.AdventureRPG.WorldSystem.BatchSystem.BatchSystem;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class GridSystem {

    // Debug
    private final boolean debug = false; // TODO: Debug line

    // Game Manager
    public final Settings settings;
    private final WorldSystem worldSystem;
    private final WorldTick worldTick;
    private final BatchSystem batchSystem;
    private final Loader loader;

    // Grid
    private final Grid grid;

    // Settings
    private final int MAX_CHUNK_LOADS_PER_FRAME;
    private final int MAX_CHUNK_LOADS_PER_TICK;

    // Position Tracking
    private long currentChunk;
    private int currentChunkX, currentChunkY;

    // Chunk Tracking
    private final float loadFactor;
    private LongOpenHashSet chunkCoordinates;
    private Long2LongOpenHashMap gridToChunkMap;
    private Long2LongOpenHashMap chunkToGridMap;
    private Long2ObjectOpenHashMap<Chunk> loadedChunks;

    // Queue System
    private Long2ObjectOpenHashMap<Chunk> unloadQueue;
    private LongArrayFIFOQueue loadQueue;
    private LongArrayFIFOQueue generateQueue;
    private LongOpenHashSet generateQueueCheck;
    private LongArrayFIFOQueue assessmentQueue;
    private LongOpenHashSet assessmentQueueCheck;
    private LongArrayFIFOQueue buildQueue;
    private LongOpenHashSet buildQueueCheck;

    private int loadedChunksThisFrame;
    private int loadedChunksThisTick;

    private final long nullMapping;

    // Base \\

    public GridSystem(WorldSystem worldSystem) {

        // Chunk System
        this.settings = worldSystem.settings;
        this.worldSystem = worldSystem;
        this.worldTick = worldSystem.worldTick;
        this.batchSystem = worldSystem.batchSystem;
        this.loader = new Loader(worldSystem);

        // Grid
        this.grid = new Grid(this);
        grid.buildGrid();

        // Settings
        this.MAX_CHUNK_LOADS_PER_FRAME = GlobalConstant.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = GlobalConstant.MAX_CHUNK_LOADS_PER_TICK;

        // Position Tracking
        this.currentChunk = Coordinate2Int.pack(-1, -1);
        this.currentChunkX = -1;
        this.currentChunkY = -1;

        // Chunk Tracking
        this.loadFactor = 0.75f;
        int initialCapacity = (int) Math.ceil(grid.totalChunks() / loadFactor);
        this.chunkCoordinates = new LongOpenHashSet(grid.totalChunks());
        this.gridToChunkMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.chunkToGridMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.loadedChunks = new Long2ObjectOpenHashMap<>(grid.totalChunks());

        // Queue System
        this.unloadQueue = new Long2ObjectOpenHashMap<>(grid.totalChunks());
        this.loadQueue = new LongArrayFIFOQueue(grid.totalChunks());
        this.generateQueue = new LongArrayFIFOQueue(grid.totalChunks());
        this.generateQueueCheck = new LongOpenHashSet(grid.totalChunks());
        this.assessmentQueue = new LongArrayFIFOQueue(grid.totalChunks());
        this.assessmentQueueCheck = new LongOpenHashSet(grid.totalChunks());
        this.buildQueue = new LongArrayFIFOQueue(grid.totalChunks());
        this.buildQueueCheck = new LongOpenHashSet(grid.totalChunks());

        this.loadedChunksThisFrame = 0;
        this.loadedChunksThisTick = 0;

        this.nullMapping = Long.MAX_VALUE;
    }

    public void awake() {

        loader.awake();
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

        for (int i = 0; i < grid.totalChunks(); i++) {

            long gridCoordinate = grid.loadOrder(i);

            long chunkCoordinate = gridToChunkMap.get(gridCoordinate);
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk != null)
                loadedChunk.dispose();
        }
    }

    // Update \\

    private void updateQueue() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        // Reset every tick
        if (worldTick.tick())
            loadedChunksThisTick = 0;

        ReceiveData();

        while (!totalProcessThisFrame() && hasQueue()) {

            QueueProcess next = pickNextQueue();

            if (next == null)
                break;

            while (!totalProcessThisFrame() && getQueueSize(next) > 0)
                if (!next.process(this))
                    break;
        }
    }

    private QueueProcess pickNextQueue() {

        QueueProcess priorityQueue = null;
        int priorityScore = 0;

        for (QueueProcess queue : QueueProcess.values()) {

            int queueScore = getQueueScore(queue);

            if (queueScore > priorityScore) {
                priorityQueue = queue;
                priorityScore = queueScore;
            }
        }

        return priorityQueue;
    }

    private int getQueueScore(QueueProcess process) {

        int queueSize = getQueueSize(process);

        if (queueSize == 0)
            return 0;

        return queueSize + process.getPriority() * 100;
    }

    // Main \\

    public void updateChunksInGrid(Vector2Int chunkCoordinate) {

        // Pack the coordinate
        int chunkCoordinateX = chunkCoordinate.x;
        int chunkCoordinateY = chunkCoordinate.y;
        long packedCoordinate = Coordinate2Int.pack(chunkCoordinateX, chunkCoordinateY);

        // Triple check if the chunk is already active
        if (packedCoordinate == currentChunk)
            return;

        // Set the current chunk first
        currentChunk = packedCoordinate;
        currentChunkX = chunkCoordinateX;
        currentChunkY = chunkCoordinateY;

        // Clear all collections
        clearQueue();

        // Determine which chunks need to be loaded where
        createQueue();
    }

    private void clearQueue() {

        // Clear mappings
        gridToChunkMap.clear();
        chunkToGridMap.clear();

        // Clear unload map
        unloadQueue.clear();

        // Clear all primitive queues
        loadQueue.clear();
        generateQueue.clear();
        generateQueueCheck.clear();
        assessmentQueue.clear();
        assessmentQueueCheck.clear();
        buildQueue.clear();
        buildQueueCheck.clear();

        // Reversely remove all computed Coordinate2Ints from unloadQueue for efficiency
        unloadQueue.putAll(loadedChunks);
    }

    private void createQueue() {

        // The grid is prebuilt and never changes
        for (int i = 0; i < grid.totalChunks(); i++) {

            long gridCoordinate = grid.loadOrder(i);

            // Use the position in grid as an offset
            int offsetX = Coordinate2Int.unpackX(gridCoordinate);
            int offsetY = Coordinate2Int.unpackY(gridCoordinate);

            // Use the current chunks coordinates to compute necessary chunks
            int chunkX = currentChunkX + offsetX;
            int chunkY = currentChunkY + offsetY;

            // Pack the coordinates into a usable long value and wrap it
            long chunkCoordinate = Coordinate2Int.pack(chunkX, chunkY);
            chunkCoordinate = worldSystem.wrapAroundWorld(chunkCoordinate);

            // The very first step is to remap all the chunks to the correct position
            gridToChunkMap.put(gridCoordinate, chunkCoordinate);
            chunkToGridMap.put(chunkCoordinate, gridCoordinate);

            // Log all active chunk coordinates
            chunkCoordinates.add(chunkCoordinate);

            // Remove the computed coordinate from the unload queue
            unloadQueue.remove(chunkCoordinate);

            // Attempt to use the chunk coordinate to retrieve a loaded chunk
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            // If the chunk was loaded we move it to the new grid position
            if (loadedChunk != null) {

                loadedChunk.moveTo(gridCoordinate);
                loadedChunk.enqueue();

                batchSystem.assessChunk(loadedChunk);
            }

            else // If the chunkCoordinate could not be found add it to the queue
                loadQueue.enqueue(gridCoordinate);
        }
    }

    // Unload \\

    private boolean unloadQueue() {

        int index = 0;
        var iterator = unloadQueue.long2ObjectEntrySet().fastIterator();

        while (iterator.hasNext() && processIsSafe(index)) {

            var entry = iterator.next();
            long chunkCoordinate = entry.getLongKey();
            Chunk loadedChunk = entry.getValue();

            if (loadedChunk == null)
                continue;

            // Remove from unloadQueue
            iterator.remove();
            batchSystem.removeChunk(loadedChunk);
            loadedChunks.remove(chunkCoordinate);
            loadedChunk.dispose();

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Load \\

    private boolean loadQueue() {

        // Return early if there is no room for loading new chunks
        if (loadedChunks.size() == grid.totalChunks())
            return totalProcessThisFrame();

        int index = 0;

        // Capture queue size once to avoid changing bound while dequeuing
        int qSize = loadQueue.size();

        while (index < qSize && processIsSafe(index)) {

            long gridCoordinate = loadQueue.dequeueLong();
            long chunkCoordinate = gridToChunkMap.get(gridCoordinate);

            loader.requestLoad(chunkCoordinate);

            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Generate \\

    private boolean generateQueue() {

        int index = 0;

        int qSize = generateQueue.size();

        while (index < qSize && processIsSafe(index)) {

            long chunkCoordinate = dequeueGenerateQueue();
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk == null) {

                loadedChunks.remove(chunkCoordinate);
                index = incrementQueueTotal(index);

                continue;
            }

            loader.requestGenerate(loadedChunk);

            // Increment counters
            index = incrementQueueTotal(index);

        }

        return totalProcessThisFrame();
    }

    // Assessment \\

    private boolean assessmentQueue() {

        int index = 0;

        int qSize = assessmentQueue.size();

        while (index < qSize && processIsSafe(index)) {

            long chunkCoordinate = dequeueAssessmentQueue();
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk == null) {

                loadedChunks.remove(chunkCoordinate);
                index = incrementQueueTotal(index);

                continue;
            }

            loadedChunk.assessNeighbors();

            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Build \\

    private boolean buildQueue() {

        int index = 0;

        int qSize = buildQueue.size();

        while (index < qSize && processIsSafe(index)) {

            long chunkCoordinate = dequeueBuildQueue();
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk == null) {

                loadedChunks.remove(chunkCoordinate);
                index = incrementQueueTotal(index);

                continue;
            }

            if (loadedChunk.getNeighborStatus() != Chunk.NeighborStatus.INCOMPLETE)
                loader.requestBuild(loadedChunk);

            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Thread Queue \\

    private void ReceiveData() {

        receiveLoadedChunks();
        receiveGeneratedChunks();
        receiveBuiltChunks();
    }

    private void receiveLoadedChunks() {

        while (loader.hasLoadedData()) {

            Chunk loadedChunk = loader.pollLoadedChunk();

            long chunkCoordinate = loadedChunk.coordinate;
            long gridCoordinate = chunkToGridMap.getOrDefault(chunkCoordinate, nullMapping);

            if (gridCoordinate != nullMapping) {

                loadedChunk.moveTo(gridCoordinate);
                loadedChunks.put(chunkCoordinate, loadedChunk);
                loadedChunk.enqueue();
            }

            else
                loadedChunk.dispose();
        }
    }

    private void receiveGeneratedChunks() {

        while (loader.hasGeneratedData()) {

            Chunk loadedChunk = loader.pollGeneratedChunk();

            long chunkCoordinate = loadedChunk.coordinate;
            long gridCoordinate = chunkToGridMap.getOrDefault(chunkCoordinate, nullMapping);

            if (gridCoordinate != nullMapping)
                loadedChunk.enqueue();

            else
                loadedChunk.dispose();
        }
    }

    private void receiveBuiltChunks() {

        while (loader.hasBuiltData()) {

            Chunk loadedChunk = loader.pollBuiltChunk();

            long chunkCoordinate = loadedChunk.coordinate;
            long gridCoordinate = chunkToGridMap.getOrDefault(chunkCoordinate, nullMapping);

            if (gridCoordinate != nullMapping)
                batchSystem.addChunk(loadedChunk);

            else // TODO: May want to add a small pool to hold older chunks for easy re-access
                loadedChunk.dispose();
        }
    }

    // Queue Utility \\

    private boolean processIsSafe(int index) {
        return loadedChunksThisFrame < MAX_CHUNK_LOADS_PER_FRAME &&
                loadedChunksThisTick < MAX_CHUNK_LOADS_PER_TICK;
    }

    private boolean totalProcessThisFrame() {
        return loadedChunksThisFrame >= MAX_CHUNK_LOADS_PER_FRAME ||
                loadedChunksThisTick >= MAX_CHUNK_LOADS_PER_TICK;
    }

    private int incrementQueueTotal(int index) {

        loadedChunksThisFrame++;
        loadedChunksThisTick++;

        return ++index;
    }

    enum QueueProcess {

        Unload(5) {
            boolean process(GridSystem system) {
                return system.unloadQueue();
            }
        },

        Load(4) {
            boolean process(GridSystem system) {
                return system.loadQueue();
            }
        },

        Generate(3) {
            boolean process(GridSystem system) {
                return system.generateQueue();
            }
        },

        Assessment(2) {
            boolean process(GridSystem system) {
                return system.assessmentQueue();
            }
        },

        Build(1) {
            boolean process(GridSystem system) {
                return system.buildQueue();
            }
        };

        private final int priority;

        abstract boolean process(GridSystem system);

        QueueProcess(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    private long dequeueGenerateQueue() {

        long chunkCoordinate = generateQueue.dequeueLong();
        generateQueueCheck.remove(chunkCoordinate);

        return chunkCoordinate;
    }

    private long dequeueAssessmentQueue() {

        long chunkCoordinate = assessmentQueue.dequeueLong();
        assessmentQueueCheck.remove(chunkCoordinate);

        return chunkCoordinate;
    }

    private long dequeueBuildQueue() {

        long chunkCoordinate = buildQueue.dequeueLong();
        buildQueueCheck.remove(chunkCoordinate);

        return chunkCoordinate;
    }

    // External Queueing \\

    public void addToGenerateQueue(long chunkCoordinate) {

        if (generateQueueCheck.add(chunkCoordinate))
            generateQueue.enqueue(chunkCoordinate);
    }

    public void addToAssessmentQueue(long chunkCoordinate) {

        if (assessmentQueueCheck.add(chunkCoordinate))
            assessmentQueue.enqueue(chunkCoordinate);
    }

    public void addToBuildQueue(long chunkCoordinate) {

        if (buildQueueCheck.add(chunkCoordinate))
            buildQueue.enqueue(chunkCoordinate);
    }

    // Accessible \\

    public Chunk getChunkFromCoordinate(long chunkCoordinate) {
        return loadedChunks.get(chunkCoordinate);
    }

    public boolean hasQueue() {
        return unloadQueue.size() > 0 ||
                loadQueue.size() > 0 ||
                assessmentQueue.size() > 0 ||
                generateQueue.size() > 0 ||
                buildQueue.size() > 0;
    }

    public int totalQueueSize() {
        return unloadQueue.size() +
                loadQueue.size() +
                assessmentQueue.size() +
                generateQueue.size() +
                buildQueue.size();
    }

    public int getQueueSize(QueueProcess process) {
        return switch (process) {
            case Unload -> unloadQueue.size();
            case Load -> loadQueue.size();
            case Generate -> generateQueue.size();
            case Assessment -> assessmentQueue.size();
            case Build -> buildQueue.size();
        };
    }

    // Intended to be called after changing the render distance
    public void rebuildGrid() {

        // Rebuild grid geometry
        grid.buildGrid();

        // Clear all mappings and queues
        clearQueue();

        // Recreate queues based on the current player chunk
        createQueue();

        // Finally act as if the player crossed a chunk boundary
        updateChunksInGrid(worldSystem.chunk());
    }

    // Debug \\

    private void debug() { // TODO: Debug line

    }
}
