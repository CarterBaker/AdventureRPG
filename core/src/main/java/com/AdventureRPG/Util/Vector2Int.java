package com.AdventureRPG.Util;

public class Vector2Int {
    public int x;
    public int y;

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

    public Vector2Int add(Vector2Int other) {
        return new Vector2Int(this.x + other.x, this.y + other.y);
    }

    public Vector2Int subtract(Vector2Int other) {
        return new Vector2Int(this.x - other.x, this.y - other.y);
    }

    public Vector2Int multiply(int scalar) {
        return new Vector2Int(this.x * scalar, this.y * scalar);
    }

    public int dot(Vector2Int other) {
        return this.x * other.x + this.y * other.y;
    }

    public int magnitudeSquared() {
        return this.x * this.x + this.y * this.y;
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
        return "(" + x + ", " + y + ")";
    }
}
