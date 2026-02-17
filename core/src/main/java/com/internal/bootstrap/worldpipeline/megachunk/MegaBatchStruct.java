package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.core.engine.StructPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class MegaBatchStruct extends StructPackage {

    // Internal
    private int megaScale;
    private long megaChunkCoordinate;
    private final Long2ObjectOpenHashMap<ChunkInstance> batchedChunks;
    private boolean isComplete;

    // Internal \\

    public MegaBatchStruct(
            long megaChunkCoordinate,
            int megaScale) {

        this.megaScale = megaScale;
        this.megaChunkCoordinate = megaChunkCoordinate;
        this.batchedChunks = new Long2ObjectOpenHashMap<>();
    }

    public void constructor(long megaChunkCoordinate, int megaScale) {

        this.megaChunkCoordinate = megaChunkCoordinate;
        this.megaScale = megaScale;
        this.batchedChunks.clear();
        this.isComplete = false;
    }

    public void reset() {

        batchedChunks.clear();
        isComplete = false;
    }

    // Accessible \\

    public boolean isComplete() {
        return isComplete;
    }

    public long getMegaChunkCoordinate() {
        return megaChunkCoordinate;
    }

    public boolean batchChunk(ChunkInstance chunkInstance) {

        if (Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate()) != megaChunkCoordinate)
            return false;

        batchedChunks.put(chunkInstance.getCoordinate(), chunkInstance);
        isComplete = batchedChunks.size() == megaScale;
        return true;
    }

    public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks() {
        return batchedChunks;
    }

    public ChunkInstance getbatchedChunk(long chunkCoordinate) {
        return batchedChunks.get(chunkCoordinate);
    }
}