package com.AdventureRPG.bootstrap.worldpipeline.chunkstreammanager;

import com.AdventureRPG.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.AdventureRPG.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.AdventureRPG.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.AdventureRPG.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.util.mathematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.queuesystem.QueueInstance;
import com.AdventureRPG.core.util.queuesystem.QueueItemHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

class InternalQueueManager extends ManagerPackage {

    // Internal
    private InternalStreamSystem internalStreamSystem;
    private QueueInstance queueInstance;

    // Chunk Streaming
    private Long2ObjectOpenHashMap<ChunkInstance> loadedChunks;
    private Long2LongOpenHashMap gridCoordinate2ChunkInstance;
    private Long2LongOpenHashMap chunkInstance2GridCoordinate;

    // Chunk Queue
    private Long2ObjectOpenHashMap<ChunkInstance> unloadQueue;

    // Queue Utility
    private ChunkStreamQueue activeQueueItem;
    private Int2ObjectOpenHashMap<ChunkStreamQueue> queueItemMap;

    private int processPerBatch;
    private int loadedChunksThisFrame;

    private static final long INVALID_GRID_SLOT = Long.MIN_VALUE;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.queueInstance = create(QueueInstance.class);

        for (ChunkStreamQueue queue : ChunkStreamQueue.values())
            queueInstance.addQueueItem(queue.name(), queue.priority);

        // Chunk Streaming
        this.loadedChunks = new Long2ObjectOpenHashMap<>();
        this.gridCoordinate2ChunkInstance = new Long2LongOpenHashMap();
        this.chunkInstance2GridCoordinate = new Long2LongOpenHashMap();
        this.chunkInstance2GridCoordinate.defaultReturnValue(INVALID_GRID_SLOT);

        // Chunk Queue
        this.unloadQueue = new Long2ObjectOpenHashMap<>();

        // Queue Utility
        this.activeQueueItem = ChunkStreamQueue.LOAD;
        this.queueItemMap = new Int2ObjectOpenHashMap<>();

        this.processPerBatch = 32;
        this.loadedChunksThisFrame = 0;
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
            internalStreamSystem.requestLoad(chunkCoordinate);
    }

    // Chunk Queue \\

    private void processData() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        while (!totalProcessesThisFrame() && hasQueueData()) {

            QueueItemHandle queueItemHandle = queueInstance.getNextQueueItem();
            activeQueueItem = queueItemMap.get(queueItemHandle.getQueueItemID());

            if (activeQueueItem == null)
                continue;

            switch (activeQueueItem) {
                case LOAD -> processLoadData();
                case GENERATE -> {
                }
                case ASSESSMENT -> {
                }
                case BUILD -> {
                }
                case BATCH -> {
                }
                case UNLOAD -> {
                }
            }
        }
    }

    private boolean hasQueueData() {
        return !unloadQueue.isEmpty();
    }

    // Load \\

    private boolean processLoadData() {

        int index = 0;

        while (processIsSafe(index)) {

            ChunkInstance loadedChunk = internalStreamSystem.pollLoadedChunk();

            if (loadedChunk == null)
                return totalProcessesThisFrame();

            long chunkCoordinate = loadedChunk.getChunkCoordinate();
            long gridCoordinate = chunkInstance2GridCoordinate.get(chunkCoordinate);

            if (gridCoordinate == INVALID_GRID_SLOT)
                continue;

            loadedChunks.put(gridCoordinate, loadedChunk);
            gridCoordinate2ChunkInstance.put(gridCoordinate, chunkCoordinate);
            chunkInstance2GridCoordinate.put(chunkCoordinate, gridCoordinate);

            internalStreamSystem.requestGenerate(loadedChunk);

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessesThisFrame();
    }

    // Utility \\

    private boolean processIsSafe(int index) {
        return index < processPerBatch &&
                loadedChunksThisFrame < EngineSetting.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private boolean totalProcessesThisFrame() {
        return loadedChunksThisFrame <= EngineSetting.MAX_CHUNK_LOADS_PER_FRAME;
    }

    private int incrementQueueTotal(int index) {

        loadedChunksThisFrame++;

        return ++index;
    }
}
