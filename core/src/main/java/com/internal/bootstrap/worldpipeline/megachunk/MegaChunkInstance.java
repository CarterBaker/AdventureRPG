package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.RenderType;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderManager;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/*
 * A merged geometry batch composed of MEGA_CHUNK_SIZE^2 adjacent ChunkInstances.
 * Threading is governed entirely by MegaDataSyncContainer — callers must check
 * isLocked() before reading or writing state or batch data. merge() runs on a
 * worker thread inside a SyncStructConsumer and must not be called directly.
 */
public class MegaChunkInstance extends WorldRenderInstance {

    // Internal
    private MegaDataSyncContainer megaDataSyncContainer;
    private int megaScale;
    private int[] vertPositionArray;
    private float[] mergeOffsetValues;
    private int CHUNK_SIZE;
    private int megaX;
    private int megaZ;
    // Batch Data
    private MegaBatchStruct megaBatchStruct;
    // Internal \\

    @Override
    protected void create() {
        this.megaDataSyncContainer = create(MegaDataSyncContainer.class);
        this.vertPositionArray = new int[] { 0, 2 };
        this.mergeOffsetValues = new float[2];
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.megaBatchStruct = new MegaBatchStruct(0, 0);
        super.create();
    }

    public void constructor(
            WorldRenderManager worldRenderSystem,
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
        megaBatchStruct.constructor(megaChunkCoordinate, megaScale);
        megaDataSyncContainer.resetData();
    }

    public void reset() {
        megaDataSyncContainer.resetData();
        dynamicPacketInstance.clear();
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

    public MegaDataSyncContainer getMegaDataSyncContainer() {
        return megaDataSyncContainer;
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