package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.settings.EngineSetting;

public class ChunkInstance extends WorldRenderInstance {

    // Internal
    private volatile ChunkState chunkState;

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

        for (short i = 0; i < EngineSetting.WORLD_HEIGHT; i++)
            subChunks[i] = create(SubChunkInstance.class);

        // Neighbors
        this.chunkNeighbors = new ChunkNeighborStruct();

        super.create();
    }

    @Override
    public void constructor(
            WorldRenderSystem worldRenderSystem,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle) {

        super.constructor(
                worldRenderSystem,
                worldHandle,
                coordinate,
                vaoHandle);

        // SubChunks
        for (byte subChunkCoordinate = 0; subChunkCoordinate < EngineSetting.WORLD_HEIGHT; subChunkCoordinate++)
            subChunks[subChunkCoordinate].constructor(
                    worldrendersystem,
                    worldHandle,
                    subChunkCoordinate,
                    vaoHandle);

        // Neighbors
        this.chunkNeighbors.constructor(
                coordinate,
                this);
    }

    // Utility \\

    public void merge() {

        dynamicPacketInstance.clear();

        for (SubChunkInstance subChunkInstance : subChunks)
            dynamicPacketInstance.merge(subChunkInstance.getDynamicPacketInstance());
    }

    // Accessible \\

    // Internal
    public QueueOperation getChunkStateOperation() {
        return chunkState.getAssociatedOperation();
    }

    public void setChunkState(ChunkState chunkState) {
        this.chunkState = chunkState;
    }

    // SubChunks
    public SubChunkInstance[] getSubChunks() {
        return subChunks;
    }

    public SubChunkInstance getSubChunk(int subChunkCoordinate) {
        return subChunks[subChunkCoordinate];
    }

    // Neighbors
    public ChunkNeighborStruct getChunkNeighbors() {
        return chunkNeighbors;
    }
}
