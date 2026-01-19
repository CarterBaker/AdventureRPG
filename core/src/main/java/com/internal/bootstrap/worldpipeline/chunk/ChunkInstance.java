package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.InstancePackage;
import com.internal.core.engine.settings.EngineSetting;

public class ChunkInstance extends InstancePackage {

    // Internal
    private WorldHandle worldHandle;
    private long chunkCoordinate;
    private volatile ChunkState chunkState;

    // SubChunks
    private SubChunkInstance[] subChunks;

    // Neighbors
    private ChunkNeighborStruct chunkNeighbors;

    // Dynamic Mesh
    private DynamicPacketInstance dynamicPacketInstance;

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

        // Dynamic Mesh
        this.dynamicPacketInstance = create(DynamicPacketInstance.class);
    }

    public void constructor(
            WorldHandle worldHandle,
            long chunkCoordinate,
            VAOHandle vaoHandle) {

        // Internal
        this.worldHandle = worldHandle;
        this.chunkCoordinate = chunkCoordinate;

        // SubChunks
        for (byte subChunkCoordinate = 0; subChunkCoordinate < EngineSetting.WORLD_HEIGHT; subChunkCoordinate++)
            subChunks[subChunkCoordinate].constructor(
                    subChunkCoordinate,
                    vaoHandle);

        // Neighbors
        this.chunkNeighbors.constructor(
                chunkCoordinate,
                this);

        // Dynamic Mesh
        this.dynamicPacketInstance.constructor(vaoHandle);
    }

    public void dispose() {

    }

    // Accessible \\

    // Internal
    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public long getChunkCoordinate() {
        return chunkCoordinate;
    }

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

    // Dynamic Mesh
    public DynamicPacketInstance getDynamicModelInstance() {
        return dynamicPacketInstance;
    }
}
