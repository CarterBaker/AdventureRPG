package com.internal.core.util.mathematics.Extras;

import com.internal.core.engine.UtilityPackage;
import com.internal.core.engine.settings.EngineSetting;

public final class Coordinate3Short extends UtilityPackage {

    // Internal \\
    private Coordinate3Short() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Chunk dimensions
    private static final int BIOME_SIZE;
    private static final int CHUNK_SIZE;
    public static final short BIOME_COORDINATE_COUNT;
    public static final short BLOCK_COORDINATE_COUNT;

    // Precomputed flattened coordinates packed into shorts
    // Format: xxxx yyyy zzzz (4 bits each, 12 bits total per coordinate)
    private static final short[] biomeCoordinates;
    private static final short[] blockCoordinates;

    static {
        // Load settings
        BIOME_SIZE = EngineSetting.BIOME_SIZE;
        CHUNK_SIZE = EngineSetting.CHUNK_SIZE;

        int biomeAxis = CHUNK_SIZE / BIOME_SIZE;
        BIOME_COORDINATE_COUNT = (short) (biomeAxis * biomeAxis * biomeAxis);
        BLOCK_COORDINATE_COUNT = (short) (CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE);

        // Allocate coordinate arrays
        biomeCoordinates = new short[BIOME_COORDINATE_COUNT];
        blockCoordinates = new short[BLOCK_COORDINATE_COUNT];

        // Precompute all coordinates
        flattenBiomeCoordinates(biomeAxis);
        flattenBlockCoordinates();
    }

    private static void flattenBiomeCoordinates(int biomeAxis) {
        int idx = 0;
        for (int y = 0; y < biomeAxis; y++) {
            for (int z = 0; z < biomeAxis; z++) {
                for (int x = 0; x < biomeAxis; x++) {
                    biomeCoordinates[idx++] = pack(x, y, z);
                }
            }
        }
    }

