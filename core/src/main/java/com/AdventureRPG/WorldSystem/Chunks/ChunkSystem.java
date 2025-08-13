package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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

    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;

    private final int MAX_CHUNK_LOADS_PER_FRAME;
    private final int MAX_CHUNK_LOADS_PER_TICK;

    // Position Tracking
    private long currentChunk;
    private int currentChunkX, currentChunkY;

    // Chunk Tracking
    private int totalChunks;

    private LongOpenHashSet gridCoordinates;
    private Long2ObjectOpenHashMap<Chunk> loadedChunks;

    private Long2ObjectOpenHashMap<Chunk> unloadQueue;
    private Long2LongOpenHashMap loadQueue;

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

        this.CHUNK_SIZE = settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = settings.WORLD_HEIGHT;

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

        this.unloadQueue = new Long2ObjectOpenHashMap<>(totalChunks);
        this.loadQueue = new Long2LongOpenHashMap(totalChunks + maxRenderDistance * 2, 1f);

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

        this.unloadQueue = new Long2ObjectOpenHashMap<>(totalChunks);
        this.loadQueue = new Long2LongOpenHashMap(totalChunks, 1f);

        // Assign the correct keys to the grid
        for (int x = -(maxRenderDistance / 2); x <= maxRenderDistance / 2; x++) {

            for (int y = -(maxRenderDistance / 2); y <= maxRenderDistance / 2; y++) {

                long gridCoordinate = Coordinate2Int.pack(x, y);
                gridCoordinates.add(gridCoordinate);
            }
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

        // TODO: Process the queue
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
            worldSystem.wrapAroundWorld(chunkCoordinate);

            // Remove the computed coordinate from the unload queue
            unloadQueue.remove(chunkCoordinate);

            // Attempt to use the chunk coordinate to retrieve a loaded chunk
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            // If the chunk was loaded we move it to the new grid position
            if (loadedChunk != null)
                loadedChunk.moveTo(gridCoordinate);

            else // If the chunkCoordinate could not be found add it to the queue
                loadQueue.put(gridCoordinate, chunkCoordinate);
        }
    }

    // Unload \\

    private void UpdateUnloading() {

        increaseQueueCount();
    }

    // Load \\

    private void updateLoading() {

        increaseQueueCount();
    }

    // Build \\

    private void updateBuilding() {

        increaseQueueCount();
    }

    // Utility \\

    private void increaseQueueCount() {

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
