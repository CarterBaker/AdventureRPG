package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.bootstrap.worldpipeline.worldrendermanager.RenderType;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkInstance extends WorldRenderInstance {

    // Internal
    private ChunkDataSyncContainer chunkDataSyncContainer;
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
        this.chunkDataSyncContainer = create(ChunkDataSyncContainer.class);
        this.vertPositionArray = new int[] { 1 };
        this.mergeOffsetValues = new float[1];
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.subChunks = new SubChunkInstance[EngineSetting.WORLD_HEIGHT];
        for (short i = 0; i < EngineSetting.WORLD_HEIGHT; i++)
            subChunks[i] = create(SubChunkInstance.class);
        super.create();
    }

    public void constructor(
            WorldRenderManager worldRenderSystem,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle,
            short airBlockId,
            short defaultBiomeId,
            Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {

        super.constructor(
                worldRenderSystem,
                worldHandle,
                RenderType.INDIVIDUAL,
                coordinate,
                vaoHandle);

        for (byte subChunkCoordinate = 0; subChunkCoordinate < EngineSetting.WORLD_HEIGHT; subChunkCoordinate++)
            subChunks[subChunkCoordinate].constructor(
                    worldRenderSystem,
                    worldHandle,
                    subChunkCoordinate,
                    vaoHandle,
                    airBlockId,
                    defaultBiomeId);

        this.chunkNeighbors = new ChunkNeighborStruct(
                coordinate,
                this,
                activeChunks);
    }

    public void reset() {
        chunkDataSyncContainer.resetData();
        dynamicPacketInstance.clear();
        for (SubChunkInstance subChunk : subChunks)
            subChunk.reset();
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

    public ChunkDataSyncContainer getChunkDataSyncContainer() {
        return chunkDataSyncContainer;
    }

    public SubChunkInstance[] getSubChunks() {
        return subChunks;
    }

    public SubChunkInstance getSubChunk(int subChunkCoordinate) {
        return subChunks[subChunkCoordinate];
    }

    public ChunkNeighborStruct getChunkNeighbors() {
        return chunkNeighbors;
    }
}