package com.AdventureRPG.core.util.Mathematics.Matrices;

public class Matrix2 {

    // Data
    public float m00, m01;
    public float m10, m11;

    // Constructors \\

    public Matrix2(
            float m00, float m01,
            float m10, float m11) {

        this.m00 = m00;
        this.m01 = m01;

        this.m10 = m10;
        this.m11 = m11;
    }

    public Matrix2() {
        this(1, 0,
                0, 1);
    }

    public Matrix2(float scalar) {
        this(scalar, 0,
                0, scalar);
    }

    public Matrix2(Matrix2 other) {
        this(other.m00, other.m01,
                other.m10, other.m11);
    }

    public Matrix2(float[] array) {
        this(
                array[0], array[1],
                array[2], array[3]);

        if (array == null || array.length != 9) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix3 array must have exactly 9 elements");
    }

    // Set \\

    public Matrix2 set(
            float m00, float m01,
            float m10, float m11) {

        this.m00 = m00;
        this.m01 = m01;

        this.m10 = m10;
        this.m11 = m11;

        return this;
    }

    public Matrix2 set(float scalar) {
        return set(
                scalar, 0,
                0, scalar);
    }

    public Matrix2 set(Matrix2 other) {
        return set(
                other.m00, other.m01,
                other.m10, other.m11);
    }

    // Addition \\

    public Matrix2 add(
            float m00, float m01,
            float m10, float m11) {

        this.m00 += m00;
        this.m01 += m01;

        this.m10 += m10;
        this.m11 += m11;

        return this;
    }

    public Matrix2 add(float scalar) {
        return add(
                scalar, scalar,
                scalar, scalar);
    }

    public Matrix2 add(Matrix2 other) {
        return add(
                other.m00, other.m01,
                other.m10, other.m11);
    }

    // Subtraction \\

    public Matrix2 subtract(
            float m00, float m01,
            float m10, float m11) {

        this.m00 -= m00;
        this.m01 -= m01;

        this.m10 -= m10;
        this.m11 -= m11;

        return this;
    }

    public Matrix2 subtract(float scalar) {
        return subtract(
                scalar, scalar,
                scalar, scalar);
    }

    public Matrix2 subtract(Matrix2 other) {
        return subtract(
                other.m00, other.m01,
                other.m10, other.m11);
    }

    // Multiplication \\

    public Matrix2 multiply(Matrix2 other) {

        // Prevent self-multiply corruption
        float a00 = m00, a01 = m01;
        float a10 = m10, a11 = m11;

        float r00 = a00 * other.m00 + a01 * other.m10;
        float r01 = a00 * other.m01 + a01 * other.m11;

        float r10 = a10 * other.m00 + a11 * other.m10;
        float r11 = a10 * other.m01 + a11 * other.m11;

        m00 = r00;
        m01 = r01;
        m10 = r10;
        m11 = r11;

        return this;
    }

    // Scalar Multiplication \\

    public Matrix2 multiply(float s) {

        m00 *= s;
        m01 *= s;
        m10 *= s;
        m11 *= s;

        return this;
    }

    // Division \\

    public Matrix2 divide(Matrix2 other) {

        Matrix2 inv = new Matrix2(other).inverse();

        return multiply(inv);
    }

    public Matrix2 divide(float scalar) {

        if (scalar == 0) // TODO: Add my own error
            throw new ArithmeticException("Division by zero");

        return multiply(1.0f / scalar);
    }

    // Inversion \\

    public Matrix2 inverse() {

        float a00 = m00, a01 = m01;
        float a10 = m10, a11 = m11;

        float det = a00 * a11 - a01 * a10;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        float invDet = 1.0f / det;

        float r00 = a11 * invDet;
        float r01 = -a01 * invDet;
        float r10 = -a10 * invDet;
        float r11 = a00 * invDet;

        m00 = r00;
        m01 = r01;
        m10 = r10;
        m11 = r11;

        return this;
    }

    // Utility \\

    public boolean hasValues() {
        return m00 != 0 || m01 != 0 ||
                m10 != 0 || m11 != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Matrix2) {
            Matrix2 other = (Matrix2) obj;
            return m00 == other.m00 && m01 == other.m01 &&
                    m10 == other.m10 && m11 == other.m11;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int r = 17;

        r = 31 * r + Float.hashCode(m00);
        r = 31 * r + Float.hashCode(m01);

        r = 31 * r + Float.hashCode(m10);
        r = 31 * r + Float.hashCode(m11);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix2(" +
                "[" + m00 + ", " + m01 + "], " +
                "[" + m10 + ", " + m11 + "]" +
                ")";
    }
}
