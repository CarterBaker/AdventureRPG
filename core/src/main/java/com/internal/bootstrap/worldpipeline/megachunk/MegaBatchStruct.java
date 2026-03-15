package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.core.engine.StructPackage;
import com.internal.core.util.mathematics.extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/*
 * Tracks which chunks have been registered and which have been successfully
 * merged into the mega's geometry. mergedCoordinates drives readiness —
 * when its size reaches megaScale all contributions are present and the
 * mega is ready for GPU upload. On re-merge, mergedCoordinates is cleared
 * and rebuilt so the count stays accurate after a full geometry rebuild.
 */
public class MegaBatchStruct extends StructPackage {

    // Internal
    private int megaScale;
    private long megaChunkCoordinate;
    private final Long2ObjectOpenHashMap<ChunkInstance> batchedChunks;
    private final LongOpenHashSet mergedCoordinates;
    // Internal \\

    public MegaBatchStruct(long megaChunkCoordinate, int megaScale) {
        this.megaScale = megaScale;
        this.megaChunkCoordinate = megaChunkCoordinate;
        this.batchedChunks = new Long2ObjectOpenHashMap<>();
        this.mergedCoordinates = new LongOpenHashSet();
    }

    public void constructor(long megaChunkCoordinate, int megaScale) {
        this.megaChunkCoordinate = megaChunkCoordinate;
        this.megaScale = megaScale;
        this.batchedChunks.clear();
        this.mergedCoordinates.clear();
    }

    public void reset() {
        batchedChunks.clear();
        mergedCoordinates.clear();
    }

    // Accessible \\

    public boolean isReadyToRender() {
        return mergedCoordinates.size() == megaScale;
    }

    public boolean isEmpty() {
        return batchedChunks.isEmpty();
    }

    public long getMegaChunkCoordinate() {
        return megaChunkCoordinate;
    }

    public boolean registerChunk(ChunkInstance chunkInstance) {
        if (Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate()) != megaChunkCoordinate)
            return false;
        batchedChunks.put(chunkInstance.getCoordinate(), chunkInstance);
        return true;
    }

    public void recordMerged(long coordinate) {
        mergedCoordinates.add(coordinate);
    }

    public void clearMerged() {
        mergedCoordinates.clear();
    }

    public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks() {
        return batchedChunks;
    }

    public ChunkInstance getBatchedChunk(long chunkCoordinate) {
        return batchedChunks.get(chunkCoordinate);
    }
}