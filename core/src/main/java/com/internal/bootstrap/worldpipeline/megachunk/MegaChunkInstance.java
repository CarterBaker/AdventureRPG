package com.internal.bootstrap.worldpipeline.megachunk;

import java.util.concurrent.atomic.AtomicReference;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.RenderType;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class MegaChunkInstance extends WorldRenderInstance {

    // Internal
    private int megaScale;
    private AtomicReference<MegaState> megaState;

    private int[] vertPositionArray;
    private float[] mergeOffsetValues;
    private int CHUNK_SIZE;
    private int megaX;
    private int megaZ;

    // Batch Data
    private MegaBatchStruct megaBatchStruct;

    @Override
    protected void create() {

        this.megaState = new AtomicReference<>(MegaState.UNINITIALIZED);
        this.vertPositionArray = new int[] { 0, 2 };
        this.mergeOffsetValues = new float[2];
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;

        // Batch struct created once, re-initialized on reuse
        this.megaBatchStruct = new MegaBatchStruct(0, 0);

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
                RenderType.BATCHED,
                megaChunkCoordinate,
                vaoHandle);

        this.megaX = Coordinate2Long.unpackX(megaChunkCoordinate);
        this.megaZ = Coordinate2Long.unpackY(megaChunkCoordinate);
        this.megaScale = megaScale;

        // Re-initialize struct in place
        megaBatchStruct.constructor(megaChunkCoordinate, megaScale);

        // Reset state for reuse
        megaState.set(MegaState.UNINITIALIZED);
    }

    public void reset() {

        megaState.set(MegaState.UNINITIALIZED);
        dynamicPacketInstance.clear();
        setGridSlotHandle(null);
        megaBatchStruct.reset();
    }

    // Utility \\

    public boolean merge() {

        boolean success = true;
        dynamicPacketInstance.clear();

        Long2ObjectOpenHashMap<ChunkInstance> batchedChunks = megaBatchStruct.getBatchedChunks();

        for (ChunkInstance chunkInstance : batchedChunks.values()) {

            long chunkCoord = chunkInstance.getCoordinate();
            int chunkX = Coordinate2Long.unpackX(chunkCoord);
            int chunkZ = Coordinate2Long.unpackY(chunkCoord);

            mergeOffsetValues[0] = (chunkX - megaX) * CHUNK_SIZE;
            mergeOffsetValues[1] = (chunkZ - megaZ) * CHUNK_SIZE;

            if (!dynamicPacketInstance.merge(
                    chunkInstance.getDynamicPacketInstance(),
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

    public MegaState getMegaState() {
        return megaState.get();
    }

    public void setMegaState(MegaState megaState) {
        this.megaState.set(megaState);
    }

    public boolean tryBeginOperation(MegaState targetState) {

        MegaState currentState = megaState.get();

        if (currentState == targetState)
            return false;

        return megaState.compareAndSet(currentState, targetState);
    }

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