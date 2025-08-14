package com.AdventureRPG.Util;

public enum Direction3Int {

    UP(0, 1, 0),
    NORTH(0, 0, 1),
    SOUTH(0, 0, -1),
    EAST(1, 0, 0),
    WEST(-1, 0, 0),
    DOWN(0, -1, 0);

    public final int x, y, z;
    public final long packed;

    Direction3Int(int x, int y, int z) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.packed = Coordinate3Int.pack(x, y, z);
    }
}
