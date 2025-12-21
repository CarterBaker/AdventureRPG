package com.AdventureRPG.core.util.Mathematics.Vectors;

import com.AdventureRPG.core.util.Mathematics.Extras.Direction3Int;

public class Vector3Double {

    // Data
    public double x, y, z;

    // Constructors \\

    public Vector3Double(double x, double y, double z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3Double() {
        this(0, 0, 0);
    }

    public Vector3Double(double scalar) {
        this(scalar, scalar, scalar);
    }

    public Vector3Double(Vector3Double other) {
        this(other.x, other.y, other.z);
    }

    // Set \\

    public Vector3Double set(double x, double y, double z) {

        this.x = x;
        this.y = y;
        this.z = z;

        return this;
    }

    public Vector3Double set(double scalar) {
        return set(scalar, scalar, scalar);
    }

    public Vector3Double set(Vector3Double other) {
        return set(other.x, other.y, other.z);
    }

    // Addition \\

    public Vector3Double add(double x, double y, double z) {

        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public Vector3Double add(double scalar) {
        return add(scalar, scalar, scalar);
    }

    public Vector3Double add(Vector3Double other) {
        return add(other.x, other.y, other.z);
    }

    // Subtraction \\

    public Vector3Double subtract(double x, double y, double z) {

        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Vector3Double subtract(double scalar) {
        return subtract(scalar, scalar, scalar);
    }

    public Vector3Double subtract(Vector3Double other) {
        return subtract(other.x, other.y, other.z);
    }

    // Multiplication \\

    public Vector3Double multiply(double x, double y, double z) {

        this.x *= x;
        this.y *= y;
        this.z *= z;

        return this;
    }

    public Vector3Double multiply(double scalar) {
        return multiply(scalar, scalar, scalar);
    }

    public Vector3Double multiply(Vector3Double other) {
        return multiply(other.x, other.y, other.z);
    }

    // Division \\

    public Vector3Double divide(double x, double y, double z) {

        if (x == 0 || y == 0 || z == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

        this.x /= x;
        this.y /= y;
        this.z /= z;

        return this;
    }

    public Vector3Double divide(double scalar) {
        return divide(scalar, scalar, scalar);
    }

    public Vector3Double divide(Vector3Double other) {
        return divide(other.x, other.y, other.z);
    }

    // Direction Mapping \\

    public Vector3Double up() {

        this.x = Direction3Int.UP.x;
        this.y = Direction3Int.UP.y;
        this.z = Direction3Int.UP.z;

        return this;
    }

    public Vector3Double north() {

        this.x = Direction3Int.NORTH.x;
        this.y = Direction3Int.NORTH.y;
        this.z = Direction3Int.NORTH.z;

        return this;
    }

    public Vector3Double south() {

        this.x = Direction3Int.SOUTH.x;
        this.y = Direction3Int.SOUTH.y;
        this.z = Direction3Int.SOUTH.z;

        return this;
    }

    public Vector3Double east() {

        this.x = Direction3Int.EAST.x;
        this.y = Direction3Int.EAST.y;
        this.z = Direction3Int.EAST.z;

        return this;
    }

    public Vector3Double west() {

        this.x = Direction3Int.WEST.x;
        this.y = Direction3Int.WEST.y;
        this.z = Direction3Int.WEST.z;

        return this;
    }

    public Vector3Double down() {

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

        if (obj instanceof Vector3Double) {

            Vector3Double v = (Vector3Double) obj;
            return this.x == v.x &&
                    this.y == v.y &&
                    this.z == v.z;
        }

        return false;
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);

        return result;
    }

    @Override
    public String toString() {
        return "Vector3Double(" + x + ", " + y + ", " + z + ")";
    }
}
