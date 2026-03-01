package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/*
 * Root manager for chunk and mega lifecycle. Owns both activeChunks and
 * activeMegaChunks maps and dispatches them to their respective queue managers.
 * rebuildGrid() is the single gateway for any runtime settings change that
 * affects render distance or grid scale — all dependent systems are notified
 * from here.
 */
public class ChunkStreamManager extends ManagerPackage {

    // Internal
    private VAOManager vaoManager;
    private PlayerManager playerManager;
    private GridManager gridManager;
    private WorldRenderManager worldRenderSystem;
    private MegaStreamManager megaStreamManager;
    private VAOHandle chunkVAO;
    private long activeChunkCoordinate;
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;
    private ChunkQueueManager chunkQueueManager;
    // Internal \\

    @Override
    protected void create() {
        this.activeChunkCoordinate = Coordinate2Long.pack(-1, -1);
        this.activeMegaChunks = new Long2ObjectLinkedOpenHashMap<>();
        this.chunkQueueManager = create(ChunkQueueManager.class);
    }

    @Override
    protected void get() {
        this.vaoManager = get(VAOManager.class);
        this.playerManager = get(PlayerManager.class);
        this.gridManager = get(GridManager.class);
        this.worldRenderSystem = get(WorldRenderManager.class);
        this.megaStreamManager = get(MegaStreamManager.class);
    }

    @Override
    protected void start() {
        this.chunkVAO = vaoManager.getVAOHandleFromName(EngineSetting.CHUNK_VAO);
        gridManager.getGrid().setWorldHandle(playerManager.getPlayer().getWorldHandle());
        int maxChunks = gridManager.getGrid().getTotalSlots() + EngineSetting.CHUNK_POOL_MAX_OVERFLOW;
        this.activeChunks = new Long2ObjectLinkedOpenHashMap<>(maxChunks);
        chunkQueueManager.setActiveChunks(activeChunks);
        megaStreamManager.setActiveMegaChunks(activeMegaChunks);
    }

    @Override
    protected void update() {
        streamChunks();
    }

    // Chunk Streaming \\

    private void streamChunks() {
        if (!newActiveChunk())
            return;
        gridManager.getGrid().setActiveChunkCoordinate(activeChunkCoordinate);
        worldRenderSystem.rebuildRenderQueue();
    }

    private boolean newActiveChunk() {
        long playerChunkCoordinate = playerManager.getPlayerPosition().getChunkCoordinate();
        if (activeChunkCoordinate == playerChunkCoordinate)
            return false;
        activeChunkCoordinate = playerChunkCoordinate;
        return true;
    }

    // Runtime Grid Rebuild — single gateway for all settings changes \\

    public void rebuildGrid() {
        // 1. Rebuild the grid itself
        gridManager.rebuildGrid();
        gridManager.getGrid().setWorldHandle(playerManager.getPlayer().getWorldHandle());
        gridManager.getGrid().setActiveChunkCoordinate(activeChunkCoordinate);

        // 2. Flush and resize active chunk map
        int maxChunks = gridManager.getGrid().getTotalSlots() + EngineSetting.CHUNK_POOL_MAX_OVERFLOW;
        activeChunks = new Long2ObjectLinkedOpenHashMap<>(maxChunks);
        chunkQueueManager.setActiveChunks(activeChunks);

        // 3. Notify chunk queue — flushes active chunks and clears requests
        chunkQueueManager.onGridRebuilt();

        // 4. Notify mega queue — recomputes cap and flushes active megas
        megaStreamManager.onGridRebuilt();

        // 5. Rebuild render queue with new grid
        worldRenderSystem.rebuildRenderQueue();
    }

    // Utility \\

    public void invalidateChunkBatch(long chunkCoordinate) {
        ChunkInstance chunk = activeChunks.get(chunkCoordinate);
        if (chunk == null)
            return;
        ChunkDataSyncContainer sync = chunk.getChunkDataSyncContainer();
        if (!sync.tryAcquire())
            return;
        try {
            sync.data[ChunkData.BATCH_DATA.index] = false;
        } finally {
            sync.release();
        }
    }

    public void invalidateMegaForChunk(long chunkCoordinate) {
        megaStreamManager.invalidateMegaForChunk(chunkCoordinate);
    }

    // Accessible \\

    public VAOHandle getChunkVAO() {
        return chunkVAO;
    }

    public WorldHandle getActiveWorldHandle() {
        return playerManager.getPlayer().getWorldHandle();
    }

    public ChunkInstance getChunkInstance(long chunkCoordinate) {
        return activeChunks.get(chunkCoordinate);
    }
}