package com.AdventureRPG.core.util.Methematics.Vectors;

public class Vector3Boolean {

    // Data
    public boolean x, y, z;

    // Constructors \\

    public Vector3Boolean(boolean x, boolean y, boolean z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3Boolean() {
        this(false, false, false);
    }

    public Vector3Boolean(boolean scalar) {
        this(scalar, scalar, scalar);
    }

    public Vector3Boolean(Vector3Boolean other) {
        this(other.x, other.y, other.z);
    }

    // Set \\

    public Vector3Boolean set(boolean x, boolean y, boolean z) {

        this.x = x;
        this.y = y;
        this.z = z;

        return this;
    }

    public Vector3Boolean set(boolean scalar) {
        return set(scalar, scalar, scalar);
    }

    public Vector3Boolean set(Vector3Boolean other) {
        return set(other.x, other.y, other.z);
    }

    // And \\

    public Vector3Boolean and(boolean x, boolean y, boolean z) {

        this.x = this.x && x;
        this.y = this.y && y;
        this.z = this.z && z;

        return this;
    }

    public Vector3Boolean and(boolean scalar) {
        return and(scalar, scalar, scalar);
    }

    public Vector3Boolean and(Vector3Boolean other) {
        return and(other.x, other.y, other.z);
    }

    // Or \\

    public Vector3Boolean or(boolean x, boolean y, boolean z) {

        this.x = this.x || x;
        this.y = this.y || y;
        this.z = this.z || z;

        return this;
    }

    public Vector3Boolean or(boolean scalar) {
        return or(scalar, scalar, scalar);
    }

    public Vector3Boolean or(Vector3Boolean other) {
        return or(other.x, other.y, other.z);
    }

    // Xor \\

    public Vector3Boolean xor(boolean x, boolean y, boolean z) {

        this.x = this.x ^ x;
        this.y = this.y ^ y;
        this.z = this.z ^ z;

        return this;
    }

    public Vector3Boolean xor(boolean scalar) {
        return xor(scalar, scalar, scalar);
    }

    public Vector3Boolean xor(Vector3Boolean other) {
        return xor(other.x, other.y, other.z);
    }

    // Not \\

    public Vector3Boolean not() {

        this.x = !this.x;
        this.y = !this.y;
        this.z = !this.z;

        return this;
    }

    // Utility \\

    public boolean any() {
        return x || y || z;
    }

    public boolean all() {
        return x && y && z;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Vector3Boolean))
            return false;

        Vector3Boolean v = (Vector3Boolean) obj;

        return x == v.x && y == v.y && z == v.z;
    }

    @Override
    public int hashCode() {

        int h = Boolean.hashCode(x);
        h = 31 * h + Boolean.hashCode(y);
        return 31 * h + Boolean.hashCode(z);
    }

    @Override
    public String toString() {
        return "Vector3Boolean(" + x + ", " + y + ", " + z + ")";
    }
}
