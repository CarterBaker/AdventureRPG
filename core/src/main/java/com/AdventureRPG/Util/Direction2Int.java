package com.AdventureRPG.Util;

public enum Direction2Int {

    NORTH(0, 0, 1),
    SOUTH(1, 0, -1),
    EAST(2, 1, 0),
    WEST(3, -1, 0),

    NORTHEAST(4, 1, 1),
    NORTHWEST(5, -1, 1),
    SOUTHEAST(6, 1, -1),
    SOUTHWEST(7, -1, -1);

    public final int index;
    public final int x, y;
    public final long packed;

    Direction2Int(int index, int x, int y) {
        this.index = index;
        this.x = x;
        this.y = y;
        this.packed = Coordinate2Int.pack(x, y);
    }
}
