package com.AdventureRPG.core.util.Mathematics.Extras;

public enum Direction2Int {

    NORTH(0, 1),
    SOUTH(0, -1),
    EAST(1, 0),
    WEST(-1, 0),

    NORTHEAST(1, 1),
    NORTHWEST(-1, 1),
    SOUTHEAST(1, -1),
    SOUTHWEST(-1, -1);

    public final int index;
    public final int x, y;
    public final long packed;

    Direction2Int(int x, int y) {

        this.index = this.ordinal();

        this.x = x;
        this.y = y;

        this.packed = Coordinate2Int.pack(x, y);
    }

}
