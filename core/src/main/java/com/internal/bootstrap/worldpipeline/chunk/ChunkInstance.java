package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.InstancePackage;
import com.internal.core.engine.settings.EngineSetting;

public class ChunkInstance extends InstancePackage {

    // Internal
    private long chunkCoordinate;
    private ChunkState chunkState;

    // SubChunks
    private SubChunkInstance[] subChunks;

    // Neighbors
    private ChunkNeighborStruct chunkNeighbors;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkState = ChunkState.UNINITIALIZED;

        // SubChunks
        this.subChunks = new SubChunkInstance[EngineSetting.WORLD_HEIGHT];

        for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++)
            subChunks[i] = create(SubChunkInstance.class);

        // Neighbors
        this.chunkNeighbors = new ChunkNeighborStruct();
    }

    public void constructor(long chunkCoordinate) {

        // Internal
        this.chunkCoordinate = chunkCoordinate;

        // Neighbors
        this.chunkNeighbors.constructor(chunkCoordinate);
    }

    public void dispose() {

    }

    // Accessible \\

    public long getChunkCoordinate() {
        return chunkCoordinate;
    }

    public ChunkState getChunkState() {
        return chunkState;
    }

    public QueueOperation getChunkStateOperation() {
        return chunkState.getAssociatedOperation();
    }

    public void setChunkState(ChunkState chunkState) {
        this.chunkState = chunkState;
    }

    public SubChunkInstance[] getSubChunks() {
        return subChunks;
    }

    public SubChunkInstance getSubChunk(int i) {
        return subChunks[i];
    }

    public ChunkNeighborStruct getChunkNeighbors() {
        return chunkNeighbors;
    }
}
