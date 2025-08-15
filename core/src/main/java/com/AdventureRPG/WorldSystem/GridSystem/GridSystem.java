package com.AdventureRPG.WorldSystem.GridSystem;

import java.util.Comparator;
import java.util.List;

import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class GridSystem {

    // Debug
    private final boolean debug = true; // TODO: Remove debug line

    // Game Manager
    private final Settings settings;
    private final ChunkData chunkData;
    private final WorldSystem worldSystem;
    private final ChunkSystem chunkSystem;
    private final WorldTick worldTick;

    // Settings
    private int maxRenderDistance;
    private final int MAX_CHUNK_LOADS_PER_FRAME;
    private final int MAX_CHUNK_LOADS_PER_TICK;

    // Position Tracking
    private long currentChunk;
    private int currentChunkX, currentChunkY;

    // Chunk Tracking
    private long[] loadOrder;
    private final float loadFactor;
    private int totalChunks;
    private LongOpenHashSet gridCoordinates;
    private LongOpenHashSet chunkCoordinates;
    private Long2LongOpenHashMap gridToChunkMap;
    private Long2LongOpenHashMap chunkToGridMap;
    private Long2ObjectOpenHashMap<Chunk> loadedChunks;

    // Queue System
    private Long2ObjectOpenHashMap<Chunk> unloadQueue;
    private LongArrayFIFOQueue loadQueue;
    private LongArrayFIFOQueue generateQueue;
    private LongArrayFIFOQueue buildQueue;

    private final QueueProcess[] queueProcess;
    private int queueBatch;
    private final int processPerBatch;
    private int loadedChunksThisFrame;
    private int loadedChunksThisTick;

    // Model Instance
    private Long2ObjectOpenHashMap<ModelInstance> modelInstances;

    // Utility
    private final Chunk[] neighborChunks;
    private final Chunk[] NEIGHBOR_OUT_OF_GRID;

    // Base \\

    public GridSystem(WorldSystem WorldSystem) {

        // Chunk System
        this.settings = WorldSystem.settings;
        this.chunkData = WorldSystem.saveSystem.chunkData;
        this.worldSystem = WorldSystem;
        this.chunkSystem = worldSystem.chunkSystem;
        this.worldTick = WorldSystem.worldTick;

        // Settings
        this.maxRenderDistance = settings.maxRenderDistance;
        this.MAX_CHUNK_LOADS_PER_FRAME = settings.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = settings.MAX_CHUNK_LOADS_PER_TICK;

        // Position Tracking
        this.currentChunk = Coordinate2Int.pack(-1, -1);
        this.currentChunkX = -1;
        this.currentChunkY = -1;

        // Chunk Tracking
        this.loadFactor = 0.75f;
        this.totalChunks = maxRenderDistance * maxRenderDistance;
        int initialCapacity = (int) Math.ceil(totalChunks / loadFactor);
        this.loadOrder = new long[totalChunks];
        this.gridCoordinates = new LongOpenHashSet(totalChunks);
        this.chunkCoordinates = new LongOpenHashSet(totalChunks);
        this.gridToChunkMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.chunkToGridMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.loadedChunks = new Long2ObjectOpenHashMap<>(totalChunks);

        // Queue System
        this.unloadQueue = new Long2ObjectOpenHashMap<>(totalChunks);
        this.loadQueue = new LongArrayFIFOQueue(totalChunks);
        this.generateQueue = new LongArrayFIFOQueue(totalChunks);
        this.buildQueue = new LongArrayFIFOQueue(totalChunks);

        this.queueProcess = new QueueProcess[] {
                QueueProcess.Unload,
                QueueProcess.Load,
                QueueProcess.Generate,
                QueueProcess.Build
        };
        this.queueBatch = 0;
        this.processPerBatch = 32;
        this.loadedChunksThisFrame = 0;
        this.loadedChunksThisTick = 0;

        // Model Instance
        this.neighborChunks = new Chunk[4];
        this.modelInstances = new Long2ObjectOpenHashMap<>(totalChunks);

        // Utility
        this.NEIGHBOR_OUT_OF_GRID = new Chunk[0];
    }

    public void awake() {
        rebuildGrid();
    }

    public void start() {

    }

    public void update() {
        updateQueue();
    }

    public void render(ModelBatch modelBatch) {
        renderChunks(modelBatch);
    }

    // Awake \\

    public void rebuildGrid() {

        // Start with a simple square grid
        this.maxRenderDistance = settings.maxRenderDistance;

        // The actual circle radius
        float radius = maxRenderDistance / 2f;
        float radiusSquared = radius * radius;

        // Temporary list to hold coordinates and squared distance
        class ChunkDistance {

            long coord;
            float distSquared; // no sqrt needed for sorting

            ChunkDistance(long c, float d2) {
                coord = c;
                distSquared = d2;
            }
        }

        List<ChunkDistance> tempList = new java.util.ArrayList<>();

        // Collect all coordinates inside the circle
        for (int x = -(maxRenderDistance / 2); x < maxRenderDistance / 2; x++) {

            for (int y = -(maxRenderDistance / 2); y < maxRenderDistance / 2; y++) {

                float distSquared = (x * x) + (y * y);

                if (distSquared <= radiusSquared) {

                    long gridCoord = Coordinate2Int.pack(x, y);
                    tempList.add(new ChunkDistance(gridCoord, distSquared));
                }
            }
        }

        // Chunk Tracking
        this.totalChunks = tempList.size();
        int initialCapacity = (int) Math.ceil(totalChunks / loadFactor);
        this.loadOrder = new long[totalChunks];
        this.gridCoordinates = new LongOpenHashSet(totalChunks);
        this.chunkCoordinates = new LongOpenHashSet(totalChunks);
        this.gridToChunkMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.chunkToGridMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);

        // Queue System
        this.unloadQueue = new Long2ObjectOpenHashMap<>(totalChunks);
        this.loadQueue = new LongArrayFIFOQueue(totalChunks);

        // Fill gridCoordinates
        for (ChunkDistance cd : tempList)
            gridCoordinates.add(cd.coord);

        // Sort by distance
        tempList.sort(Comparator.comparingDouble(cd -> cd.distSquared));

        // Assign load order
        for (int i = 0; i < totalChunks; i++)
            loadOrder[i] = tempList.get(i).coord;
    }

    // Update \\

    private void updateQueue() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        // Reset every tick
        if (worldTick.tick())
            loadedChunksThisTick = 0;

        ReceiveData();

        // Alternating queue update
        if (hasQueue())
            for (int i = 0; i < queueProcess.length; i++) {

                if (queueProcess[queueBatch].process(this))
                    return;

                queueBatch = (queueBatch + 1) % queueProcess.length;
            }

        // Return data from other threads
        chunkSystem.processData();
    }

    enum QueueProcess {

        Unload {
            boolean process(GridSystem system) {
                return system.unloadQueue();
            }
        },

        Load {
            boolean process(GridSystem system) {
                return system.loadQueue();
            }
        },

        Generate {
            boolean process(GridSystem system) {
                return system.generateQueue();
            }
        },

        Build {
            boolean process(GridSystem system) {
                return system.buildQueue();
            }
        };

        abstract boolean process(GridSystem system);
    }

    // Render \\

    private void renderChunks(ModelBatch modelBatch) {

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

        // Clear the queue
        unloadQueue.clear();

        gridToChunkMap.clear();
        chunkToGridMap.clear();
        loadQueue.clear();

        // Reversely remove all computed Coordinate2Ints from unloadQueue for efficiency
        unloadQueue.putAll(loadedChunks);
    }

    private void createQueue() {

        // The grid is prebuilt and never changes
        for (long gridCoordinate : gridCoordinates) {

            // Use the position in grid as an offset
            int offsetX = Coordinate2Int.unpackX(gridCoordinate);
            int offsetY = Coordinate2Int.unpackY(gridCoordinate);

            // Use the current chunks coordinates to compute necessary chunks
            int chunkX = currentChunkX + offsetX;
            int chunkY = currentChunkY + offsetY;

            // Pack the coordinates into a usable long value and wrap it
            long chunkCoordinate = Coordinate2Int.pack(chunkX, chunkY);
            chunkCoordinate = worldSystem.wrapAroundWorld(chunkCoordinate);

            // Log all active chunk coordinates
            chunkCoordinates.add(chunkCoordinate);

            // Remove the computed coordinate from the unload queue
            unloadQueue.remove(chunkCoordinate);

            // Attempt to use the chunk coordinate to retrieve a loaded chunk
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            // If the chunk was loaded we move it to the new grid position
            if (loadedChunk != null)
                loadedChunk.moveTo(gridCoordinate);

            else { // If the chunkCoordinate could not be found add it to the queue
                gridToChunkMap.put(gridCoordinate, chunkCoordinate);
                chunkToGridMap.put(chunkCoordinate, gridCoordinate);
                loadQueue.enqueue(gridCoordinate);
            }
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

            modelInstances.remove(chunkCoordinate);

            // Remove from unloadQueue
            iterator.remove();
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
        if (loadedChunks.size() == totalChunks)
            return totalProcessThisFrame();

        int index = 0;

        while (index < loadQueue.size() && processIsSafe(index)) {

            long gridCoordinate = loadQueue.dequeueLong();
            long chunkCoordinate = gridToChunkMap.get(gridCoordinate);

            chunkSystem.requestLoad(chunkCoordinate);

            // Increment counters
            index = incrementQueueTotal(index);

        }

        return totalProcessThisFrame();
    }

    // Generate \\

    private boolean generateQueue() {

        int index = 0;

        while (index < generateQueue.size() && processIsSafe(index)) {

            long chunkCoordinate = generateQueue.dequeueLong();
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk == null) {

                loadedChunks.remove(chunkCoordinate);
                index = incrementQueueTotal(index);

                continue;
            }

            chunkSystem.requestGenerate(loadedChunk);
            buildQueue.enqueue(chunkCoordinate);

            // Increment counters
            index = incrementQueueTotal(index);

        }

        return totalProcessThisFrame();
    }

    // Build \\

    private boolean buildQueue() {

        int index = 0;

        while (index < buildQueue.size() && processIsSafe(index)) {

            long chunkCoordinate = buildQueue.dequeueLong();
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk == null) {

                loadedChunks.remove(chunkCoordinate);
                return totalProcessThisFrame();
            }

            Chunk[] neighbors = getNeighborsIfLoaded(loadedChunk);

            if (neighbors == NEIGHBOR_OUT_OF_GRID) {
                index = incrementQueueTotal(index);
                continue;
            }

            if (neighbors == null) {

                buildQueue.enqueue(chunkCoordinate);

                index = incrementQueueTotal(index);
                continue;
            }

            boolean hasModel = loadedChunk.tryBuild(neighbors);

            if (hasModel)
                modelInstances.put(chunkCoordinate, loadedChunk.modelInstance);

            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    public Chunk[] getNeighborsIfLoaded(Chunk loadedChunk) {

        long north = loadedChunk.north;
        long south = loadedChunk.south;
        long east = loadedChunk.east;
        long west = loadedChunk.west;

        neighborChunks[0] = loadedChunks.get(north);
        neighborChunks[1] = loadedChunks.get(south);
        neighborChunks[2] = loadedChunks.get(east);
        neighborChunks[3] = loadedChunks.get(west);

        if (neighborChunks[0] == null || neighborChunks[1] == null || neighborChunks[2] == null
                || neighborChunks[3] == null) {

            // If missing neighbor and outside grid → never going to load
            if (neighborChunks[0] == null && !chunkCoordinates.contains(north))
                return NEIGHBOR_OUT_OF_GRID;
            if (neighborChunks[1] == null && !chunkCoordinates.contains(south))
                return NEIGHBOR_OUT_OF_GRID;
            if (neighborChunks[2] == null && !chunkCoordinates.contains(east))
                return NEIGHBOR_OUT_OF_GRID;
            if (neighborChunks[3] == null && !chunkCoordinates.contains(west))
                return NEIGHBOR_OUT_OF_GRID;

            return null; // Not all neighbors are loaded
        }

        return neighborChunks;
    }

    // Thread Queue \\

    private void ReceiveData() {

        ReceiveLoadedChunks();
    }

    private void ReceiveLoadedChunks() {

        int index = 0;

        while (index < processPerBatch && chunkSystem.hasReturnData()) {

            Chunk loadedChunk = chunkSystem.pollLoadedChunk();

            long chunkCoordinate = loadedChunk.coordinate;
            long gridCoordinate = chunkToGridMap.get(chunkCoordinate);

            if (loadedChunk.hasData())
                buildQueue.enqueue(chunkCoordinate);
            else
                generateQueue.enqueue(chunkCoordinate);

            loadedChunk.moveTo(gridCoordinate);
            loadedChunks.put(chunkCoordinate, loadedChunk);

            index++;
        }
    }

    // Queue Utility \\

    private boolean processIsSafe(int index) {
        return index < processPerBatch &&
                loadedChunksThisFrame < MAX_CHUNK_LOADS_PER_FRAME &&
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

    public boolean hasQueue() {
        return unloadQueue.size() > 0 ||
                loadQueue.size() > 0 ||
                generateQueue.size() > 0 ||
                buildQueue.size() > 0;
    }

    public int totalQueueSize() {
        return unloadQueue.size() +
                loadQueue.size() +
                generateQueue.size() +
                buildQueue.size();
    }

    // Debug \\

    private void debug() { // TODO: Remove debug line

    }
}
