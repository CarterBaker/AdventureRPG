package com.AdventureRPG.Util;

public final class Coordinate2Int {

    // Base \\

    public static long pack(int x, int y) {
        return (((long) x) & 0xFFFFFFFFL) << 32 | (((long) y) & 0xFFFFFFFFL);
    }

    public static int unpackX(long value) {
        return (int) (value >>> 32);
    }

    public static int unpackY(long value) {
        return (int) value;
    }

    // Arithmetic \\

    public static long add(long a, long b) {

        int ax = unpackX(a);
        int ay = unpackY(a);

        int bx = unpackX(b);
        int by = unpackY(b);

        int cx = ax + bx;
        int cy = ay + by;

        return pack(cx, cy);
    }

    public static long subtract(long a, long b) {

        int ax = unpackX(a);
        int ay = unpackY(a);

        int bx = unpackX(b);
        int by = unpackY(b);

        int cx = ax - bx;
        int cy = ay - by;

        return pack(cx, cy);
    }

    public static long multiply(long a, long b) {

        int ax = unpackX(a);
        int ay = unpackY(a);

        int bx = unpackX(b);
        int by = unpackY(b);

        int cx = ax * bx;
        int cy = ay * by;

        return pack(cx, cy);
    }

    public static long divide(long a, long b) {

        int ax = unpackX(a);
        int ay = unpackY(a);

        int bx = unpackX(b);
        int by = unpackY(b);

        int cx = (bx != 0) ? (ax / bx) : ax;
        int cy = (by != 0) ? (ay / by) : ay;

        return pack(cx, cy);
    }

    // Utility \\

    public static String toString(long value) {
        return "Coordinate2Int(" + unpackX(value) + ", " + unpackY(value) + ")";
    }
}
