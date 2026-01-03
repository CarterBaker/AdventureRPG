package com.AdventureRPG.core.util.mathematics.vectors;

import com.AdventureRPG.core.util.mathematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.mathematics.Extras.Direction2Int;

public class Vector2Int {

    // Data
    public int x, y;

    // Constructors \\

    public Vector2Int(int x, int y) {

        this.x = x;
        this.y = y;
    }

    public Vector2Int() {
        this(0, 0);
    }

    public Vector2Int(int scalar) {
        this(scalar, scalar);
    }

    public Vector2Int(Vector2Int other) {
        this(other.x, other.y);
    }

    // Set \\

    public Vector2Int set(int x, int y) {

        this.x = x;
        this.y = y;

        return this;
    }

    public Vector2Int set(int scalar) {
        return set(scalar, scalar);
    }

    public Vector2Int set(Vector2Int other) {
        return set(other.x, other.y);
    }

    // Addition \\

    public Vector2Int add(int x, int y) {

        this.x += x;
        this.y += y;

        return this;
    }

    public Vector2Int add(int scalar) {
        return add(scalar, scalar);
    }

    public Vector2Int add(Vector2Int other) {
        return add(other.x, other.y);
    }

    // Subtraction \\

    public Vector2Int subtract(int x, int y) {

        this.x -= x;
        this.y -= y;

        return this;
    }

    public Vector2Int subtract(int scalar) {
        return subtract(scalar, scalar);
    }

    public Vector2Int subtract(Vector2Int other) {
        return subtract(other.x, other.y);
    }

    // Multiplication \\

    public Vector2Int multiply(int x, int y) {

        this.x *= x;
        this.y *= y;

        return this;
    }

    public Vector2Int multiply(int scalar) {
        return multiply(scalar, scalar);
    }

    public Vector2Int multiply(Vector2Int other) {
        return multiply(other.x, other.y);
    }

    // Division \\

    public Vector2Int divide(int x, int y) {

        if (x == 0 || y == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

        this.x /= x;
        this.y /= y;

        return this;
    }

    public Vector2Int divide(int scalar) {
        return divide(scalar, scalar);
    }

    public Vector2Int divide(Vector2Int other) {
        return divide(other.x, other.y);
    }

    // Direction Mapping \\

    public Vector2Int up() {

        this.x = Direction2Int.NORTH.x;
        this.y = Direction2Int.NORTH.y;

        return this;
    }

    public Vector2Int down() {

        this.x = Direction2Int.SOUTH.x;
        this.y = Direction2Int.SOUTH.y;

        return this;
    }

    public Vector2Int left() {

        this.x = Direction2Int.WEST.x;
        this.y = Direction2Int.WEST.y;

        return this;
    }

    public Vector2Int right() {

        this.x = Direction2Int.EAST.x;
        this.y = Direction2Int.EAST.y;

        return this;
    }

    // Utility \\

    public boolean hasValues() {
        return x != 0 ||
                y != 0;
    }

    public long pack() {
        return Coordinate2Int.pack(x, y);
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Vector2Int) {

            Vector2Int v = (Vector2Int) obj;
            return this.x == v.x &&
                    this.y == v.y;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "Vector2Int(" + x + ", " + y + ")";
    }
}
