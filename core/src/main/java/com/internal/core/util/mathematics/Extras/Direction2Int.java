package com.internal.core.util.mathematics.Extras;

public enum Direction2Int {

    NORTH(0, 1),
    NORTHEAST(1, 1),
    EAST(1, 0),
    SOUTHEAST(1, -1),
    SOUTH(0, -1),
    SOUTHWEST(-1, -1),
    WEST(-1, 0),
    NORTHWEST(-1, 1);

    // internal
    public final int index;
    public final int x, y;
    public final long packed;

    public static final Direction2Int[] VALUES = values();
    public static final int LENGTH = values().length;

    // internal \\

    Direction2Int(int x, int y) {

        // internal
        this.index = this.ordinal();
        this.x = x;
        this.y = y;
        this.packed = Coordinate2Int.pack(x, y);
    }

    // Accessible \\

    public static Direction2Int getDirection(int i) {
        return VALUES[i];
    }

}
