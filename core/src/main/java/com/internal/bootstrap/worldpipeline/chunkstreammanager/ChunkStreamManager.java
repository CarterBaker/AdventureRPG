package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
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

    private ChunkPositionSystem chunkPositionSystem;
    private ChunkQueueManager chunkQueueManager;
    private BatchSystem batchSystem;

    // Chunk Position
    private long activeChunkCoordinate;
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkPositionSystem = create(ChunkPositionSystem.class);
        this.chunkQueueManager = create(ChunkQueueManager.class);
        this.batchSystem = create(BatchSystem.class);

        // Chunk Position
        this.activeChunkCoordinate = Coordinate2Long.pack(-1, -1);
        this.activeChunks = new Long2ObjectLinkedOpenHashMap<>();
        this.activeMegaChunks = new Long2ObjectLinkedOpenHashMap<>();

        // Assign the same collection reference to all systems
        this.chunkPositionSystem.setActiveChunks(activeChunks);
        this.chunkPositionSystem.setActiveMegaChunks(activeMegaChunks);
        this.chunkQueueManager.setActiveChunks(activeChunks);
        this.chunkQueueManager.setActiveMegaChunks(activeMegaChunks);
        this.batchSystem.setActiveMegaChunks(activeMegaChunks);
    }

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
        this.playerManager = get(PlayerManager.class);
        this.gridManager = get(GridManager.class);
    }

    @Override
    protected void awake() {

        // Internal
        this.chunkVAO = vaoManager.getVAOHandleFromName(EngineSetting.CHUNK_VAO);
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

    // Used to compare the players currently active chunk coordinate
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
}
