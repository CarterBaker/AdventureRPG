package com.AdventureRPG.Util;

public final class Coordinate2Int {

    // Base \\

    public static long pack(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public static int unpackX(long value) {
        return (int) (value >> 32);
    }

    public static int unpackY(long value) {
        return (int) value;
    }

    // Arithmetic \\

    public static long add(long a, long b) {
        int x = (int) (a >> 32) + (int) (b >> 32);
        int y = (int) a + (int) b;
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public static long subtract(long a, long b) {
        int x = (int) (a >> 32) - (int) (b >> 32);
        int y = (int) a - (int) b;
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public static long multiply(long a, long b) {
        int x = (int) (a >> 32) * (int) (b >> 32);
        int y = (int) a * (int) b;
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public static long divide(long a, long b) {
        int bx = (int) (b >> 32);
        int by = (int) b;

        // Avoid division by zero — keep original values if divisor is zero
        int x = bx != 0 ? (int) (a >> 32) / bx : (int) (a >> 32);
        int y = by != 0 ? (int) a / by : (int) a;

        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    // Utility \\

    public static String toString(long value) {
        return "Coordinate2Int(" + unpackX(value) + ", " + unpackY(value) + ")";
    }
}
