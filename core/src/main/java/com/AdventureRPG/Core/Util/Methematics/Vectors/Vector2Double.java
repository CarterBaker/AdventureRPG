package com.AdventureRPG.core.util.Methematics.Vectors;

import com.AdventureRPG.core.util.Methematics.Extras.Direction2Int;

public class Vector2Double {

    // Data
    public double x, y;

    // Constructors \\

    public Vector2Double(double x, double y) {

        this.x = x;
        this.y = y;
    }

    public Vector2Double() {
        this(0, 0);
    }

    public Vector2Double(double scalar) {
        this(scalar, scalar);
    }

    public Vector2Double(Vector2Double other) {
        this(other.x, other.y);
    }

    // Set \\

    public Vector2Double set(double x, double y) {

        this.x = x;
        this.y = y;

        return this;
    }

    public Vector2Double set(double scalar) {
        return set(scalar, scalar);
    }

    public Vector2Double set(Vector2Double other) {
        return set(other.x, other.y);
    }

    // Addition \\

    public Vector2Double add(double x, double y) {

        this.x += x;
        this.y += y;

        return this;
    }

    public Vector2Double add(double scalar) {
        return add(scalar, scalar);
    }

    public Vector2Double add(Vector2Double other) {
        return add(other.x, other.y);
    }

    // Subtraction \\

    public Vector2Double subtract(double x, double y) {

        this.x -= x;
        this.y -= y;

        return this;
    }

    public Vector2Double subtract(double scalar) {
        return subtract(scalar, scalar);
    }

    public Vector2Double subtract(Vector2Double other) {
        return subtract(other.x, other.y);
    }

    // Multiplication \\

    public Vector2Double multiply(double x, double y) {

        this.x *= x;
        this.y *= y;

        return this;
    }

    public Vector2Double multiply(double scalar) {
        return multiply(scalar, scalar);
    }

    public Vector2Double multiply(Vector2Double other) {
        return multiply(other.x, other.y);
    }

    // Division \\

    public Vector2Double divide(double x, double y) {

        if (x == 0 || y == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

        this.x /= x;
        this.y /= y;

        return this;
    }

    public Vector2Double divide(double scalar) {
        return divide(scalar, scalar);
    }

    public Vector2Double divide(Vector2Double other) {
        return divide(other.x, other.y);
    }

    // Direction Mapping \\

    public Vector2Double up() {

        this.x = Direction2Int.NORTH.x;
        this.y = Direction2Int.NORTH.y;

        return this;
    }

    public Vector2Double down() {

        this.x = Direction2Int.SOUTH.x;
        this.y = Direction2Int.SOUTH.y;

        return this;
    }

    public Vector2Double left() {

        this.x = Direction2Int.WEST.x;
        this.y = Direction2Int.WEST.y;

        return this;
    }

    public Vector2Double right() {

        this.x = Direction2Int.EAST.x;
        this.y = Direction2Int.EAST.y;

        return this;
    }

    // Utility \\

    public boolean hasValues() {
        return x != 0 ||
                y != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Vector2Double) {

            Vector2Double v = (Vector2Double) obj;
            return this.x == v.x &&
                    this.y == v.y;
        }

        return false;
    }

    @Override
    public int hashCode() {

        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);

        return result;
    }

    @Override
    public String toString() {
        return "Vector2Double(" + x + ", " + y + ")";
    }
}
