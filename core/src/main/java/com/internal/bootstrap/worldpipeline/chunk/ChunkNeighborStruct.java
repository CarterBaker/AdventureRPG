package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.core.engine.StructPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Int;
import com.internal.core.util.mathematics.Extras.Direction2Int;

public class ChunkNeighborStruct extends StructPackage {

    // Internal
    private final long[] neighborCoordinates;
    private final ChunkInstance[] neighborChunks;

    // Internal \\

    public ChunkNeighborStruct() {

        // Internal
        this.neighborCoordinates = new long[Direction2Int.LENGTH];
        this.neighborChunks = new ChunkInstance[Direction2Int.LENGTH];
    }

    public void constructor(long chunkCoordinate) {

        // Internal
        int aX = Coordinate2Int.unpackX(chunkCoordinate);
        int aY = Coordinate2Int.unpackY(chunkCoordinate);

        for (int i = 0; i < Direction2Int.LENGTH; i++) {

            Direction2Int direction = Direction2Int.getDirection(i);

            int bX = direction.x;
            int bY = direction.y;

            neighborCoordinates[i] = Coordinate2Int.pack(aX + bX, aY + bY);
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
