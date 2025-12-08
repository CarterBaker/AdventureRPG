package com.AdventureRPG.core.util.Methematics.Vectors;

public class Vector4Boolean {

    // Data
    public boolean x, y, z, w;

    // Constructors \\

    public Vector4Boolean(boolean x, boolean y, boolean z, boolean w) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4Boolean() {
        this(false, false, false, false);
    }

    public Vector4Boolean(boolean scalar) {
        this(scalar, scalar, scalar, scalar);
    }

    public Vector4Boolean(Vector4Boolean other) {
        this(other.x, other.y, other.z, other.w);
    }

    // Set \\

    public Vector4Boolean set(boolean x, boolean y, boolean z, boolean w) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;

        return this;
    }

    public Vector4Boolean set(boolean scalar) {
        return set(scalar, scalar, scalar, scalar);
    }

    public Vector4Boolean set(Vector4Boolean other) {
        return set(other.x, other.y, other.z, other.w);
    }

    // And \\

    public Vector4Boolean and(boolean x, boolean y, boolean z, boolean w) {

        this.x = this.x && x;
        this.y = this.y && y;
        this.z = this.z && z;
        this.w = this.w && w;

        return this;
    }

    public Vector4Boolean and(boolean scalar) {
        return and(scalar, scalar, scalar, scalar);
    }

    public Vector4Boolean and(Vector4Boolean other) {
        return and(other.x, other.y, other.z, other.w);
    }

    // Or \\

    public Vector4Boolean or(boolean x, boolean y, boolean z, boolean w) {

        this.x = this.x || x;
        this.y = this.y || y;
        this.z = this.z || z;
        this.w = this.w || w;

        return this;
    }

    public Vector4Boolean or(boolean scalar) {
        return or(scalar, scalar, scalar, scalar);
    }

    public Vector4Boolean or(Vector4Boolean other) {
        return or(other.x, other.y, other.z, other.w);
    }

    // Xor \\

    public Vector4Boolean xor(boolean x, boolean y, boolean z, boolean w) {

        this.x = this.x ^ x;
        this.y = this.y ^ y;
        this.z = this.z ^ z;
        this.w = this.w ^ w;

        return this;
    }

    public Vector4Boolean xor(boolean scalar) {
        return xor(scalar, scalar, scalar, scalar);
    }

    public Vector4Boolean xor(Vector4Boolean other) {
        return xor(other.x, other.y, other.z, other.w);
    }

    // Not \\

    public Vector4Boolean not() {

        this.x = !this.x;
        this.y = !this.y;
        this.z = !this.z;
        this.w = !this.w;

        return this;
    }

    // Utility \\

    public boolean any() {
        return x || y || z || w;
    }

    public boolean all() {
        return x && y && z && w;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Vector4Boolean))
            return false;

        Vector4Boolean v = (Vector4Boolean) obj;

        return x == v.x && y == v.y && z == v.z && w == v.w;
    }

    @Override
    public int hashCode() {

        int h = Boolean.hashCode(x);
        h = 31 * h + Boolean.hashCode(y);
        h = 31 * h + Boolean.hashCode(z);
        return 31 * h + Boolean.hashCode(w);
    }

    @Override
    public String toString() {
        return "Vector4Boolean(" + x + ", " + y + ", " + z + ", " + w + ")";
    }
}
