package com.AdventureRPG.WorldSystem.QueueSystem;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.QueueSystem.BatchSystem.BatchSystem;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class QueueSystem {

    // Debug
    private final boolean debug = false; // TODO: Debug line

    // Game Manager
    public final Settings settings;
    private final WorldSystem worldSystem;
    private final WorldTick worldTick;
    private final Grid grid;
    private final Loader loader;
    private final BatchSystem batchSystem;

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

    private int loadedChunksThisFrame;
    private int loadedChunksThisTick;

    // Base \\

    public QueueSystem(WorldSystem worldSystem) {

        // Game Manager
        this.settings = worldSystem.settings;
        this.worldSystem = worldSystem;
        this.worldTick = worldSystem.worldTick;
        this.grid = new Grid(this);
        grid.buildGrid();
        this.loader = new Loader(worldSystem);
        this.batchSystem = new BatchSystem(worldSystem, this);

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
        QueueProcess.Load.bundle = new GridCoordinate(grid.totalChunks());
        QueueProcess.Generate.bundle = new ChunkCoordinate(grid.totalChunks());
        QueueProcess.Assessment.bundle = new ChunkCoordinate(grid.totalChunks());
        QueueProcess.Build.bundle = new ChunkCoordinate(grid.totalChunks());
        QueueProcess.Batch.bundle = new ChunkCoordinate(grid.totalChunks());

        this.loadedChunksThisFrame = 0;
        this.loadedChunksThisTick = 0;
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

        return queueSize + process.priority * 100;
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
        for (QueueProcess queue : QueueProcess.values())
            if (queue.bundle != null)
                queue.bundle.clear();

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
            if (loadedChunk != null)
                loadedChunk.moveTo(gridCoordinate);

            else // If the chunkCoordinate could not be found add it to the queue
                queue(QueueProcess.Load).enqueue(gridCoordinate);
        }
    }

    public void addChunkToQueue(Chunk chunk) {

        loadedChunks.put(chunk.coordinate, chunk);

        unloadQueue.remove(chunk.coordinate);

        long gridCoordinate = chunkToGridMap.getOrDefault(chunk.coordinate, -1);

        if (gridCoordinate != -1)
            chunk.moveTo(gridCoordinate);

        chunk.enqueue();
    }

    // Queue \\

    public boolean processQueue(QueueProcess queue) {

        int index = 0;
        int size = queue.bundle.size();

        while (index < size && processIsSafe()) {

            long key = queue.bundle.dequeue();

            dispatchQueueWork(queue, key);

            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    private void dispatchQueueWork(QueueProcess queue, long chunkCoordinate) {

        if (queue == QueueProcess.Load) {

            loader.requestLoad(chunkCoordinate);
            return;
        }

        Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

        if (loadedChunk == null) {

            loadedChunks.remove(chunkCoordinate);
            return;
        }

        switch (queue) {

            case Generate ->
                loader.requestGenerate(loadedChunk);

            case Assessment ->
                loader.requestAssessment(loadedChunk);

            case Build ->
                loader.requestBuild(loadedChunk);

            case Batch ->
                loader.requestBatch(loadedChunk);

            default -> {
                // Unload is handled separate
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

    private int incrementQueueTotal(int index) {

        loadedChunksThisFrame++;
        loadedChunksThisTick++;

        return ++index;
    }

    // External Queueing \\

    public void addToGenerateQueue(long chunkCoordinate) {
        queue(QueueProcess.Generate).enqueue(chunkCoordinate);
    }

    public void addToAssessmentQueue(long chunkCoordinate) {
        queue(QueueProcess.Assessment).enqueue(chunkCoordinate);
    }

    public void addToBuildQueue(long chunkCoordinate) {
        queue(QueueProcess.Build).enqueue(chunkCoordinate);
    }

    public void addToBatchQueue(long chunkCoordinate) {
        queue(QueueProcess.Batch).enqueue(chunkCoordinate);
    }

    // Unload \\

    public boolean unloadQueue() {

        int index = 0;
        var iterator = unloadQueue.long2ObjectEntrySet().fastIterator();

        while (iterator.hasNext() && processIsSafe()) {

            var entry = iterator.next();
            long chunkCoordinate = entry.getLongKey();
            Chunk loadedChunk = entry.getValue();

            if (loadedChunk == null)
                continue;

            // Remove from unloadQueue
            iterator.remove();
            loadedChunks.remove(chunkCoordinate);
            loadedChunk.dispose();

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Accessible \\

    private QueueBundle queue(QueueProcess queue) {
        return (QueueBundle) queue.bundle;
    }

    public Chunk getChunkFromCoordinate(long chunkCoordinate) {
        return loadedChunks.get(chunkCoordinate);
    }

    public boolean hasQueue() {

        // Check primitive bundle-backed queues first
        for (QueueProcess queue : QueueProcess.values())
            if (queue.bundle != null && queue.bundle.size() > 0)
                return true;

        // Then check unload map
        return unloadQueue.size() > 0;
    }

    public int totalQueueSize() {

        int total = unloadQueue.size();

        for (QueueProcess queue : QueueProcess.values())
            if (queue.bundle != null)
                total += queue.bundle.size();

        return total;
    }

    public int getQueueSize(QueueProcess process) {

        return switch (process) {

            case Unload ->
                unloadQueue.size();

            case Load ->
                QueueProcess.Load.bundle.size();

            case Generate ->
                QueueProcess.Generate.bundle.size();

            case Assessment ->
                QueueProcess.Assessment.bundle.size();

            case Build ->
                QueueProcess.Build.bundle.size();

            case Batch ->
                QueueProcess.Batch.bundle.size();
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

    // Grid Access \\

    public int totalChunks() {
        return grid.totalChunks();
    }

    // Batch System \\

    public void requestBatch(Chunk chunk) {
        batchSystem.requestBatch(chunk);
    }

    // Debug \\

    private void debug(String input) {
        System.out.println("[GridSystem] " + input);
    }
}
