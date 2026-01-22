package com.internal.core.util.mathematics.Extras;

public final class Coordinate3Long {

    // Bit sizes (must sum to 64)
    private static final int X_BITS = 26;
    private static final int Y_BITS = 12;
    private static final int Z_BITS = 26;

    // Bit shifts (Z is lowest, then Y, then X)
    private static final int Y_SHIFT = Z_BITS;
    private static final int X_SHIFT = Y_BITS + Z_BITS;

    // Masks (unsigned bitmasks for packing)
    private static final long X_MASK = (1L << X_BITS) - 1;
    private static final long Y_MASK = (1L << Y_BITS) - 1;
    private static final long Z_MASK = (1L << Z_BITS) - 1;

    // Internal \\

    public static long pack(int x, int y, int z) {
        return ((long) x & X_MASK) << X_SHIFT |
                ((long) y & Y_MASK) << Y_SHIFT |
                ((long) z & Z_MASK);
    }

    public static int unpackX(long value) {
        return (int) ((value >>> X_SHIFT) & X_MASK);
    }

    public static int unpackY(long value) {
        return (int) ((value >>> Y_SHIFT) & Y_MASK);
    }

    public static int unpackZ(long value) {
        return (int) (value & Z_MASK);
    }

    // utility \\

    public static long getNeighbor(long packed, Direction2Vector direction) {
        return packed + direction.coordinate3Long;
    }

    public static long getNeighbor(long packed, Direction3Vector direction) {
        return packed + direction.coordinate3Long;
    }

    // Java Utility \\

    public static String toString(long value) {
        return "Coordinate3Int(" + unpackX(value) + ", " +
                unpackY(value) + ", " +
                unpackZ(value) + ")";
    }
}
