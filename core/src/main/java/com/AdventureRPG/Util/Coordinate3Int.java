package com.AdventureRPG.Util;

public final class Coordinate3Int {

    // Bit sizes (must sum to <= 64)
    private static final int X_BITS = 24;
    private static final int Y_BITS = 16;
    private static final int Z_BITS = 24;

    // Bit shifts for X → Y → Z ordering
    private static final int Y_SHIFT = Z_BITS;
    private static final int X_SHIFT = Y_BITS + Z_BITS;

    // Masks
    private static final long X_MASK = (1L << X_BITS) - 1;
    private static final long Y_MASK = (1L << Y_BITS) - 1;
    private static final long Z_MASK = (1L << Z_BITS) - 1;

    // Base

    public static long pack(int x, int y, int z) {
        return ((x & X_MASK) << X_SHIFT) |
                ((y & Y_MASK) << Y_SHIFT) |
                (z & Z_MASK);
    }

    public static int unpackX(long value) {
        return (int) (value << (64 - X_BITS - X_SHIFT)) >> (64 - X_BITS);
    }

    public static int unpackY(long value) {
        return (int) (value << (64 - Y_BITS - Y_SHIFT)) >> (64 - Y_BITS);
    }

    public static int unpackZ(long value) {
        return (int) (value << (64 - Z_BITS)) >> (64 - Z_BITS);
    }

    // Arithmetic \\

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

    // Utility \\

    public static String toString(long value) {
        return "Coordinate3Int(" + unpackX(value) + ", " +
                unpackY(value) + ", " +
                unpackZ(value) + ")";
    }
}
