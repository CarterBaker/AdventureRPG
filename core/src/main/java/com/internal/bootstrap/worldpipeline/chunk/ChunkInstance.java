package com.internal.bootstrap.worldpipeline.chunk;

import java.util.concurrent.atomic.AtomicReference;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.settings.EngineSetting;

public class ChunkInstance extends WorldRenderInstance {

    // Internal
    private AtomicReference<ChunkState> chunkState;

    // SubChunks
    private SubChunkInstance[] subChunks;

    // Neighbors
    private ChunkNeighborStruct chunkNeighbors;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkState = new AtomicReference<>(ChunkState.UNINITIALIZED);

        // SubChunks
        this.subChunks = new SubChunkInstance[EngineSetting.WORLD_HEIGHT];

        for (short i = 0; i < EngineSetting.WORLD_HEIGHT; i++)
            subChunks[i] = create(SubChunkInstance.class);

        // Neighbors
        this.chunkNeighbors = new ChunkNeighborStruct();

        super.create();
    }

    public void constructor(
            WorldRenderSystem worldRenderSystem,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle,
            short airBlockId) {

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
                    vaoHandle,
                    airBlockId);

        // Neighbors
        this.chunkNeighbors.constructor(
                coordinate,
                this);
    }

    // Utility \\

    public boolean merge() {

        boolean success = true;
        dynamicPacketInstance.clear();

        for (SubChunkInstance subChunkInstance : subChunks)
            if (!dynamicPacketInstance.merge(subChunkInstance.getDynamicPacketInstance()))
                success = false;

        return success;
    }

    // Accessible \\

    // Internal
    public ChunkState getChunkState() {
        return chunkState.get();
    }

    public void setChunkState(ChunkState chunkState) {
        this.chunkState.set(chunkState);
    }

    public QueueOperation getChunkStateOperation() {
        return chunkState.get().getAssociatedOperation();
    }

    public boolean tryBeginOperation(QueueOperation operation) {

        ChunkState targetState = switch (operation) {
            case GENERATE -> ChunkState.GENERATING_DATA;
            case NEIGHBOR_ASSESSMENT -> ChunkState.ASSESSING_NEIGHBORS;
            case BUILD -> ChunkState.GENERATING_GEOMETRY;
            case MERGE -> ChunkState.MERGING_DATA;
            case BATCH -> ChunkState.BATCHING_DATA;
            case SKIP -> null;
        };

        if (targetState == null)
            return false;

        // Try to transition from current state to target state
        ChunkState currentState = chunkState.get();

        // Already in the target state (already processing)
        if (currentState == targetState)
            return false;

        // Atomic transition
        return chunkState.compareAndSet(currentState, targetState);
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
