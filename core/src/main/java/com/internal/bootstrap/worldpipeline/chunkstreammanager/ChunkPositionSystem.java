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
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

class ChunkPositionSystem extends SystemPackage {

    // Internal
    private ChunkQueueManager chunkQueueManager;
    private ChunkBatchSystem chunkBatchSystem;
    private WorldRenderSystem worldRenderSystem;

    // Chunk Streaming
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;
    private LongLinkedOpenHashSet chunkUnloadQueue;
    private LongLinkedOpenHashSet megaChunkUnloadQueue;
    private Long2ObjectOpenHashMap<GridSlotHandle> chunkCoordinate2GridSlot;

    // Internal \\

    @Override
    protected void create() {

        this.chunkUnloadQueue = new LongLinkedOpenHashSet();
        this.megaChunkUnloadQueue = new LongLinkedOpenHashSet();
        this.chunkCoordinate2GridSlot = new Long2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        this.chunkQueueManager = get(ChunkQueueManager.class);
        this.chunkBatchSystem = get(ChunkBatchSystem.class);
        this.worldRenderSystem = get(WorldRenderSystem.class);
    }

    // Chunk Streaming \\

    void streamChunks(
            WorldHandle worldHandle,
            long activeChunkCoordinate,
            GridInstance gridInstance) {

        int activeChunkCoordinateX = Coordinate2Long.unpackX(activeChunkCoordinate);
        int activeChunkCoordinateY = Coordinate2Long.unpackY(activeChunkCoordinate);

        clearQueue();

        createQueue(
                worldHandle,
                gridInstance,
                activeChunkCoordinateX,
                activeChunkCoordinateY);

        worldRenderSystem.rebuildRenderQueue();
    }

    private void clearQueue() {

        chunkUnloadQueue.clear();
        megaChunkUnloadQueue.clear();

        chunkUnloadQueue.addAll(activeChunks.keySet());
        megaChunkUnloadQueue.addAll(activeMegaChunks.keySet());
        chunkCoordinate2GridSlot.clear();
    }

    private void createQueue(
            WorldHandle worldHandle,
            GridInstance gridInstance,
            int activeChunkCoordinateX,
            int activeChunkCoordinateY) {

        for (int i = 0; i < gridInstance.getTotalSlots(); i++)
            handleGridSlot(
                    worldHandle,
                    gridInstance,
                    gridInstance.getGridCoordinate(i),
                    activeChunkCoordinateX,
                    activeChunkCoordinateY);

        for (long chunkCoordinate : chunkUnloadQueue)
            chunkQueueManager.requestUnload(chunkCoordinate);

        for (long megaChunkCoordinate : megaChunkUnloadQueue)
            chunkBatchSystem.requestUnload(megaChunkCoordinate);
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

        if (loadedMegaChunk != null && chunkCoordinate == megaChunkCoordinate)
            loadedMegaChunk.setGridSlotHandle(gridSlotHandle);

        gridSlotHandle.setChunkCoordinate(chunkCoordinate);
        gridSlotHandle.setMegaCoordinate(megaChunkCoordinate);
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