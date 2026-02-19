package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkStreamManager extends ManagerPackage {

    // Internal
    private VAOManager vaoManager;
    private PlayerManager playerManager;
    private GridManager gridManager;
    private VAOHandle chunkVAO;
    private long activeChunkCoordinate;
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;
    private ChunkPositionSystem chunkPositionSystem;
    private ChunkQueueManager chunkQueueManager;
    private ChunkBatchSystem chunkBatchSystem;

    // Internal \\

    @Override
    protected void create() {

        this.activeChunkCoordinate = Coordinate2Long.pack(-1, -1);
        this.activeChunks = new Long2ObjectLinkedOpenHashMap<>();
        this.activeMegaChunks = new Long2ObjectLinkedOpenHashMap<>();

        this.chunkPositionSystem = create(ChunkPositionSystem.class);
        this.chunkQueueManager = create(ChunkQueueManager.class);
        this.chunkBatchSystem = create(ChunkBatchSystem.class);
    }

    @Override
    protected void get() {

        this.vaoManager = get(VAOManager.class);
        this.playerManager = get(PlayerManager.class);
        this.gridManager = get(GridManager.class);

        // Push placeholder references - replaced in awake once grid is ready
        this.chunkPositionSystem.setActiveChunks(activeChunks);
        this.chunkPositionSystem.setActiveMegaChunks(activeMegaChunks);
        this.chunkQueueManager.setActiveChunks(activeChunks);
        this.chunkBatchSystem.setActiveMegaChunks(activeMegaChunks);
    }

    @Override
    protected void awake() {

        this.chunkVAO = vaoManager.getVAOHandleFromName(EngineSetting.CHUNK_VAO);

        // Grid is built by now - replace map with correctly sized instance
        int maxChunks = gridManager.getGrid().getTotalSlots() + EngineSetting.CHUNK_POOL_MAX_OVERFLOW;
        this.activeChunks = new Long2ObjectLinkedOpenHashMap<>(maxChunks);

        // Re-push to all dependents
        this.chunkPositionSystem.setActiveChunks(activeChunks);
        this.chunkQueueManager.setActiveChunks(activeChunks);
    }

    @Override
    protected void update() {
        streamChunks();
    }

    // Chunk Streaming \\

    private void streamChunks() {

        if (!newActiveChunk())
            return;

        chunkPositionSystem.streamChunks(
                playerManager.getPlayer().getWorldHandle(),
                activeChunkCoordinate,
                gridManager.getGrid());
    }

    private boolean newActiveChunk() {

        long playerChunkCoordinate = playerManager.getPlayerPosition().getChunkCoordinate();

        if (activeChunkCoordinate == playerChunkCoordinate)
            return false;

        activeChunkCoordinate = playerChunkCoordinate;
        return true;
    }

    // Utility \\

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