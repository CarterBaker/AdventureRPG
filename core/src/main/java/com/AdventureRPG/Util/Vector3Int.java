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

    // Signed encode using 3 × 21 bits = 63 bits total
    public static long encode(Vector3Int key) {
        return encode(key.x, key.y, key.z);
    }

    public static long encode(int x, int y, int z) {
        long xx = zigzag(x) & 0x1FFFFF; // 21 bits
        long yy = zigzag(y) & 0x1FFFFF;
        long zz = zigzag(z) & 0x1FFFFF;

        return (xx << 42) | (yy << 21) | zz;
    }

    public static Vector3Int decode(long key) {
        int x = unzigzag((key >> 42) & 0x1FFFFF);
        int y = unzigzag((key >> 21) & 0x1FFFFF);
        int z = unzigzag(key & 0x1FFFFF);
        return new Vector3Int(x, y, z);
    }

    // ZigZag Encoding
    private static long zigzag(int n) {
        return (n << 1) ^ (n >> 31);
    }

    private static int unzigzag(long n) {
        return (int) ((n >>> 1) ^ -(n & 1));
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
