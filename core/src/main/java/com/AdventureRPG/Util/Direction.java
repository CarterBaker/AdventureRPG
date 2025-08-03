package com.AdventureRPG.Util;

public enum Direction {
    UP(0, 1, 0),
    DOWN(0, -1, 0),
    LEFT(-1, 0, 0),
    RIGHT(1, 0, 0),
    FRONT(0, 0, 1),
    BACK(0, 0, -1);

    public final int x, y, z;

    Direction(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
