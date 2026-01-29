package com.internal.bootstrap.worldpipeline.megachunk;

import java.util.concurrent.atomic.AtomicReference;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.RenderOperation;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class MegaChunkInstance extends WorldRenderInstance {

    // Internal
    private int megaScale;
    private AtomicReference<MegaState> megaState;

    // Batch Data
    private MegaBatchStruct megaBatchStruct;

    @Override
    protected void create() {

        // Internal
        this.megaState = new AtomicReference<>(MegaState.UNINITIALIZED);

        super.create();
    }

    public void constructor(
            WorldRenderSystem worldRenderSystem,
            WorldHandle worldHandle,
            long megaChunkCoordinate,
            VAOHandle vaoHandle,
            int megaScale) {

        super.constructor(
                worldRenderSystem,
                worldHandle,
                megaChunkCoordinate,
                vaoHandle);

        // Batch Data
        this.megaScale = megaScale;
        megaBatchStruct = new MegaBatchStruct(
                coordinate,
                megaScale);
    }

    // Utility \\

    public boolean merge() {

        boolean success = true;
        dynamicPacketInstance.clear();

        Long2ObjectOpenHashMap<ChunkInstance> batchedChunks = megaBatchStruct.getBatchedChunks();

        for (ChunkInstance chunkInstance : batchedChunks.values())
            if (!dynamicPacketInstance.merge(chunkInstance.getDynamicPacketInstance()))
                success = false;

        return success;
    }

    // Accessibility \\

    // Internal
    public MegaState getMegaState() {
        return megaState.get();
    }

    public void setMegaState(MegaState megaState) {
        this.megaState.set(megaState);
    }

    public RenderOperation getMegaRenderOperation() {
        return megaState.get().getRenderOperation();
    }

    public boolean tryBeginOperation(MegaState targetState) {

        MegaState currentState = megaState.get();

        // Already in the target state
        if (currentState == targetState)
            return false;

        // Atomic transition
        return megaState.compareAndSet(currentState, targetState);
    }

    // Batch Data

    public boolean isComplete() {
        return megaBatchStruct.isComplete();
    }

    public boolean batchChunk(ChunkInstance chunkInstance) {
        return megaBatchStruct.batchChunk(chunkInstance);
    }

    public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks() {
        return megaBatchStruct.getBatchedChunks();
    }
}
