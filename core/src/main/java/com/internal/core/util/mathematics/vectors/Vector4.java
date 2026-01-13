package com.internal.core.util.mathematics.vectors;

public class Vector4 {

    // Data
    public float x, y, z, w;

    // Constructors \\

    public Vector4(float x, float y, float z, float w) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4() {
        this(0, 0, 0, 0);
    }

    public Vector4(float scalar) {
        this(scalar, scalar, scalar, scalar);
    }

    public Vector4(Vector4 other) {
        this(other.x, other.y, other.z, other.w);
    }

    // Conversion \\

    public com.badlogic.gdx.math.Vector4 toGdx() {
        return new com.badlogic.gdx.math.Vector4(x, y, z, w);
    }

    public Vector4 fromGDX(com.badlogic.gdx.math.Vector4 other) {
        return set(other.x, other.y, other.z, other.w);
    }

    // Set \\

    public Vector4 set(float x, float y, float z, float w) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;

        return this;
    }

    public Vector4 set(float scalar) {
        return set(scalar, scalar, scalar, scalar);
    }

    public Vector4 set(Vector4 other) {
        return set(other.x, other.y, other.z, other.w);
    }

    // Addition \\

    public Vector4 add(float x, float y, float z, float w) {

        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;

        return this;
    }

    public Vector4 add(float scalar) {
        return add(scalar, scalar, scalar, scalar);
    }

    public Vector4 add(Vector4 other) {
        return add(other.x, other.y, other.z, other.w);
    }

    // Subtraction \\

    public Vector4 subtract(float x, float y, float z, float w) {

        this.x -= x;
        this.y -= y;
        this.z -= z;
        this.w -= w;

        return this;
    }

    public Vector4 subtract(float scalar) {
        return subtract(scalar, scalar, scalar, scalar);
    }

    public Vector4 subtract(Vector4 other) {
        return subtract(other.x, other.y, other.z, other.w);
    }

    // Multiplication \\

    public Vector4 multiply(float x, float y, float z, float w) {

        this.x *= x;
        this.y *= y;
        this.z *= z;
        this.w *= w;

        return this;
    }

    public Vector4 multiply(float scalar) {
        return multiply(scalar, scalar, scalar, scalar);
    }

    public Vector4 multiply(Vector4 other) {
        return multiply(other.x, other.y, other.z, other.w);
    }

    // Division \\

    public Vector4 divide(float x, float y, float z, float w) {

        if (x == 0 || y == 0 || z == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

        this.x /= x;
        this.y /= y;
        this.z /= z;
        this.w /= w;

        return this;
    }

    public Vector4 divide(float scalar) {
        return divide(scalar, scalar, scalar, scalar);
    }

    public Vector4 divide(Vector4 other) {
        return divide(other.x, other.y, other.z, other.w);
    }

    // Normalization \\

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z + w * w);
    }

    public float lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }

    public Vector4 normalize() {
        float len = length();

        if (len == 0)
            return this;

        this.x /= len;
        this.y /= len;
        this.z /= len;
        this.w /= len;

        return this;
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

        if (obj instanceof Vector4) {

            Vector4 v = (Vector4) obj;
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
        return "Vector4(" + x + ", " + y + ", " + z + ", " + w + ")";
    }
}
