package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Int;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

class ChunkPositionSystem extends SystemPackage {

    // Internal
    private ChunkQueueSystem chunkQueueSystem;

    // Chunk Streaming
    private Long2ObjectOpenHashMap<ChunkInstance> loadedChunks;
    private Long2LongOpenHashMap gridCoordinate2ChunkInstance;
    private Long2LongOpenHashMap chunkInstance2GridCoordinate;

    // Chunk Queue
    private Long2ObjectOpenHashMap<ChunkInstance> unloadQueue;

    // Internal \\

    @Override
    protected void create() {

        // Chunk Streaming
        this.loadedChunks = new Long2ObjectOpenHashMap<>();
        this.gridCoordinate2ChunkInstance = new Long2LongOpenHashMap();
        this.chunkInstance2GridCoordinate = new Long2LongOpenHashMap();

        // Chunk Queue
        this.unloadQueue = new Long2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.chunkQueueSystem = get(ChunkQueueSystem.class);
    }

    // Chunk Streaming \\

    void streamChunks(
            WorldHandle worldHandle,
            long activeChunkCoordinate,
            GridInstance gridInstance) {

        clearQueue();

        createQueue(
                worldHandle,
                gridInstance,
                activeChunkCoordinate);

    }

    private void clearQueue() {

        // Clear mappings
        gridCoordinate2ChunkInstance.clear();
        chunkInstance2GridCoordinate.clear();

        // Clear unload map
        unloadQueue.clear();

        // Reversely remove all computed Coordinate2Ints from unloadQueue for efficiency
        unloadQueue.putAll(loadedChunks);
    }

    private void createQueue(
            WorldHandle worldHandle,
            GridInstance gridInstance,
            long activeChunkCoordinate) {

        int activeChunkCoordinateX = Coordinate2Int.unpackX(activeChunkCoordinate);
        int activeChunkCoordinateY = Coordinate2Int.unpackY(activeChunkCoordinate);

        // The grid is prebuilt and never changes
        for (int i = 0; i < gridInstance.getTotalSlots(); i++)
            handleGridSlot(
                    worldHandle,
                    gridInstance,
                    activeChunkCoordinateX,
                    activeChunkCoordinateY,
                    i);

        for (long chunkCoordinate : unloadQueue.keySet())
            chunkQueueSystem.requestUnload(chunkCoordinate);
    }

    private void handleGridSlot(
            WorldHandle worldHandle,
            GridInstance gridInstance,
            int activeChunkCoordinateX,
            int activeChunkCoordinateY,
            int i) {

        long gridCoordinate = gridInstance.getGridCoordinate(i);

        // Use the position in grid as an offset
        int offsetX = Coordinate2Int.unpackX(gridCoordinate);
        int offsetY = Coordinate2Int.unpackY(gridCoordinate);

        // Use the current chunks coordinates to compute necessary chunks
        int chunkCoordinateX = activeChunkCoordinateX + offsetX;
        int chunkCoordinateY = activeChunkCoordinateY + offsetY;

        // Pack the coordinates into a usable long value and wrap it
        long chunkCoordinate = Coordinate2Int.pack(chunkCoordinateX, chunkCoordinateY);
        chunkCoordinate = WorldWrapUtility.wrapAroundWorld(worldHandle, chunkCoordinate);

        // The very first step is to remap all the chunks to the correct position
        gridCoordinate2ChunkInstance.put(gridCoordinate, chunkCoordinate);
        chunkInstance2GridCoordinate.put(chunkCoordinate, gridCoordinate);

        // Remove the computed coordinate from the unload queue
        unloadQueue.remove(chunkCoordinate);

        // Attempt to use the chunk coordinate to retrieve a loaded chunk
        ChunkInstance loadedChunk = loadedChunks.get(chunkCoordinate);

        // If the chunk was loaded we move it to the new grid position
        if (loadedChunk != null)
            gridInstance.assignChunkToSlot(gridCoordinate, chunkCoordinate);

        else // If the chunkCoordinate could not be found add it to the queue
            chunkQueueSystem.requestLoad(chunkCoordinate);
    }
}
