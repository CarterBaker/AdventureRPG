package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class MegaChunkInstance extends WorldRenderInstance {

    // Internal
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> chunks;
    private int MEGA_CHUNK_SCALE;

    @Override
    protected void create() {
        super.create();

        chunks = new Long2ObjectLinkedOpenHashMap<>();
    }

    @Override
    public void constructor(
            WorldRenderSystem worldRenderSystem,
            WorldHandle worldHandle,
            long megaChunkCoordinate,
            VAOHandle vaoHandle) {

        super.constructor(
                worldRenderSystem,
                worldHandle,
                megaChunkCoordinate,
                vaoHandle);

        // Internal
        int MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
        this.MEGA_CHUNK_SCALE = MEGA_CHUNK_SIZE * MEGA_CHUNK_SIZE;
    }

    // Utility \\

    public boolean addChunkInstance(ChunkInstance chunkInstance) {

        long chunkCoordinate = chunkInstance.getCoordinate();
        if (Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate) != coordinate)
            return false;

        chunks.put(chunkCoordinate, chunkInstance);

        return (chunks.size() == MEGA_CHUNK_SCALE);
    }

    public void merge() {

        dynamicPacketInstance.clear();

        for (ChunkInstance chunkInstance : chunks.values())
            dynamicPacketInstance.merge(chunkInstance.getDynamicPacketInstance());
    }
}
