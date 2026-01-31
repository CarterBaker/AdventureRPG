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
    // Format: yyyy zzzz xxxx (4 bits each, 12 bits total per coordinate)
    // Y-Z-X order matches array indexing for cache coherency
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

    // Pack y, z, x into a single short (4 bits each) - Y-Z-X order
    public static short pack(int x, int y, int z) {
        return (short) ((y << 8) | (z << 4) | x);
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
        return (byte) (packed & 0xF);
    }

    public static byte unpackY(short packed) {
        return (byte) ((packed >> 8) & 0xF);
    }

    public static byte unpackZ(short packed) {
        return (byte) ((packed >> 4) & 0xF);
    }

    // Get neighbor without wrapping, returns -1 if out of bounds
    public static short getNeighbor(short packed, int dx, int dy, int dz) {
        int x = (packed & 0xF) + dx;
        int y = ((packed >> 8) & 0xF) + dy;
        int z = ((packed >> 4) & 0xF) + dz;

        if (((x | y | z) & ~0xF) != 0)
            return -1;

        return (short) ((y << 8) | (z << 4) | x);
    }

    // Direct bit addition with Direction3Vector, no wrap
    public static short getNeighbor(short packed, Direction3Vector direction) {
        int x = (packed & 0xF) + direction.x;
        int y = ((packed >> 8) & 0xF) + direction.y;
        int z = ((packed >> 4) & 0xF) + direction.z;

        if (((x | y | z) & ~0xF) != 0)
            return -1;

        return (short) ((y << 8) | (z << 4) | x);
    }

    // Direct bit addition with Direction2Vector, no wrap
    public static short getNeighbor(short packed, Direction2Vector direction) {
        int x = (packed & 0xF) + direction.x;
        int y = (packed >> 8) & 0xF;
        int z = ((packed >> 4) & 0xF) + direction.y;

        if (((x | z) & ~0xF) != 0)
            return -1;

        return (short) ((y << 8) | (z << 4) | x);
    }

    // Neighbor lookup with wrapping (optimized bit manipulation)
    public static short getNeighborAndWrap(short packed, int dx, int dy, int dz) {
        int x = (packed + dx) & 0xF;
        int y = ((packed >> 8) + dy) & 0xF;
        int z = ((packed >> 4) + dz) & 0xF;
        return (short) ((y << 8) | (z << 4) | x);
    }

    // FAST: Direct bit addition with Direction3Vector wrapping
    public static short getNeighborAndWrap(short packed, Direction3Vector direction) {
        int x = ((packed & 0xF) + direction.x) & 0xF;
        int y = (((packed >> 8) & 0xF) + direction.y) & 0xF;
        int z = (((packed >> 4) & 0xF) + direction.z) & 0xF;
        return (short) ((y << 8) | (z << 4) | x);
    }

    // FAST: Direct bit addition with Direction2Vector wrapping
    public static short getNeighborAndWrap(short packed, Direction2Vector direction) {
        int x = ((packed & 0xF) + direction.x) & 0xF;
        int y = (packed >> 8) & 0xF;
        int z = (((packed >> 4) & 0xF) + direction.y) & 0xF;
        return (short) ((y << 8) | (z << 4) | x);
    }

    // Get neighbor with scaled direction (for greedy meshing tangent checks)
    public static short getNeighborWithOffset(short packed, Direction3Vector direction, int offset) {
        int x = (packed & 0xF) + direction.x * offset;
        int y = ((packed >> 8) & 0xF) + direction.y * offset;
        int z = ((packed >> 4) & 0xF) + direction.z * offset;

        if (((x | y | z) & ~0xF) != 0)
            return -1;

        return (short) ((y << 8) | (z << 4) | x);
    }

    // 2D version (Y unchanged)
    public static short getNeighborWithOffset(short packed, Direction2Vector direction, int offset) {
        int x = (packed & 0xF) + direction.x * offset;
        int y = (packed >> 8) & 0xF;
        int z = ((packed >> 4) & 0xF) + direction.y * offset;

        if (((x | z) & ~0xF) != 0)
            return -1;

        return (short) ((y << 8) | (z << 4) | x);
    }

    // Get neighbor in VERT SPACE (5 bits per component, range 0-16)
    public static short getNeighborFromVert(short vertPacked, BlockDirection3Vector blockDirection) {
        int x = (vertPacked & 0x1F) + blockDirection.x;
        int y = ((vertPacked >> 10) & 0x1F) + blockDirection.y;
        int z = ((vertPacked >> 5) & 0x1F) + blockDirection.z;

        return (short) ((y << 10) | (z << 5) | x);
    }

    // Get neighbor with scaled direction in VERT SPACE (for quad vertices)
    public static short getNeighborWithOffsetFromVert(short vertPacked, Direction3Vector direction, int offset) {
        int x = (vertPacked & 0x1F) + direction.x * offset;
        int y = ((vertPacked >> 10) & 0x1F) + direction.y * offset;
        int z = ((vertPacked >> 5) & 0x1F) + direction.z * offset;

        return (short) ((y << 10) | (z << 5) | x);
    }

    // Get the flattened index from a packed coordinate
    public static int getBlockIndex(short packed) {
        int x = packed & 0xF;
        int y = (packed >> 8) & 0xF;
        int z = (packed >> 4) & 0xF;
        return (y * CHUNK_SIZE + z) * CHUNK_SIZE + x;
    }

    // Get the flattened index from x, y, z
    public static int getBlockIndex(int x, int y, int z) {
        return (y * CHUNK_SIZE + z) * CHUNK_SIZE + x;
    }

    // Check if any axis is 0 or CHUNK_SIZE
    public static boolean isAtEdge(short packed) {
        int x = packed & 0xF;
        int y = (packed >> 8) & 0xF;
        int z = (packed >> 4) & 0xF;

        return (x == 0 || x == 15 ||
                y == 0 || y == 15 ||
                z == 0 || z == 15);
    }

    // Check if vertex coordinate is at edge (0 or 16 in vert space)
    public static boolean isVertAtEdge(short vertPacked) {
        int x = vertPacked & 0x1F;
        int y = (vertPacked >> 10) & 0x1F;
        int z = (vertPacked >> 5) & 0x1F;

        return ((x & 0x0F) == 0) || ((y & 0x0F) == 0) || ((z & 0x0F) == 0);
    }

    // Convert block coordinate to vert space using direction's pre-packed offset
    public static short convertToVertSpace(short blockXYZ, Direction3Vector direction) {
        int x = (blockXYZ & 0xF) + (direction.vertOffset3Short & 0xF);
        int y = ((blockXYZ >> 8) & 0xF) + ((direction.vertOffset3Short >> 8) & 0xF);
        int z = ((blockXYZ >> 4) & 0xF) + ((direction.vertOffset3Short >> 4) & 0xF);
        return (short) ((y << 10) | (z << 5) | x);
    }

    // Convert vert coordinate to block space using direction's pre-packed offset
    public static short convertToBlockSpace(short vertXYZ, Direction3Vector direction) {
        int x = (vertXYZ & 0x1F) - (direction.vertOffset3Short & 0xF);
        int y = ((vertXYZ >> 10) & 0x1F) - ((direction.vertOffset3Short >> 8) & 0xF);
        int z = ((vertXYZ >> 5) & 0x1F) - ((direction.vertOffset3Short >> 4) & 0xF);

        x = (x & 0xF);
        y = (y & 0xF);
        z = (z & 0xF);

        return (short) ((y << 8) | (z << 4) | x);
    }

    // Convert vert coordinate to block space using BlockDirection3Vector's
    // pre-packed offset
    public static short convertToBlockSpace(short vertXYZ, BlockDirection3Vector direction) {
        int x = (vertXYZ & 0x1F) - (direction.vertOffset3Short & 0xF);
        int y = ((vertXYZ >> 10) & 0x1F) - ((direction.vertOffset3Short >> 8) & 0xF);
        int z = ((vertXYZ >> 5) & 0x1F) - ((direction.vertOffset3Short >> 4) & 0xF);

        x = (x & 0xF);
        y = (y & 0xF);
        z = (z & 0xF);

        return (short) ((y << 8) | (z << 4) | x);
    }
}