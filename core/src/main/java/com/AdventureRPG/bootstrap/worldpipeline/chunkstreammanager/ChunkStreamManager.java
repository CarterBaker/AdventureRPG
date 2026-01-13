package com.AdventureRPG.bootstrap.worldpipeline.chunkstreammanager;

import com.AdventureRPG.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.AdventureRPG.bootstrap.worldpipeline.gridmanager.GridManager;
import com.AdventureRPG.core.engine.ManagerPackage;

public class ChunkStreamManager extends ManagerPackage {

    // Internal
    private PlayerManager playerManager;
    private InternalQueueManager internalQueueManager;
    private GridManager gridManager;

    // Chunk Position
    private long activeChunkCoordinate;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.internalQueueManager = create(InternalQueueManager.class);
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

        internalQueueManager.streamChunks(
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
