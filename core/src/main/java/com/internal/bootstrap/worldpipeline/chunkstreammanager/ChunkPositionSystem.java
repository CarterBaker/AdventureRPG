package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

class ChunkPositionSystem extends SystemPackage {

    // Internal
    private ChunkQueueManager chunkQueueManager;
    private WorldRenderSystem worldRenderSystem;

    // Chunk Streaming
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;
    private Long2ObjectOpenHashMap<ChunkInstance> chunkUnloadQueue;
    private Long2ObjectOpenHashMap<MegaChunkInstance> megaChunkUnloadQueue;
    private Long2ObjectOpenHashMap<GridSlotHandle> chunkCoordinate2GridSlot;

    // Internal \\

    @Override
    protected void create() {

        // Chunk Streaming
        this.chunkUnloadQueue = new Long2ObjectOpenHashMap<>();
        this.megaChunkUnloadQueue = new Long2ObjectOpenHashMap<>();
        this.chunkCoordinate2GridSlot = new Long2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.chunkQueueManager = get(ChunkQueueManager.class);
        this.worldRenderSystem = get(WorldRenderSystem.class);
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
        chunkUnloadQueue.clear();
        megaChunkUnloadQueue.clear();

        // Reversely remove all computed Coordinate2Ints from chunkUnloadQueue
        chunkUnloadQueue.putAll(activeChunks);
        megaChunkUnloadQueue.putAll(activeMegaChunks);
        chunkCoordinate2GridSlot.clear();
    }

    private void createQueue(
            WorldHandle worldHandle,
            GridInstance gridInstance,
            long activeChunkCoordinate) {

        int activeChunkCoordinateX = Coordinate2Long.unpackX(activeChunkCoordinate);
        int activeChunkCoordinateY = Coordinate2Long.unpackY(activeChunkCoordinate);

        // The grid is prebuilt and never changes
        for (int i = 0; i < gridInstance.getTotalSlots(); i++)
            handleGridSlot(
                    worldHandle,
                    gridInstance,
                    gridInstance.getGridCoordinate(i),
                    activeChunkCoordinateX,
                    activeChunkCoordinateY);

        for (long chunkCoordinate : chunkUnloadQueue.keySet())
            chunkQueueManager.requestUnload(chunkCoordinate);

        for (long megaChunkCoordinate : megaChunkUnloadQueue.keySet())
            unloadMegaChunkInstance(megaChunkCoordinate);
    }

    private void handleGridSlot(
            WorldHandle worldHandle,
            GridInstance gridInstance,
            long gridCoordinate,
            int activeChunkCoordinateX,
            int activeChunkCoordinateY) {

        int offsetX = Coordinate2Long.unpackX(gridCoordinate);
        int offsetY = Coordinate2Long.unpackY(gridCoordinate);
        int chunkCoordinateX = activeChunkCoordinateX + offsetX;
        int chunkCoordinateY = activeChunkCoordinateY + offsetY;
        long chunkCoordinate = Coordinate2Long.pack(chunkCoordinateX, chunkCoordinateY);
        chunkCoordinate = WorldWrapUtility.wrapAroundWorld(worldHandle, chunkCoordinate);

        long megaChunkCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);

        gridInstance.assignChunkToSlot(
                gridCoordinate,
                chunkCoordinate,
                megaChunkCoordinate);

        chunkUnloadQueue.remove(chunkCoordinate);
        megaChunkUnloadQueue.remove(megaChunkCoordinate);

        GridSlotHandle gridSlotHandle = gridInstance.getGridSlot(gridCoordinate);
        chunkCoordinate2GridSlot.put(chunkCoordinate, gridSlotHandle);

        ChunkInstance loadedChunk = activeChunks.get(chunkCoordinate);

        if (loadedChunk != null)
            loadedChunk.setGridSlotHandle(gridSlotHandle);
        else
            chunkQueueManager.requestLoad(chunkCoordinate);

        MegaChunkInstance loadedMegaChunk = activeMegaChunks.get(megaChunkCoordinate);

        if (loadedMegaChunk != null)
            loadedMegaChunk.setGridSlotHandle(gridSlotHandle);

        worldRenderSystem.moveWorldInstance(chunkCoordinate, gridSlotHandle.getSlotUBO());
    }

    private void unloadMegaChunkInstance(long megaChunkCoordinate) {

        boolean dispose = true;
        MegaChunkInstance megaChunkInstance = megaChunkUnloadQueue.get(megaChunkCoordinate);

        // Check if any of this mega's chunks are still active
        Long2ObjectOpenHashMap<ChunkInstance> batchedChunks = megaChunkInstance.getBatchedChunks();

        // At least one chunk is still active, don't unload the mega
        for (ChunkInstance chunkInstance : batchedChunks.values()) {

            if (activeChunks.containsKey(chunkInstance.getCoordinate())) {
                dispose = false;
                break;
            }

            worldRenderSystem.removeWorldInstance(chunkInstance.getCoordinate());
        }

        if (!dispose)
            return;

        // Only remove rendering when actually disposing
        for (ChunkInstance chunkInstance : batchedChunks.values())
            worldRenderSystem.removeWorldInstance(chunkInstance.getCoordinate());

        megaChunkInstance.dispose();

    }

    // Utility \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }

    public void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        this.activeMegaChunks = activeMegaChunks;
    }

    // Accessible \\

    public GridSlotHandle getGridSlotHandleForChunk(long chunkCoordinate) {
        return chunkCoordinate2GridSlot.get(chunkCoordinate);
    }
}
