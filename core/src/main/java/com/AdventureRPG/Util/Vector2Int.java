package com.AdventureRPG.Util;

public class Vector2Int {

    public int x, y;

    public Vector2Int() {

        this.x = 0;
        this.y = 0;
    }

    public Vector2Int(int x, int y) {

        this.x = x;
        this.y = y;
    }

    public Vector2Int set(int x, int y) {

        this.x = x;
        this.y = y;

        return this;
    }

    public Vector2Int set(Vector2Int input) {
        return this.set(input.x, input.y);
    }

    public long pack() {
        return Coordinate2Int.pack(x, y);
    }

    public Vector2Int add(Vector2Int other) {

        this.x += other.x;
        this.y += other.y;

        return this;
    }

    public Vector2Int subtract(Vector2Int other) {

        this.x -= other.x;
        this.y -= other.y;

        return this;
    }

    public Vector2Int multiply(int scalar) {

        this.x *= scalar;
        this.y *= scalar;

        return this;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Vector2Int) {

            Vector2Int v = (Vector2Int) obj;
            return this.x == v.x && this.y == v.y;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "Vector2Int(" + x + ", " + y + ")";
    }
}
