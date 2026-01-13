package com.internal.core.util.mathematics.vectors;

import com.internal.core.util.mathematics.Extras.Direction3Int;

public class Vector3 {

    // Data
    public float x, y, z;

    // Constructors \\

    public Vector3(float x, float y, float z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3() {
        this(0, 0, 0);
    }

    public Vector3(float scalar) {
        this(scalar, scalar, scalar);
    }

    public Vector3(Vector3 other) {
        this(other.x, other.y, other.z);
    }

    // Conversion \\

    public com.badlogic.gdx.math.Vector3 toGdx() {
        return new com.badlogic.gdx.math.Vector3(x, y, z);
    }

    public Vector3 fromGDX(com.badlogic.gdx.math.Vector3 other) {
        return set(other.x, other.y, other.z);
    }

    // Set \\

    public Vector3 set(float x, float y, float z) {

        this.x = x;
        this.y = y;
        this.z = z;

        return this;
    }

    public Vector3 set(float scalar) {
        return set(scalar, scalar, scalar);
    }

    public Vector3 set(Vector3 other) {
        return set(other.x, other.y, other.z);
    }

    // Addition \\

    public Vector3 add(float x, float y, float z) {

        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public Vector3 add(float scalar) {
        return add(scalar, scalar, scalar);
    }

    public Vector3 add(Vector3 other) {
        return add(other.x, other.y, other.z);
    }

    // Subtraction \\

    public Vector3 subtract(float x, float y, float z) {

        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Vector3 subtract(float scalar) {
        return subtract(scalar, scalar, scalar);
    }

    public Vector3 subtract(Vector3 other) {
        return subtract(other.x, other.y, other.z);
    }

    // Multiplication \\

    public Vector3 multiply(float x, float y, float z) {

        this.x *= x;
        this.y *= y;
        this.z *= z;

        return this;
    }

    public Vector3 multiply(float scalar) {
        return multiply(scalar, scalar, scalar);
    }

    public Vector3 multiply(Vector3 other) {
        return multiply(other.x, other.y, other.z);
    }

    // Division \\

    public Vector3 divide(float x, float y, float z) {

        if (x == 0 || y == 0 || z == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

        this.x /= x;
        this.y /= y;
        this.z /= z;

        return this;
    }

    public Vector3 divide(float scalar) {
        return divide(scalar, scalar, scalar);
    }

    public Vector3 divide(Vector3 other) {
        return divide(other.x, other.y, other.z);
    }

    // Normalization \\

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public Vector3 normalize() {
        float len = length();

        if (len == 0)
            return this;

        this.x /= len;
        this.y /= len;
        this.z /= len;

        return this;
    }

    // Direction Mapping \\

    public Vector3 up() {

        this.x = Direction3Int.UP.x;
        this.y = Direction3Int.UP.y;
        this.z = Direction3Int.UP.z;

        return this;
    }

    public Vector3 north() {

        this.x = Direction3Int.NORTH.x;
        this.y = Direction3Int.NORTH.y;
        this.z = Direction3Int.NORTH.z;

        return this;
    }

    public Vector3 south() {

        this.x = Direction3Int.SOUTH.x;
        this.y = Direction3Int.SOUTH.y;
        this.z = Direction3Int.SOUTH.z;

        return this;
    }

    public Vector3 east() {

        this.x = Direction3Int.EAST.x;
        this.y = Direction3Int.EAST.y;
        this.z = Direction3Int.EAST.z;

        return this;
    }

    public Vector3 west() {

        this.x = Direction3Int.WEST.x;
        this.y = Direction3Int.WEST.y;
        this.z = Direction3Int.WEST.z;

        return this;
    }

    public Vector3 down() {

        this.x = Direction3Int.DOWN.x;
        this.y = Direction3Int.DOWN.y;
        this.z = Direction3Int.DOWN.z;

        return this;
    }

    // Utility \\

    public boolean hasValues() {
        return x != 0 ||
                y != 0 ||
                z != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Vector3) {

            Vector3 v = (Vector3) obj;
            return this.x == v.x &&
                    this.y == v.y &&
                    this.z == v.z;
        }

        return false;
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + Float.hashCode(x);
        result = 31 * result + Float.hashCode(y);
        result = 31 * result + Float.hashCode(z);

        return result;
    }

    @Override
    public String toString() {
        return "Vector3(" + x + ", " + y + ", " + z + ")";
    }
}
