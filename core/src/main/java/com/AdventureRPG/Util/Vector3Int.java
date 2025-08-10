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

    public Vector3Int add(Direction direction) {
        this.x += direction.x;
        this.y += direction.y;
        this.z += direction.z;
        return this;
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

    public void normalize() {
        normalize(1);
    }

    public void normalize(int input) {

        int maxAbs = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));

        if (maxAbs == 0)
            return; // Avoid division by zero

        double scale = (double) input / maxAbs;

        x = (int) Math.round(x * scale);
        y = (int) Math.round(y * scale);
        z = (int) Math.round(z * scale);
    }

    public boolean hasValues() {
        return x != 0 || y != 0 || z != 0;
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
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
