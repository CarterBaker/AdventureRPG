package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkStreamManager extends ManagerPackage {

    // Internal
    private PlayerManager playerManager;
    private ChunkPositionSystem chunkPositionSystem;
    private ChunkQueueManager chunkQueueManager;
    private GridManager gridManager;

    // Chunk Position
    private long activeChunkCoordinate;
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkPositionSystem = create(ChunkPositionSystem.class);
        this.chunkQueueManager = create(ChunkQueueManager.class);

        // Chunk Position
        this.activeChunkCoordinate = Coordinate2Long.pack(-1, -1);
        this.activeChunks = new Long2ObjectLinkedOpenHashMap<>();

        // Assign the same collection reference to all systems
        this.chunkPositionSystem.setActiveChunks(activeChunks);
        this.chunkQueueManager.setActiveChunks(activeChunks);
    }

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.gridManager = get(GridManager.class);
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
