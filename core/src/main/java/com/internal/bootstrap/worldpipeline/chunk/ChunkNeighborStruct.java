package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.core.engine.StructPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Direction2Vector;

public class ChunkNeighborStruct extends StructPackage {

    // Internal
    private final long[] neighborCoordinates;
    private final ChunkInstance[] neighborChunks;

    // Internal \\

    public ChunkNeighborStruct() {

        // Internal
        this.neighborCoordinates = new long[Direction2Vector.LENGTH];
        this.neighborChunks = new ChunkInstance[Direction2Vector.LENGTH];
    }

    public void constructor(
            long chunkCoordinate,
            ChunkInstance chunkInstance) {

        // Internal
        for (byte direction2VectorIndex = 0; direction2VectorIndex < Direction2Vector.LENGTH; direction2VectorIndex++) {

            long neighborCoordinate = Coordinate2Long.getNeighbor(
                    chunkCoordinate,
                    Direction2Vector.VALUES[direction2VectorIndex]);

            neighborCoordinates[direction2VectorIndex] = WorldWrapUtility.wrapAroundWorld(
                    chunkInstance.getWorldHandle(),
                    neighborCoordinate);
        }
    }

    // Accessible \\

    public long getNeighborCoordinate(int direction2VectorIndex) {
        return neighborCoordinates[direction2VectorIndex];
    }

    public ChunkInstance getNeighborChunk(int direction2VectorIndex) {
        return neighborChunks[direction2VectorIndex];
    }

    public void setNeighborChunk(int direction2VectorIndex, ChunkInstance chunkInstance) {
        neighborChunks[direction2VectorIndex] = chunkInstance;
    }
}
