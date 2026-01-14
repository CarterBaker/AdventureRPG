package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Int;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

class ChunkPositionSystem extends SystemPackage {

    // Internal
    private ChunkQueueManager chunkQueueManager;

    // Chunk Streaming
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectOpenHashMap<ChunkInstance> unloadQueue;

    // Internal \\

    @Override
    protected void create() {

        // Chunk Streaming
        this.unloadQueue = new Long2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.chunkQueueManager = get(ChunkQueueManager.class);
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

        // Clear unload map
        unloadQueue.clear();

        // Reversely remove all computed Coordinate2Ints from unloadQueue for efficiency
        unloadQueue.putAll(activeChunks);
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
                    gridInstance.getGridCoordinate(i),
                    activeChunkCoordinateX,
                    activeChunkCoordinateY);

        for (long chunkCoordinate : unloadQueue.keySet())
            chunkQueueManager.requestUnload(chunkCoordinate);
    }

    private void handleGridSlot(
            WorldHandle worldHandle,
            GridInstance gridInstance,
            long gridCoordinate,
            int activeChunkCoordinateX,
            int activeChunkCoordinateY) {

        // Use the position in grid as an offset
        int offsetX = Coordinate2Int.unpackX(gridCoordinate);
        int offsetY = Coordinate2Int.unpackY(gridCoordinate);

        // Use the current chunks coordinates to compute necessary chunks
        int chunkCoordinateX = activeChunkCoordinateX + offsetX;
        int chunkCoordinateY = activeChunkCoordinateY + offsetY;

        // Pack the coordinates into a usable long value and wrap it
        long chunkCoordinate = Coordinate2Int.pack(chunkCoordinateX, chunkCoordinateY);
        chunkCoordinate = WorldWrapUtility.wrapAroundWorld(worldHandle, chunkCoordinate);

        // Remove the computed coordinate from the unload queue
        unloadQueue.remove(chunkCoordinate);

        // Attempt to use the chunk coordinate to retrieve a loaded chunk
        ChunkInstance loadedChunk = activeChunks.get(chunkCoordinate);

        // If the chunk was loaded we move it to the new grid position
        if (loadedChunk != null)
            gridInstance.assignChunkToSlot(gridCoordinate, chunkCoordinate);

        else // If the chunkCoordinate could not be found add it to the queue
            chunkQueueManager.requestLoad(chunkCoordinate);
    }

    // Utility \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }
}
