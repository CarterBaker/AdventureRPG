package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.core.engine.ManagerPackage;

public class ChunkStreamManager extends ManagerPackage {

    // Internal
    private PlayerManager playerManager;
    private ChunkPositionSystem chunkPositionSystem;
    private ChunkQueueSystem chunkQueueSystem;
    private GridManager gridManager;

    // Chunk Position
    private long activeChunkCoordinate;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkPositionSystem = create(ChunkPositionSystem.class);
        this.chunkQueueSystem = create(ChunkQueueSystem.class);
    }

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.gridManager = get(GridManager.class);
    }

    @Override
    protected void start() {

        // Chunk Position
        this.activeChunkCoordinate = playerManager.getPlayerPosition().getChunkCoordinate();
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
}
