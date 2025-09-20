package com.AdventureRPG.Util;

public final class Coordinate3Int {

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

    private Coordinate3Int() {
        // Prevent instantiation
    }

    // Pack coordinates into a single long
    public static long pack(int x, int y, int z) {
        return ((long) x & X_MASK) << X_SHIFT |
                ((long) y & Y_MASK) << Y_SHIFT |
                ((long) z & Z_MASK);
    }

    // Unpack X from packed value
    public static int unpackX(long value) {
        return (int) ((value >>> X_SHIFT) & X_MASK);
    }

    // Unpack Y from packed value
    public static int unpackY(long value) {
        return (int) ((value >>> Y_SHIFT) & Y_MASK);
    }

    // Unpack Z from packed value
    public static int unpackZ(long value) {
        return (int) (value & Z_MASK); // Z is at the bottom
    }

    // Arithmetic

    public static long add(long a, long b) {
        return pack(
                unpackX(a) + unpackX(b),
                unpackY(a) + unpackY(b),
                unpackZ(a) + unpackZ(b));
    }

    public static long subtract(long a, long b) {
        return pack(
                unpackX(a) - unpackX(b),
                unpackY(a) - unpackY(b),
                unpackZ(a) - unpackZ(b));
    }

    public static long multiply(long a, long b) {
        return pack(
                unpackX(a) * unpackX(b),
                unpackY(a) * unpackY(b),
                unpackZ(a) * unpackZ(b));
    }

    public static long divide(long a, long b) {
        return pack(
                unpackX(a) / unpackX(b),
                unpackY(a) / unpackY(b),
                unpackZ(a) / unpackZ(b));
    }

    // Utility

    public static String toString(long value) {
        return "Coordinate3Int(" + unpackX(value) + ", " +
                unpackY(value) + ", " +
                unpackZ(value) + ")";
    }
}
