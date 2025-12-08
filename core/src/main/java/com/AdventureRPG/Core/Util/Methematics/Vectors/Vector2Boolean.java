package com.AdventureRPG.core.util.Methematics.Vectors;

public class Vector2Boolean {

    // Data
    public boolean x, y;

    // Constructors \\

    public Vector2Boolean(boolean x, boolean y) {

        this.x = x;
        this.y = y;
    }

    public Vector2Boolean() {
        this(false, false);
    }

    public Vector2Boolean(boolean scalar) {
        this(scalar, scalar);
    }

    public Vector2Boolean(Vector2Boolean other) {
        this(other.x, other.y);
    }

    // Set \\

    public Vector2Boolean set(boolean x, boolean y) {

        this.x = x;
        this.y = y;

        return this;
    }

    public Vector2Boolean set(boolean scalar) {
        return set(scalar, scalar);
    }

    public Vector2Boolean set(Vector2Boolean other) {
        return set(other.x, other.y);
    }

    // And \\

    public Vector2Boolean and(boolean x, boolean y) {

        this.x = this.x && x;
        this.y = this.y && y;

        return this;
    }

    public Vector2Boolean and(boolean scalar) {
        return and(scalar, scalar);
    }

    public Vector2Boolean and(Vector2Boolean other) {
        return and(other.x, other.y);
    }

    // Or \\

    public Vector2Boolean or(boolean x, boolean y) {

        this.x = this.x || x;
        this.y = this.y || y;

        return this;
    }

    public Vector2Boolean or(boolean scalar) {
        return or(scalar, scalar);
    }

    public Vector2Boolean or(Vector2Boolean other) {
        return or(other.x, other.y);
    }

    // Xor \\

    public Vector2Boolean xor(boolean x, boolean y) {

        this.x = this.x ^ x;
        this.y = this.y ^ y;

        return this;
    }

    public Vector2Boolean xor(boolean scalar) {
        return xor(scalar, scalar);
    }

    public Vector2Boolean xor(Vector2Boolean other) {
        return xor(other.x, other.y);
    }

    // Not \\

    public Vector2Boolean not() {

        this.x = !this.x;
        this.y = !this.y;

        return this;
    }

    // Utility \\

    public boolean any() {
        return x || y;
    }

    public boolean all() {
        return x && y;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Vector2Boolean))
            return false;

        Vector2Boolean v = (Vector2Boolean) obj;

        return x == v.x && y == v.y;
    }

    @Override
    public int hashCode() {

        int h = Boolean.hashCode(x);

        return 31 * h + Boolean.hashCode(y);
    }

    @Override
    public String toString() {
        return "Vector2Boolean(" + x + ", " + y + ")";
    }
}
