package com.AdventureRPG.core.util.Methematics.Vectors;

public class Vector4Int {

    // Data
    public int x, y, z, w;

    // Constructors \\

    public Vector4Int(int x, int y, int z, int w) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4Int() {
        this(0, 0, 0, 0);
    }

    public Vector4Int(int scalar) {
        this(scalar, scalar, scalar, scalar);
    }

    public Vector4Int(Vector4Int other) {
        this(other.x, other.y, other.z, other.w);
    }

    // Set \\

    public Vector4Int set(int x, int y, int z, int w) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;

        return this;
    }

    public Vector4Int set(int scalar) {
        return set(scalar, scalar, scalar, scalar);
    }

    public Vector4Int set(Vector4Int other) {
        return set(other.x, other.y, other.z, other.w);
    }

    // Addition \\

    public Vector4Int add(int x, int y, int z, int w) {

        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;

        return this;
    }

    public Vector4Int add(int scalar) {
        return add(scalar, scalar, scalar, scalar);
    }

    public Vector4Int add(Vector4Int other) {
        return add(other.x, other.y, other.z, other.w);
    }

    // Subtraction \\

    public Vector4Int subtract(int x, int y, int z, int w) {

        this.x -= x;
        this.y -= y;
        this.z -= z;
        this.w -= w;

        return this;
    }

    public Vector4Int subtract(int scalar) {
        return subtract(scalar, scalar, scalar, scalar);
    }

    public Vector4Int subtract(Vector4Int other) {
        return subtract(other.x, other.y, other.z, other.w);
    }

    // Multiplication \\

    public Vector4Int multiply(int x, int y, int z, int w) {

        this.x *= x;
        this.y *= y;
        this.z *= z;
        this.w *= w;

        return this;
    }

    public Vector4Int multiply(int scalar) {
        return multiply(scalar, scalar, scalar, scalar);
    }

    public Vector4Int multiply(Vector4Int other) {
        return multiply(other.x, other.y, other.z, other.w);
    }

    // Division \\

    public Vector4Int divide(int x, int y, int z, int w) {

        if (x == 0 || y == 0 || z == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

        this.x /= x;
        this.y /= y;
        this.z /= z;
        this.w /= w;

        return this;
    }

    public Vector4Int divide(int scalar) {
        return divide(scalar, scalar, scalar, scalar);
    }

    public Vector4Int divide(Vector4Int other) {
        return divide(other.x, other.y, other.z, other.w);
    }

    // Utility \\

    public boolean hasValues() {
        return x != 0 ||
                y != 0 ||
                z != 0 ||
                w != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Vector4Int) {

            Vector4Int v = (Vector4Int) obj;
            return this.x == v.x &&
                    this.y == v.y &&
                    this.z == v.z &&
                    this.w == v.w;
        }

        return false;
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + Float.hashCode(x);
        result = 31 * result + Float.hashCode(y);
        result = 31 * result + Float.hashCode(z);
        result = 31 * result + Float.hashCode(w);

        return result;
    }

    @Override
    public String toString() {
        return "Vector4Int(" + x + ", " + y + ", " + z + ", " + w + ")";
    }
}
