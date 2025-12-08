package com.AdventureRPG.core.util.Methematics.Vectors;

public class Vector4Double {

    // Data
    public double x, y, z, w;

    // Constructors \\

    public Vector4Double(double x, double y, double z, double w) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4Double() {
        this(0, 0, 0, 0);
    }

    public Vector4Double(double scalar) {
        this(scalar, scalar, scalar, scalar);
    }

    public Vector4Double(Vector4Double other) {
        this(other.x, other.y, other.z, other.w);
    }

    // Set \\

    public Vector4Double set(double x, double y, double z, double w) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;

        return this;
    }

    public Vector4Double set(double scalar) {
        return set(scalar, scalar, scalar, scalar);
    }

    public Vector4Double set(Vector4Double other) {
        return set(other.x, other.y, other.z, other.w);
    }

    // Addition \\

    public Vector4Double add(double x, double y, double z, double w) {

        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;

        return this;
    }

    public Vector4Double add(double scalar) {
        return add(scalar, scalar, scalar, scalar);
    }

    public Vector4Double add(Vector4Double other) {
        return add(other.x, other.y, other.z, other.w);
    }

    // Subtraction \\

    public Vector4Double subtract(double x, double y, double z, double w) {

        this.x -= x;
        this.y -= y;
        this.z -= z;
        this.w -= w;

        return this;
    }

    public Vector4Double subtract(double scalar) {
        return subtract(scalar, scalar, scalar, scalar);
    }

    public Vector4Double subtract(Vector4Double other) {
        return subtract(other.x, other.y, other.z, other.w);
    }

    // Multiplication \\

    public Vector4Double multiply(double x, double y, double z, double w) {

        this.x *= x;
        this.y *= y;
        this.z *= z;
        this.w *= w;

        return this;
    }

    public Vector4Double multiply(double scalar) {
        return multiply(scalar, scalar, scalar, scalar);
    }

    public Vector4Double multiply(Vector4Double other) {
        return multiply(other.x, other.y, other.z, other.w);
    }

    // Division \\

    public Vector4Double divide(double x, double y, double z, double w) {

        if (x == 0 || y == 0 || z == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

        this.x /= x;
        this.y /= y;
        this.z /= z;
        this.w /= w;

        return this;
    }

    public Vector4Double divide(double scalar) {
        return divide(scalar, scalar, scalar, scalar);
    }

    public Vector4Double divide(Vector4Double other) {
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

        if (obj instanceof Vector4Double) {

            Vector4Double v = (Vector4Double) obj;
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

        result = 31 * result + Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        result = 31 * result + Double.hashCode(w);

        return result;
    }

    @Override
    public String toString() {
        return "Vector4Double(" + x + ", " + y + ", " + z + ", " + w + ")";
    }
}
