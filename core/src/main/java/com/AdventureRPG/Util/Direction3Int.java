package com.AdventureRPG.Util;

public enum Direction3Int {

    UP(0,
            0, 1, 0,
            null),
    NORTH(1,
            0, 0, 1,
            Direction2Int.NORTH),
    SOUTH(2,
            0, 0, -1,
            Direction2Int.SOUTH),
    EAST(3,
            1, 0, 0,
            Direction2Int.EAST),
    WEST(4,
            -1, 0, 0,
            Direction2Int.WEST),
    DOWN(4,
            0, -1, 0,
            null);

    public final int index;
    public final int x, y, z;
    public final Direction2Int direction2Int;
    public final long packed;

    Direction3Int(int index,
            int x, int y, int z,
            Direction2Int direction2Int) {

        this.index = index;
        this.x = x;
        this.y = y;
        this.z = z;
        this.direction2Int = direction2Int;
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

    public static Direction3Int[] getTangents(Direction3Int normal) {

        switch (normal) {

            case UP:
            case DOWN:
                return new Direction3Int[] { EAST, NORTH };

            case NORTH:
            case SOUTH:
                return new Direction3Int[] { EAST, UP };

            case EAST:
            case WEST:
                return new Direction3Int[] { NORTH, UP };

            default:
                throw new IllegalStateException("Unexpected normal: " + normal);
        }
    }
}
