package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotDetailLevel;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
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

    // Player position within mega (for boundary decisions)
    private int playerOffsetX;
    private int playerOffsetZ;
    private int MEGA_CHUNK_SIZE;

    // Internal \\

    @Override
    protected void create() {

        // Chunk Streaming
        this.chunkUnloadQueue = new LongLinkedOpenHashSet();
        this.megaChunkUnloadQueue = new LongLinkedOpenHashSet();
        this.chunkCoordinate2GridSlot = new Long2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.chunkQueueManager = get(ChunkQueueManager.class);
        this.chunkBatchSystem = get(ChunkBatchSystem.class);
        this.worldRenderSystem = get(WorldRenderSystem.class);

        this.MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
    }

    // Chunk Streaming \\

    void streamChunks(
            WorldHandle worldHandle,
            long activeChunkCoordinate,
            GridInstance gridInstance) {

        // Calculate player's offset within their mega chunk
        int activeChunkX = Coordinate2Long.unpackX(activeChunkCoordinate);
        int activeChunkZ = Coordinate2Long.unpackY(activeChunkCoordinate);
        long playerMegaCoord = Coordinate2Long.toMegaChunkCoordinate(activeChunkCoordinate);
        int playerMegaX = Coordinate2Long.unpackX(playerMegaCoord);
        int playerMegaZ = Coordinate2Long.unpackY(playerMegaCoord);

        playerOffsetX = activeChunkX - playerMegaX; // 0 to MEGA_CHUNK_SIZE-1
        playerOffsetZ = activeChunkZ - playerMegaZ; // 0 to MEGA_CHUNK_SIZE-1

        // Pass references and clear render queue
        worldRenderSystem.setActiveChunks(activeChunks);
        worldRenderSystem.setActiveMegaChunks(activeMegaChunks);
        worldRenderSystem.clearRenderQueue();

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

        // Mark all existing chunks/megas for potential unload
        chunkUnloadQueue.addAll(activeChunks.keySet());
        megaChunkUnloadQueue.addAll(activeMegaChunks.keySet());
        chunkCoordinate2GridSlot.clear();
    }

    private void createQueue(
            WorldHandle worldHandle,
            GridInstance gridInstance,
            long activeChunkCoordinate) {

        int activeChunkCoordinateX = Coordinate2Long.unpackX(activeChunkCoordinate);
        int activeChunkCoordinateY = Coordinate2Long.unpackY(activeChunkCoordinate);

        // The grid is prebuilt and never changes during gameplay
        // Loop through all grid slots and update chunk/mega assignments
        for (int i = 0; i < gridInstance.getTotalSlots(); i++)
            handleGridSlot(
                    worldHandle,
                    gridInstance,
                    gridInstance.getGridCoordinate(i),
                    activeChunkCoordinateX,
                    activeChunkCoordinateY);

        // Unload chunks that weren't in the new grid
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

        // Remove from unload queue - this chunk/mega is still needed
        chunkUnloadQueue.remove(chunkCoordinate);
        megaChunkUnloadQueue.remove(megaChunkCoordinate);

        // Store grid slot for this chunk
        GridSlotHandle gridSlotHandle = gridInstance.getGridSlot(gridCoordinate);
        GridSlotDetailLevel level = gridSlotHandle.getDetailLevel();
        chunkCoordinate2GridSlot.put(chunkCoordinate, gridSlotHandle);

        // Update or load chunk
        ChunkInstance loadedChunk = activeChunks.get(chunkCoordinate);

        if (loadedChunk != null)
            loadedChunk.setGridSlotHandle(gridSlotHandle);
        else
            chunkQueueManager.requestLoad(chunkCoordinate);

        // Update mega if it exists at this coordinate
        MegaChunkInstance loadedMegaChunk = activeMegaChunks.get(megaChunkCoordinate);

        if (loadedMegaChunk != null && (chunkCoordinate == megaChunkCoordinate))
            loadedMegaChunk.setGridSlotHandle(gridSlotHandle);

        // Queue for rendering with boundary logic
        queueForRendering(chunkCoordinate, megaChunkCoordinate, level);
    }

    private void queueForRendering(long chunkCoordinate, long megaCoordinate, GridSlotDetailLevel level) {

        // Calculate this chunk's offset within its mega
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        int megaX = Coordinate2Long.unpackX(megaCoordinate);
        int megaZ = Coordinate2Long.unpackY(megaCoordinate);

        int chunkOffsetX = chunkX - megaX; // 0 to MEGA_CHUNK_SIZE-1
        int chunkOffsetZ = chunkZ - megaZ; // 0 to MEGA_CHUNK_SIZE-1

        // Distance from player offset to chunk offset (Manhattan distance)
        int offsetDist = Math.abs(chunkOffsetX - playerOffsetX) + Math.abs(chunkOffsetZ - playerOffsetZ);

        // Boundary threshold - if a mega has chunks with offsetDist > this, render
        // chunks individually
        int boundaryThreshold = MEGA_CHUNK_SIZE / 2;

        if (level.level >= 6) { // BATCHED range
            // If this chunk is far from player's offset within the mega, it's at the edge
            // Render individual chunks instead of mega to avoid overlap
            if (offsetDist > boundaryThreshold) {
                worldRenderSystem.queueForRender(chunkCoordinate);
            } else {
                worldRenderSystem.queueForRender(megaCoordinate);
            }
        } else if (level.level >= 4) { // Individual range
            // Check if this chunk's mega is in BATCHED range
            // If so, skip this individual chunk (mega will handle it)
            if (offsetDist <= boundaryThreshold) {
                // Don't queue - mega will render it
                return;
            }
            worldRenderSystem.queueForRender(chunkCoordinate);
        }
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