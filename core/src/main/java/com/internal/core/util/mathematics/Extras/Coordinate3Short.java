package com.internal.core.util.mathematics.Extras;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.BlockDirection3Vector;
import com.internal.core.engine.UtilityPackage;
import com.internal.core.engine.settings.EngineSetting;

public final class Coordinate3Short extends UtilityPackage {

    // Internal \\
    private Coordinate3Short() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Chunk dimensions
    private static final int CHUNK_SIZE;
    public static final short BLOCK_COORDINATE_COUNT;

    // Precomputed flattened coordinates packed into shorts
    // Format: xxxx yyyy zzzz (4 bits each, 12 bits total per coordinate)
    private static final short[] blockCoordinates;

    static {
        // Load settings
        CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        BLOCK_COORDINATE_COUNT = (short) (CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE);

        // Allocate coordinate arrays
        blockCoordinates = new short[BLOCK_COORDINATE_COUNT];

        // Precompute all coordinates
        flattenBlockCoordinates();
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

    // Public API for block coordinates
    public static short[] getBlockCoordinates() {
        return blockCoordinates;
    }

    public static short getBlockCoordinate(int index) {
        return blockCoordinates[index];
    }

    // Direct unpacking from packed coordinate
    public static byte unpackX(short packed) {
        return (byte) ((packed >> 8) & 0xF);
    }

    public static byte unpackY(short packed) {
        return (byte) ((packed >> 4) & 0xF);
    }

    public static byte unpackZ(short packed) {
        return (byte) (packed & 0xF);
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

    // Add this to Coordinate3Short class
    // Get neighbor in VERT SPACE (5 bits per component, range 0-16)
    public static short getNeighborFromVert(short vertPacked, BlockDirection3Vector blockDirection) {
        short directionPacked = blockDirection.coordinate3Short;

        // Extract from VERT SPACE (5 bits), add BLOCK SPACE direction (4 bits, but may
        // be negative like -1=15)
        int x = ((vertPacked >> 10) & 0x1F) + ((directionPacked >> 8) & 0xF);
        int y = ((vertPacked >> 5) & 0x1F) + ((directionPacked >> 4) & 0xF);
        int z = (vertPacked & 0x1F) + (directionPacked & 0xF);

        // Result can be -1 to 17, but we'll handle wrapping in convertToBlockSpace
        // Pack back into vert space (5 bits each)
        return (short) ((x << 10) | (y << 5) | z);
    }

    // Get neighbor with scaled direction in VERT SPACE (for quad vertices)
    public static short getNeighborWithOffsetFromVert(short vertPacked, Direction3Vector direction, int offset) {
        // Extract from VERT SPACE (5 bits per component)
        int x = ((vertPacked >> 10) & 0x1F) + ((direction.coordinate3Short >> 8) & 0xF) * offset;
        int y = ((vertPacked >> 5) & 0x1F) + ((direction.coordinate3Short >> 4) & 0xF) * offset;
        int z = (vertPacked & 0x1F) + (direction.coordinate3Short & 0xF) * offset;

        // Pack back into vert space (5 bits each) - no bounds checking needed for quad
        // vertices
        return (short) ((x << 10) | (y << 5) | z);
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

    // Check if vertex coordinate is at edge (0 or 16 in vert space)
    public static boolean isVertAtEdge(short vertPacked) {
        int x = (vertPacked >> 10) & 0x1F;
        int y = (vertPacked >> 5) & 0x1F;
        int z = vertPacked & 0x1F;

        // Values 0 and 16 both have (val & 0x0F) == 0
        return ((x & 0x0F) == 0) || ((y & 0x0F) == 0) || ((z & 0x0F) == 0);
    }

    // Convert block coordinate to vert space using direction's pre-packed offset
    // vertOffset3Short should contain 0 or 1 for each axis (packed as 4 bits each)
    public static short convertToVertSpace(short blockXYZ, Direction3Vector direction) {
        // Don't mask the result - allow values 0-16
        int x = ((blockXYZ >> 8) & 0xF) + ((direction.vertOffset3Short >> 8) & 0xF);
        int y = ((blockXYZ >> 4) & 0xF) + ((direction.vertOffset3Short >> 4) & 0xF);
        int z = (blockXYZ & 0xF) + (direction.vertOffset3Short & 0xF);
        // Pack with 5 bits per component to hold 0-16
        return (short) ((x << 10) | (y << 5) | z);
    }

    // Convert vert coordinate to block space using direction's pre-packed offset
    public static short convertToBlockSpace(short vertXYZ, Direction3Vector direction) {
        int x = ((vertXYZ >> 10) & 0x1F) - ((direction.vertOffset3Short >> 8) & 0xF);
        int y = ((vertXYZ >> 5) & 0x1F) - ((direction.vertOffset3Short >> 4) & 0xF);
        int z = (vertXYZ & 0x1F) - (direction.vertOffset3Short & 0xF);

        // Wrap: -1 → 15, 16 → 0, otherwise keep value
        x = (x & 0xF);
        y = (y & 0xF);
        z = (z & 0xF);

        return (short) ((x << 8) | (y << 4) | z);
    }

    // Convert vert coordinate to block space using BlockDirection3Vector's
    // pre-packed offset
    public static short convertToBlockSpace(short vertXYZ, BlockDirection3Vector direction) {
        int x = ((vertXYZ >> 10) & 0x1F) - ((direction.vertOffset3Short >> 8) & 0xF);
        int y = ((vertXYZ >> 5) & 0x1F) - ((direction.vertOffset3Short >> 4) & 0xF);
        int z = (vertXYZ & 0x1F) - (direction.vertOffset3Short & 0xF);

        // Wrap: -1 → 15, 16 → 0, otherwise keep value
        x = (x & 0xF);
        y = (y & 0xF);
        z = (z & 0xF);

        return (short) ((x << 8) | (y << 4) | z);
    }
}