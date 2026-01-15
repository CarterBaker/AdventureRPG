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
        for (int i = 0; i < Direction2Vector.LENGTH; i++) {

            long neighborCoordinate = Coordinate2Long.getNeighbor(
                    chunkCoordinate,
                    Direction2Vector.VALUES[i]);

            neighborCoordinates[i] = WorldWrapUtility.wrapAroundWorld(
                    chunkInstance.getWorldHandle(),
                    neighborCoordinate);
        }
    }

    // Accessible \\

    public long getNeighborCoordinate(int i) {
        return neighborCoordinates[i];
    }

    public ChunkInstance getNeighborChunk(int i) {
        return neighborChunks[i];
    }

    public void setNeighborChunk(int i, ChunkInstance chunkInstance) {
        neighborChunks[i] = chunkInstance;
    }
}
