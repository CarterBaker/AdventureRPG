package com.internal.core.util.mathematics.Extras;

import com.internal.core.engine.settings.EngineSetting;

public final class Coordinate2Long {

    private static final int MEGA_SHIFT = Integer.numberOfTrailingZeros(EngineSetting.MEGA_CHUNK_SIZE);

    // Internal \\

    public static long pack(int x, int y) {
        return (((long) x) & 0xFFFFFFFFL) << 32 | (((long) y) & 0xFFFFFFFFL);
    }

    public static int unpackX(long value) {
        return (int) (value >>> 32);
    }

    public static int unpackY(long value) {
        return (int) value;
    }

    public static long getNeighbor(long packed, Direction2Vector direction) {
        return packed + direction.coordinate2Long;
    }

    public static long getNeighbor(long packed, Direction3Vector direction) {
        return packed + direction.coordinate2Long;
    }

    // Utility \\

    public static long toMegaChunkCoordinate(long packedChunk) {
        int chunkX = (int) (packedChunk >>> 32);
        int chunkY = (int) packedChunk;

        // Clear the lower MEGA_SHIFT bits to round down to nearest multiple
        int megaX = chunkX & ~((1 << MEGA_SHIFT) - 1);
        int megaY = chunkY & ~((1 << MEGA_SHIFT) - 1);

        return pack(megaX, megaY);
    }

    // Java Utility \\

    public static String toString(long value) {
        return "Coordinate2Int(" + unpackX(value) + ", " + unpackY(value) + ")";
    }
}
