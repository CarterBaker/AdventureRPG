package com.AdventureRPG.WorldSystem.Chunks;

import java.util.Arrays;
import java.util.Comparator;

import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class ChunkSystem {

    // Debug
    private final boolean debug = false; // TODO: Remove debug line

    // Chunk System
    private final Settings settings;
    private final ChunkData chunkData;
    private final WorldSystem worldSystem;
    private final WorldTick worldTick;

    // Settings
    private int maxRenderDistance;
    private final int MAX_CHUNK_LOADS_PER_FRAME;
    private final int MAX_CHUNK_LOADS_PER_TICK;

    // Position Tracking
    private long currentChunk;
    private int currentChunkX, currentChunkY;

    // Chunk Tracking
    private int totalChunks;
    private LongOpenHashSet gridCoordinates;
    private Long2ObjectOpenHashMap<Chunk> loadedChunks;

    // Chunk Queue
    private final QueueProcess[] queueProcess;
    private long[] loadOrder;
    private int queueBatch = 0;
    private int processPerBatch = 32;
    private Long2ObjectOpenHashMap<Chunk> unloadQueue;
    private final float loadFactor;
    private Long2LongOpenHashMap loadQueue;
    private LongArrayFIFOQueue loadQueueOrder;

    // Queue System
    private int loadedChunksThisFrame;
    private int loadedChunksThisTick;

    // Base \\

    public ChunkSystem(WorldSystem WorldSystem) {

        // Chunk System
        this.settings = WorldSystem.settings;
        this.chunkData = WorldSystem.saveSystem.chunkData;
        this.worldSystem = WorldSystem;
        this.worldTick = WorldSystem.worldTick;

        // Settings
        this.maxRenderDistance = settings.maxRenderDistance;
        this.MAX_CHUNK_LOADS_PER_FRAME = settings.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = settings.MAX_CHUNK_LOADS_PER_TICK;

        // Position Tracking
        this.currentChunk = 0;
        this.currentChunkX = 0;
        this.currentChunkY = 0;

        // Chunk Tracking
        this.totalChunks = maxRenderDistance * maxRenderDistance;
        this.gridCoordinates = new LongOpenHashSet(totalChunks);
        this.loadedChunks = new Long2ObjectOpenHashMap<>(totalChunks);

        // Queue Queue
        this.queueProcess = new QueueProcess[] {
                QueueProcess.Unload,
                QueueProcess.Load,
                QueueProcess.Render
        };
        this.loadOrder = new long[totalChunks];
        this.unloadQueue = new Long2ObjectOpenHashMap<>(totalChunks);
        this.loadFactor = 0.75f;
        int initialCapacity = (int) Math.ceil(totalChunks / loadFactor);
        this.loadQueue = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.loadQueueOrder = new LongArrayFIFOQueue(totalChunks);

        // Queue System
        this.loadedChunksThisFrame = 0;
        this.loadedChunksThisTick = 0;
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

        // Settings
        this.maxRenderDistance = settings.maxRenderDistance;

        // Chunk Tracking
        this.totalChunks = maxRenderDistance * maxRenderDistance;
        this.gridCoordinates = new LongOpenHashSet(totalChunks);
        this.loadedChunks = new Long2ObjectOpenHashMap<>(totalChunks);

        // Queue System
        this.loadOrder = new long[totalChunks];
        this.unloadQueue = new Long2ObjectOpenHashMap<>(totalChunks);
        int initialCapacity = (int) Math.ceil(totalChunks / loadFactor);
        this.loadQueue = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.loadQueueOrder = new LongArrayFIFOQueue(totalChunks);

        // Temporary array to hold coordinates with their distance
        class ChunkDistance {
            long coord;
            int distance; // distance from center

            ChunkDistance(long c, int d) {
                coord = c;
                distance = d;
            }
        }

        ChunkDistance[] temp = new ChunkDistance[totalChunks];
        int index = 0;

        // Assign the correct keys to the grid
        for (int x = -(maxRenderDistance / 2); x < maxRenderDistance / 2; x++) {

            for (int y = -(maxRenderDistance / 2); y < maxRenderDistance / 2; y++) {

                long gridCoordinate = Coordinate2Int.pack(x, y);
                gridCoordinates.add(gridCoordinate);

                // Compute Manhattan distance from center (0,0)
                int dist = Math.abs(x) + Math.abs(y);
                temp[index++] = new ChunkDistance(gridCoordinate, dist);
            }
        }

        // Sort coordinates by distance (closest to center first)
        Arrays.sort(temp, Comparator.comparingInt(cd -> cd.distance));

        // Fill loadOrder array in priority order
        for (int i = 0; i < temp.length; i++) {
            loadOrder[i] = temp[i].coord;
        }

    }

    // Update \\

    private void updateQueue() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        // Reset every tick
        if (worldTick.tick())
            loadedChunksThisTick = 0;

        // If there is no queue we don't need to do anything
        if (!hasQueue())
            return;

        // Alternating queue update
        for (int i = 0; i < queueProcess.length; i++) {

            if (queueProcess[queueBatch].process(this))
                return;

            queueBatch = (queueBatch + 1) % queueProcess.length;
        }
    }

    enum QueueProcess {

        Unload {
            boolean process(ChunkSystem system) {
                return system.unloadQueue();
            }
        },

        Load {
            boolean process(ChunkSystem system) {
                return system.loadQueue();
            }
        },

        Render {
            boolean process(ChunkSystem system) {
                return system.renderQueue();
            }
        };

        abstract boolean process(ChunkSystem system);
    }

    // Render \\

    private void renderChunks(ModelBatch modelBatch) {

    }

    // Main \\

    public void loadChunks(Vector2Int chunkCoordinate) {

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
        buildQueue();
    }

    private void clearQueue() {

        // Clear the queue
        unloadQueue.clear();
        loadQueue.clear();
        loadQueueOrder.clear();

        // Reversely remove all computed Coordinate2Ints from unloadQueue for efficiency
        unloadQueue.putAll(loadedChunks);
    }

    private void buildQueue() {

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

            // Remove the computed coordinate from the unload queue
            unloadQueue.remove(chunkCoordinate);

            // Attempt to use the chunk coordinate to retrieve a loaded chunk
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            // If the chunk was loaded we move it to the new grid position
            if (loadedChunk != null)
                loadedChunk.moveTo(gridCoordinate);

            else { // If the chunkCoordinate could not be found add it to the queue
                loadQueue.put(gridCoordinate, chunkCoordinate);
                loadQueueOrder.enqueue(gridCoordinate);
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

            // Remove from unloadQueue
            iterator.remove();
            loadedChunks.remove(chunkCoordinate);
            loadedChunk.dispose();

            // Increment counters
            incrementQueueTotal();
            index++;
        }

        return checkProcessLimit();
    }

    // Load \\

    private boolean loadQueue() {

        // Return early if there is no room for loading new chunks
        if (loadedChunks.size() == totalChunks)
            return checkProcessLimit();

        int index = 0;

        while (index < loadQueueOrder.size() && processIsSafe(index)) {
            long gridCoordinate = loadQueueOrder.dequeueLong();
            long chunkCoordinate = loadQueue.get(gridCoordinate);

            Chunk loadedChunk = chunkData.readChunk(chunkCoordinate);

            if (loadedChunk == null)
                loadedChunk = worldSystem.worldGenerator.generateChunk(chunkCoordinate);

            loadedChunk.moveTo(gridCoordinate);
            loadedChunks.put(chunkCoordinate, loadedChunk);

            // Increment counters
            incrementQueueTotal();
            index++;

        }

        return checkProcessLimit();
    }

    // Build \\

    private boolean renderQueue() {

        return checkProcessLimit();
    }

    // Utility \\

    private boolean processIsSafe(int index) {
        return index < processPerBatch &&
                loadedChunksThisFrame < MAX_CHUNK_LOADS_PER_FRAME &&
                loadedChunksThisTick < MAX_CHUNK_LOADS_PER_TICK;
    }

    private boolean checkProcessLimit() {
        return loadedChunksThisFrame >= MAX_CHUNK_LOADS_PER_FRAME ||
                loadedChunksThisTick >= MAX_CHUNK_LOADS_PER_TICK;
    }

    private void incrementQueueTotal() {

        loadedChunksThisFrame += 1;
        loadedChunksThisTick += 1;
    }

    public boolean hasQueue() {
        return false;
    }

    public int totalQueueSize() {
        return 0;
    }

    // Debug \\

    private void debug() { // TODO: Remove debug line

    }
}
