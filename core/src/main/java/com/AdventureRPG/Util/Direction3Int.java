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

    public static final Direction3Int[] DIRECTIONS = {
            Direction3Int.UP,
            Direction3Int.NORTH,
            Direction3Int.SOUTH,
            Direction3Int.EAST,
            Direction3Int.WEST,
            Direction3Int.DOWN
    };
}
