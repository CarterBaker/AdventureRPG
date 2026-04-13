package application.bootstrap.worldpipeline.chunk;

import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction2Vector;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkNeighborStruct extends StructPackage {

    /*
     * Pre-computed neighbor coordinates and a shared reference to the active
     * chunk map for fast neighbor lookups during assessment. Coordinates are
     * wrapped at construction time so world boundary handling is free at runtime.
     */

    // Internal
    private final long[] neighborCoordinates;
    private final Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;

    // Constructor \\

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