    private static void flattenBlockCoordinates() {
        int idx = 0;
        // Iterate in Y-Z-X order to match BlockPaletteHandle's getIndex() ordering
        for (int y = 0; y < CHUNK_SIZE; y++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    blockCoordinates[idx++] = pack(x, y, z);
                }
            }
        }
    }

    // Accessible \\

    // Pack x, y, z into a single short (4 bits each)
    public static short pack(int x, int y, int z) {
        return (short) ((x << 8) | (y << 4) | z);
    }

    // Public API for biome coordinates
    public static short[] getBiomeCoordinates() {
        return biomeCoordinates;
    }

    public static short getBiomeCoordinate(int index) {
        return biomeCoordinates[index];
    }

    // Public API for block coordinates
    public static short[] getBlockCoordinates() {
        return blockCoordinates;
    }

    public static short getBlockCoordinate(int index) {
        return blockCoordinates[index];
    }

    // Direct unpacking from packed coordinate
    public static int unpackX(short packed) {
        return (packed >> 8) & 0xF;
    }

    public static int unpackY(short packed) {
        return (packed >> 4) & 0xF;
    }

    public static int unpackZ(short packed) {
        return packed & 0xF;
    }

    // Get neighbor without wrapping, returns -1 if out of bounds
    public static short getNeighbor(short packed, int dx, int dy, int dz) {
        // Extract and add in one step
        int x = ((packed >> 8) & 0xF) + dx;
        int y = ((packed >> 4) & 0xF) + dy;
        int z = (packed & 0xF) + dz;

        // Check if any coordinate is outside [0, 15] using bitwise OR
        // If any value has bits set beyond bit 4, it's out of bounds
        if (((x | y | z) & ~0xF) != 0) {
            return -1;
        }

        return (short) ((x << 8) | (y << 4) | z);
    }

    // Direct bit addition with Direction3Vector, no wrap
    public static short getNeighbor(short packed, Direction3Vector direction3Int) {
        short directionPacked = direction3Int.coordinate3Short;

        int x = ((packed >> 8) & 0xF) + ((directionPacked >> 8) & 0xF);
        int y = ((packed >> 4) & 0xF) + ((directionPacked >> 4) & 0xF);
        int z = (packed & 0xF) + (directionPacked & 0xF);

        // Single bitwise check for all coordinates
        if (((x | y | z) & ~0xF) != 0) {
            return -1;
        }

        return (short) ((x << 8) | (y << 4) | z);
    }

    // Direct bit addition with Direction2Vector, no wrap
    public static short getNeighbor(short packed, Direction2Vector direction2Int) {
        short directionPacked = direction2Int.coordinate3Short;

        int x = ((packed >> 8) & 0xF) + ((directionPacked >> 8) & 0xF);
        int y = (packed >> 4) & 0xF; // Y unchanged
        int z = (packed & 0xF) + (directionPacked & 0xF);

        // Check only X and Z (Y is unchanged so can't be out of bounds)
        if (((x | z) & ~0xF) != 0) {
            return -1;
        }

        return (short) ((x << 8) | (y << 4) | z);
    }

    // Neighbor lookup with wrapping (optimized bit manipulation)
    public static short getNeighborAndWrap(short packed, int dx, int dy, int dz) {
        // Extract, add, and wrap in minimal operations
        int x = ((packed >> 8) + dx) & 0xF;
        int y = ((packed >> 4) + dy) & 0xF;
        int z = (packed + dz) & 0xF;
        return (short) ((x << 8) | (y << 4) | z);
    }

    // FAST: Direct bit addition with Direction3Int.chunkCoordinate3Short
    public static short getNeighborAndWrap(short packed, Direction3Vector direction3Int) {

        short directionPacked = direction3Int.coordinate3Short;

        // Add each component directly, then mask to wrap
        int x = ((packed >> 8) + (directionPacked >> 8)) & 0xF;
        int y = ((packed >> 4) + (directionPacked >> 4)) & 0xF;
        int z = (packed + directionPacked) & 0xF;
        return (short) ((x << 8) | (y << 4) | z);
    }

    // FAST: Direct bit addition with Direction2Int.chunkCoordinate3Short
    public static short getNeighborAndWrap(short packed, Direction2Vector direction2Int) {

        short directionPacked = direction2Int.coordinate3Short;

        // Add X and Z components, Y stays the same
        int x = ((packed >> 8) + (directionPacked >> 8)) & 0xF;
        int y = (packed >> 4) & 0xF; // Y unchanged
        int z = (packed + directionPacked) & 0xF;
        return (short) ((x << 8) | (y << 4) | z);
    }

    // Get neighbor with scaled direction (for greedy meshing tangent checks)
    public static short getNeighborWithOffset(short packed, Direction3Vector direction, int offset) {
        // Extract base coordinates
        int x = ((packed >> 8) & 0xF) + ((direction.coordinate3Short >> 8) & 0xF) * offset;
        int y = ((packed >> 4) & 0xF) + ((direction.coordinate3Short >> 4) & 0xF) * offset;
        int z = (packed & 0xF) + (direction.coordinate3Short & 0xF) * offset;

        // Check if any coordinate is outside [0, 15]
        if (((x | y | z) & ~0xF) != 0) {
            return -1;
        }

        return (short) ((x << 8) | (y << 4) | z);
    }

    // 2D version (Y unchanged)
    public static short getNeighborWithOffset(short packed, Direction2Vector direction, int offset) {
        int x = ((packed >> 8) & 0xF) + ((direction.coordinate3Short >> 8) & 0xF) * offset;
        int y = (packed >> 4) & 0xF; // Y unchanged
        int z = (packed & 0xF) + (direction.coordinate3Short & 0xF) * offset;

        // Check only X and Z
        if (((x | z) & ~0xF) != 0) {
            return -1;
        }

        return (short) ((x << 8) | (y << 4) | z);
    }

    // Get the flattened index from a packed coordinate
    public static int getBlockIndex(short packed) {
        int x = unpackX(packed);
        int y = unpackY(packed);
        int z = unpackZ(packed);
        // Matches BlockPaletteHandle's getIndex: (y * size + z) * size + x
        return (y * CHUNK_SIZE + z) * CHUNK_SIZE + x;
    }

    // Get the flattened index from x, y, z
    public static int getBlockIndex(int x, int y, int z) {
        return (y * CHUNK_SIZE + z) * CHUNK_SIZE + x;
    }

    // Check if any axis is 0 or CHUNK_SIZE
    public static boolean isAtEdge(short packed) {
        int x = (packed >> 8) & 0xF;
        int y = (packed >> 4) & 0xF;
        int z = packed & 0xF;

        return (x == 0 || x == 15 ||
                y == 0 || y == 15 ||
                z == 0 || z == 15);
    }

    // Convert block coordinate to vert space using direction's pre-packed offset
    public static short convertToVertSpace(short blockXYZ, Direction3Vector direction) {
        int x = ((blockXYZ >> 8) + (direction.vertOffset3Short >> 8)) & 0xF;
        int y = ((blockXYZ >> 4) + (direction.vertOffset3Short >> 4)) & 0xF;
        int z = (blockXYZ + direction.vertOffset3Short) & 0xF;
        return (short) ((x << 8) | (y << 4) | z);
    }
}