package com.AdventureRPG.Util;

public class Vector3Int {
    public int x;
    public int y;
    public int z;

    public Vector3Int() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public Vector3Int(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(Vector3Int input) {
        set(input.x, input.y, input.z);
    }

    public Vector3Int add(Vector3Int other) {
        return new Vector3Int(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vector3Int subtract(Vector3Int other) {
        return new Vector3Int(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector3Int multiply(int scalar) {
        return new Vector3Int(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public int dot(Vector3Int other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public int magnitudeSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public double distance(Vector3Int other) {
        return Math.sqrt(this.subtract(other).magnitudeSquared());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector3Int) {
            Vector3Int v = (Vector3Int) obj;
            return this.x == v.x && this.y == v.y && this.z == v.z;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + x;
        hash = 31 * hash + y;
        hash = 31 * hash + z;
        return hash;
    }

    @Override
    public String toString() {
        return "Vector3Int(" + x + ", " + y + ", " + z + ")";
    }
}
