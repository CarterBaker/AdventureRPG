package com.internal.core.util.mathematics.Extras;

import com.internal.core.engine.settings.EngineSetting;

public final class Coordinate2Long {
    private static final int MEGA_SHIFT = Integer.numberOfTrailingZeros(EngineSetting.MEGA_CHUNK_SIZE);

    // Internal \\
    public static long pack(int x, int y) {
        // High 32 bits get x, low 32 bits get y
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public static int unpackX(long value) {
        // Arithmetic right shift preserves sign
        return (int) (value >> 32);
    }

    public static int unpackY(long value) {
        // Cast to int automatically takes low 32 bits and preserves sign
        return (int) value;
    }

    public static long getNeighbor(long packed, Direction2Vector direction) {
        return add(packed, direction.coordinate2Long);
    }

    public static long getNeighbor(long packed, Direction3Vector direction) {
        return add(packed, direction.coordinate2Long);
    }

    // Addition \\
    public static long add(long a, long b) {

        int aX = unpackX(a);
        int aY = unpackY(a);
        int bX = unpackX(b);
        int bY = unpackY(b);

        return pack(aX + bX, aY + bY);
    }

    // Utility \\

    public static long toMegaChunkCoordinate(long packedChunk) {

        int chunkX = unpackX(packedChunk);
        int chunkY = unpackY(packedChunk);

        int megaSize = 1 << MEGA_SHIFT;
        int megaX = Math.floorDiv(chunkX, megaSize) * megaSize;
        int megaY = Math.floorDiv(chunkY, megaSize) * megaSize;

        return pack(megaX, megaY);
    }

    // Java Utility \\
    public static String toString(long value) {
        return "Coordinate2Long(" +
                unpackX(value) + ", " +
                unpackY(value) + ")";
    }
}