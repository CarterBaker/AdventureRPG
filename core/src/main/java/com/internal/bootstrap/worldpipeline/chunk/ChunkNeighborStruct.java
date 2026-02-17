package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.core.engine.StructPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Direction2Vector;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkNeighborStruct extends StructPackage {

    // Internal
    private final long[] neighborCoordinates;
    private final Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;

    // Internal \\

    public ChunkNeighborStruct(
            long chunkCoordinate,
            ChunkInstance chunkInstance,
            Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {

        this.neighborCoordinates = new long[Direction2Vector.LENGTH];
        this.activeChunks = activeChunks;

        for (byte i = 0; i < Direction2Vector.LENGTH; i++) {
            long neighborCoordinate = Coordinate2Long.getNeighbor(
                    chunkCoordinate,
                    Direction2Vector.VALUES[i]);
            neighborCoordinates[i] = WorldWrapUtility.wrapAroundWorld(
                    chunkInstance.getWorldHandle(),
                    neighborCoordinate);
        }
    }

    // Accessible \\

    public long getNeighborCoordinate(int direction2VectorIndex) {
        return neighborCoordinates[direction2VectorIndex];
    }

    public ChunkInstance getNeighborChunk(int direction2VectorIndex) {
        return activeChunks.get(neighborCoordinates[direction2VectorIndex]);
    }
}