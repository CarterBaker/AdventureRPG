package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.bootstrap.worldpipeline.worldrendermanager.RenderType;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/*
 * A merged geometry batch composed of MEGA_CHUNK_SIZE^2 adjacent ChunkInstances.
 * Geometry is accumulated incrementally via batchAndMerge() as each chunk
 * contributes. If a chunk re-contributes (its geometry was rebuilt), the entire
 * packet is cleared and all registered chunks are re-merged — partial un-merge
 * is not supported. Once all chunks are present, finalizeGeometry() marks the
 * packet ready for GPU upload. Threading is governed by MegaDataSyncContainer.
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

    // Incremental Merge \\

    /*
     * Fresh contribution: merge the chunk's geometry into the packet and
     * register it. Re-contribution: the packet is cleared and all registered
     * chunks are re-merged in full, using the updated geometry for the
     * changed chunk. Returns false if any geometry merge step fails.
     */
    public boolean batchAndMerge(ChunkInstance chunkInstance) {
        long chunkCoord = chunkInstance.getCoordinate();
        boolean isRemerge = megaBatchStruct.getBatchedChunks().containsKey(chunkCoord);

        if (isRemerge) {
            megaBatchStruct.getBatchedChunks().put(chunkCoord, chunkInstance);
            megaBatchStruct.clearMerged();
            dynamicPacketInstance.clear();
            for (ChunkInstance batched : megaBatchStruct.getBatchedChunks().values()) {
                if (!mergeChunk(batched))
                    return false;
                megaBatchStruct.recordMerged(batched.getCoordinate());
            }
            return true;
        }

        if (!megaBatchStruct.registerChunk(chunkInstance))
            return false;
        if (!mergeChunk(chunkInstance))
            return false;
        megaBatchStruct.recordMerged(chunkCoord);
        return true;
    }

    private boolean mergeChunk(ChunkInstance chunkInstance) {
        long chunkCoord = chunkInstance.getCoordinate();
        int chunkX = Coordinate2Long.unpackX(chunkCoord);
        int chunkZ = Coordinate2Long.unpackY(chunkCoord);
        mergeOffsetValues[0] = (chunkX - megaX) * CHUNK_SIZE;
        mergeOffsetValues[1] = (chunkZ - megaZ) * CHUNK_SIZE;
        return dynamicPacketInstance.merge(
                chunkInstance.getDynamicPacketInstance(),
                vertPositionArray,
                mergeOffsetValues);
    }

    /*
     * Marks the packet ready for GPU upload once all chunks have contributed.
     * If the mega is empty after a full re-merge, the packet is unlocked instead.
     */
    public void finalizeGeometry() {
        if (dynamicPacketInstance.hasModels())
            dynamicPacketInstance.setReady();
        else
            dynamicPacketInstance.unlock();
    }

    // Accessible \\

    public MegaDataSyncContainer getMegaDataSyncContainer() {
        return megaDataSyncContainer;
    }

    public boolean isReadyToRender() {
        return megaBatchStruct.isReadyToRender();
    }

    public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks() {
        return megaBatchStruct.getBatchedChunks();
    }
}