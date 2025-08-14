package com.AdventureRPG.Util;

public enum Direction2Int {

    NORTH(0, 1),
    SOUTH(0, -1),
    EAST(1, 0),
    WEST(-1, 0);

    public final int x, y;
    public final long packed;

    Direction2Int(int x, int y) {
        this.x = x;
        this.y = y;
        this.packed = Coordinate2Int.pack(x, y);
    }
}
