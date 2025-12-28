package com.AdventureRPG.core.util.Mathematics.Vectors;

import com.AdventureRPG.core.util.Mathematics.Extras.Direction2Int;

public class Vector2 {

    // Data
    public float x, y;

    // Constructors \\

    public Vector2(float x, float y) {

        this.x = x;
        this.y = y;
    }

    public Vector2() {
        this(0, 0);
    }

    public Vector2(float scalar) {
        this(scalar, scalar);
    }

    public Vector2(Vector2 other) {
        this(other.x, other.y);
    }

    // Conversion \\

    public com.badlogic.gdx.math.Vector2 toGdx() {
        return new com.badlogic.gdx.math.Vector2(x, y);
    }

    public Vector2 fromGDX(com.badlogic.gdx.math.Vector2 other) {
        return set(other.x, other.y);
    }

    // Set \\

    public Vector2 set(float x, float y) {

        this.x = x;
        this.y = y;

        return this;
    }

    public Vector2 set(float scalar) {
        return set(scalar, scalar);
    }

    public Vector2 set(Vector2 other) {
        return set(other.x, other.y);
    }

    // Addition \\

    public Vector2 add(float x, float y) {

        this.x += x;
        this.y += y;

        return this;
    }

    public Vector2 add(float scalar) {
        return add(scalar, scalar);
    }

    public Vector2 add(Vector2 other) {
        return add(other.x, other.y);
    }

    // Subtraction \\

    public Vector2 subtract(float x, float y) {

        this.x -= x;
        this.y -= y;

        return this;
    }

    public Vector2 subtract(float scalar) {
        return subtract(scalar, scalar);
    }

    public Vector2 subtract(Vector2 other) {
        return subtract(other.x, other.y);
    }

    // Multiplication \\

    public Vector2 multiply(float x, float y) {

        this.x *= x;
        this.y *= y;

        return this;
    }

    public Vector2 multiply(float scalar) {
        return multiply(scalar, scalar);
    }

    public Vector2 multiply(Vector2 other) {
        return multiply(other.x, other.y);
    }

    // Division \\

    public Vector2 divide(float x, float y) {

        if (x == 0 || y == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

        this.x /= x;
        this.y /= y;

        return this;
    }

    public Vector2 divide(float scalar) {
        return divide(scalar, scalar);
    }

    public Vector2 divide(Vector2 other) {
        return divide(other.x, other.y);
    }

    // Direction Mapping \\

    public Vector2 up() {

        this.x = Direction2Int.NORTH.x;
        this.y = Direction2Int.NORTH.y;

        return this;
    }

    public Vector2 down() {

        this.x = Direction2Int.SOUTH.x;
        this.y = Direction2Int.SOUTH.y;

        return this;
    }

    public Vector2 left() {

        this.x = Direction2Int.WEST.x;
        this.y = Direction2Int.WEST.y;

        return this;
    }

    public Vector2 right() {

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

        if (obj instanceof Vector2) {

            Vector2 v = (Vector2) obj;
            return this.x == v.x &&
                    this.y == v.y;
        }

        return false;
    }

    @Override
    public int hashCode() {

        int result = Float.hashCode(x);
        result = 31 * result + Float.hashCode(y);

        return result;
    }

    @Override
    public String toString() {
        return "Vector2(" + x + ", " + y + ")";
    }
}
