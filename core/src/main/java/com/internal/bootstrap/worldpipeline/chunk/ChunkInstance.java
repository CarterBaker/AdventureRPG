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

    private int[] vertPositionArray;
    private float[] mergeOffsetValues;
    private int CHUNK_SIZE;

    // SubChunks
    private SubChunkInstance[] subChunks;

    // Neighbors
    private ChunkNeighborStruct chunkNeighbors;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkState = new AtomicReference<>(ChunkState.UNINITIALIZED);
        this.vertPositionArray = new int[] { 1 }; // We only need to change the height when merging sub chunks
        this.mergeOffsetValues = new float[1];
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;

        // SubChunks
        this.subChunks = new SubChunkInstance[EngineSetting.WORLD_HEIGHT];

        for (short i = 0; i < EngineSetting.WORLD_HEIGHT; i++)
            subChunks[i] = create(SubChunkInstance.class);

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
        this.chunkNeighbors = new ChunkNeighborStruct(
                coordinate,
                this);
    }

    // Utility \\

    public boolean merge() {

        boolean success = true;
        dynamicPacketInstance.clear();

        for (SubChunkInstance subChunkInstance : subChunks) {

            mergeOffsetValues[0] = subChunkInstance.getCoordinate() * CHUNK_SIZE;

            if (!dynamicPacketInstance.merge(
                    subChunkInstance.getDynamicPacketInstance(),
                    vertPositionArray,
                    mergeOffsetValues))
                success = false;
        }

        if (success && dynamicPacketInstance.hasModels())
            dynamicPacketInstance.setReady();
        else if (!dynamicPacketInstance.hasModels())
            dynamicPacketInstance.unlock();

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
