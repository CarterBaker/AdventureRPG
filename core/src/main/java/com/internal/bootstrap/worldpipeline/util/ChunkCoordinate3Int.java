package com.internal.bootstrap.worldpipeline.util;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector;
import com.internal.core.engine.UtilityPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

public class ChunkCoordinate3Int extends UtilityPackage {

    // Internal \\

    private ChunkCoordinate3Int() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Chunk dimensions
    private static final int CHUNK_SIZE;
    public static final short BLOCK_COORDINATE_COUNT;

    // Precomputed flattened coordinates packed into shorts
    // Format: yyyy zzzz xxxx (4 bits each, 12 bits total per coordinate)
    // Y-Z-X order matches array indexing for cache coherency
    private static final int[] blockCoordinates;

    static {

        // Load settings
        CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        BLOCK_COORDINATE_COUNT = (short) (CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE);

        // Allocate coordinate arrays
        blockCoordinates = new int[BLOCK_COORDINATE_COUNT];

        // Precompute all coordinates
        flattenBlockCoordinates();
    }

    private static void flattenBlockCoordinates() {

        short idx = 0;

        // Iterate in Y-Z-X order to match BlockPaletteHandle's getIndex() ordering
        for (byte y = 0; y < CHUNK_SIZE; y++)
            for (byte z = 0; z < CHUNK_SIZE; z++)
                for (byte x = 0; x < CHUNK_SIZE; x++)
                    blockCoordinates[idx++] = Coordinate3Int.pack(x, y, z);
    }

    // Accessible \\

    public static int[] getBlockCoordinates() {
        return blockCoordinates;
    }

    public static int getBlockCoordinate(int index) {
        return blockCoordinates[index];
    }

    // Convert a vert coordinate to block space
    public static int convertToBlockSpace(int vertPacked, VertBlockNeighbor3Vector direction) {

        int vx = (direction.vertOffset3Int & 0x3FF);
        int vy = ((direction.vertOffset3Int >> 20) & 0x3FF);
        int vz = ((direction.vertOffset3Int >> 10) & 0x3FF);

        int x = Coordinate3Int.unpackX(vertPacked) - vx;
        int y = Coordinate3Int.unpackY(vertPacked) - vy;
        int z = Coordinate3Int.unpackZ(vertPacked) - vz;

        return Coordinate3Int.pack(x, y, z);
    }

    // Get the neighbor next to a coordinate using the `Direction3Vector`
    public static int getNeighbor(int packed, Direction3Vector direction) {

        int x = Coordinate3Int.unpackX(packed) + direction.x;
        int y = Coordinate3Int.unpackY(packed) + direction.y;
        int z = Coordinate3Int.unpackZ(packed) + direction.z;

        return Coordinate3Int.pack(x, y, z);
    }

    // Get the neighbor next to a coordinate and wrap using the `Direction3Vector`
    public static int getNeighborAndWrap(int packed, Direction3Vector direction) {

        int mask = CHUNK_SIZE - 1;
        int x = ((Coordinate3Int.unpackX(packed) + direction.x) & mask);
        int y = ((Coordinate3Int.unpackY(packed) + direction.y) & mask);
        int z = ((Coordinate3Int.unpackZ(packed) + direction.z) & mask);

        return Coordinate3Int.pack(x, y, z);
    }

    // Get the neighbor next to a coordinate and along a tangent
    public static int getNeighborWithOffset(int packed, Direction3Vector tangent, int offset) {

        int x = Coordinate3Int.unpackX(packed) + (tangent.x * offset);
        int y = Coordinate3Int.unpackY(packed) + (tangent.y * offset);
        int z = Coordinate3Int.unpackZ(packed) + (tangent.z * offset);

        return Coordinate3Int.pack(x, y, z);
    }

    // Check if the x, y or z component is 0 or (`CHUNK_SIZE` - 1)
    public static boolean isAtEdge(int packed) {

        int max = CHUNK_SIZE - 1;
        int x = Coordinate3Int.unpackX(packed);
        int y = Coordinate3Int.unpackY(packed);
        int z = Coordinate3Int.unpackZ(packed);

        return (x == 0 || x == max ||
                y == 0 || y == max ||
                z == 0 || z == max);
    }

    // Convert a block coordinate to vert space
    public static int convertToVertSpace(int packed, Direction3Vector direction) {

        int x = Coordinate3Int.unpackX(packed) + (direction.x > 0 ? 1 : 0);
        int y = Coordinate3Int.unpackY(packed) + (direction.y > 0 ? 1 : 0);
        int z = Coordinate3Int.unpackZ(packed) + (direction.z > 0 ? 1 : 0);

        return Coordinate3Int.pack(x, y, z);
    }

    // Get the neighbor next to a coordinate using the `VertNeighbor3Vector`
    public static int getNeighborFromVert(int vertPacked, VertBlockNeighbor3Vector neighbor) {

        int x = Coordinate3Int.unpackX(vertPacked) + neighbor.x;
        int y = Coordinate3Int.unpackY(vertPacked) + neighbor.y;
        int z = Coordinate3Int.unpackZ(vertPacked) + neighbor.z;

        return Coordinate3Int.pack(x, y, z);
    }

    // Get a vert coordinate from a packed vert position
    public static int getVertCoordinateFromOffset(int vertPacked, Direction3Vector direction, int offset) {

        int x = Coordinate3Int.unpackX(vertPacked) + (direction.x * offset);
        int y = Coordinate3Int.unpackY(vertPacked) + (direction.y * offset);
        int z = Coordinate3Int.unpackZ(vertPacked) + (direction.z * offset);

        return Coordinate3Int.pack(x, y, z);
    }
}
