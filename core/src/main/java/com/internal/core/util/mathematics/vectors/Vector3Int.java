package com.internal.core.util.mathematics.vectors;

import com.internal.core.util.mathematics.Extras.Coordinate3Int;
import com.internal.core.util.mathematics.Extras.Direction3Int;

public class Vector3Int {

    // Data
    public int x, y, z;

    // Constructors \\

    public Vector3Int(int x, int y, int z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3Int() {
        this(0, 0, 0);
    }

    public Vector3Int(int scalar) {
        this(scalar, scalar, scalar);
    }

    public Vector3Int(Vector3Int other) {
        this(other.x, other.y, other.z);
    }

    // Set \\

    public Vector3Int set(int x, int y, int z) {

        this.x = x;
        this.y = y;
        this.z = z;

        return this;
    }

    public Vector3Int set(int scalar) {
        return set(scalar, scalar, scalar);
    }

    public Vector3Int set(Vector3Int other) {
        return set(other.x, other.y, other.z);
    }

    // Addition \\

    public Vector3Int add(int x, int y, int z) {

        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public Vector3Int add(int scalar) {
        return add(scalar, scalar, scalar);
    }

    public Vector3Int add(Vector3Int other) {
        return add(other.x, other.y, other.z);
    }

    // Subtraction \\

    public Vector3Int subtract(int x, int y, int z) {

        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Vector3Int subtract(int scalar) {
        return subtract(scalar, scalar, scalar);
    }

    public Vector3Int subtract(Vector3Int other) {
        return subtract(other.x, other.y, other.z);
    }

    // Multiplication \\

    public Vector3Int multiply(int x, int y, int z) {

        this.x *= x;
        this.y *= y;
        this.z *= z;

        return this;
    }

    public Vector3Int multiply(int scalar) {
        return multiply(scalar, scalar, scalar);
    }

    public Vector3Int multiply(Vector3Int other) {
        return multiply(other.x, other.y, other.z);
    }

    // Division \\

    public Vector3Int divide(int x, int y, int z) {

        if (x == 0 || y == 0 || z == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

        this.x /= x;
        this.y /= y;
        this.z /= z;

        return this;
    }

    public Vector3Int divide(int scalar) {
        return divide(scalar, scalar, scalar);
    }

    public Vector3Int divide(Vector3Int other) {
        return divide(other.x, other.y, other.z);
    }

    // Direction Mapping \\

    public Vector3Int up() {

        this.x = Direction3Int.UP.x;
        this.y = Direction3Int.UP.y;
        this.z = Direction3Int.UP.z;

        return this;
    }

    public Vector3Int north() {

        this.x = Direction3Int.NORTH.x;
        this.y = Direction3Int.NORTH.y;
        this.z = Direction3Int.NORTH.z;

        return this;
    }

    public Vector3Int south() {

        this.x = Direction3Int.SOUTH.x;
        this.y = Direction3Int.SOUTH.y;
        this.z = Direction3Int.SOUTH.z;

        return this;
    }

    public Vector3Int east() {

        this.x = Direction3Int.EAST.x;
        this.y = Direction3Int.EAST.y;
        this.z = Direction3Int.EAST.z;

        return this;
    }

    public Vector3Int west() {

        this.x = Direction3Int.WEST.x;
        this.y = Direction3Int.WEST.y;
        this.z = Direction3Int.WEST.z;

        return this;
    }

    public Vector3Int down() {

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

    public long pack() {
        return Coordinate3Int.pack(x, y, z);
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Vector3Int) {

            Vector3Int v = (Vector3Int) obj;
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
        return "Vector3Int(" + x + ", " + y + ", " + z + ")";
    }
}
