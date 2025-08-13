package com.AdventureRPG.Util;

public final class Coordinate2Int {

    public static long pack(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public static int unpackX(long value) {
        return (int) (value >> 32);
    }

    public static int unpackY(long value) {
        return (int) value;
    }

    public static String toString(long value) {

        int x = unpackX(value);
        int y = unpackY(value);

        return "Coordinate2Int(" + x + ", " + y + ")";
    }
}
