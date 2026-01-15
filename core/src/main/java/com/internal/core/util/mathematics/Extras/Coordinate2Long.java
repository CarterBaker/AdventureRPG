package com.internal.core.util.mathematics.Extras;

public final class Coordinate2Long {

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

    public static String toString(long value) {
        return "Coordinate2Int(" + unpackX(value) + ", " + unpackY(value) + ")";
    }
}
