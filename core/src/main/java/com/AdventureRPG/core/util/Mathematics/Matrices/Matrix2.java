package com.AdventureRPG.core.util.mathematics.matrices;

public class Matrix2 {

    // Data
    public final float[] val = new float[4];

    // Constructors \\

    public Matrix2(
            float m00, float m01,
            float m10, float m11) {
        val[0] = m00;
        val[1] = m10;
        val[2] = m01;
        val[3] = m11;
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
        System.arraycopy(other.val, 0, val, 0, 4);
    }

    public Matrix2(float[] array) {

        if (array == null || array.length != 4) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix2 array must have exactly 4 elements");

        // Row-major order [m00, m01, m10, m11]
        val[0] = array[0]; // m00
        val[1] = array[2]; // m10
        val[2] = array[1]; // m01
        val[3] = array[3]; // m11
    }

    // Accessors \\

    public float getM00() {
        return val[0];
    }

    public float getM10() {
        return val[1];
    }

    public float getM01() {
        return val[2];
    }

    public float getM11() {
        return val[3];
    }

    public void setM00(float value) {
        val[0] = value;
    }

    public void setM10(float value) {
        val[1] = value;
    }

    public void setM01(float value) {
        val[2] = value;
    }

    public void setM11(float value) {
        val[3] = value;
    }

    // Set \\

    public Matrix2 set(
            float m00, float m01,
            float m10, float m11) {

        val[0] = m00;
        val[1] = m10;
        val[2] = m01;
        val[3] = m11;

        return this;
    }

    public Matrix2 set(float scalar) {
        return set(
                scalar, 0,
                0, scalar);
    }

    public Matrix2 set(Matrix2 other) {

        System.arraycopy(other.val, 0, val, 0, 4);

        return this;
    }

    // Addition \\

    public Matrix2 add(
            float m00, float m01,
            float m10, float m11) {

        val[0] += m00;
        val[1] += m10;
        val[2] += m01;
        val[3] += m11;

        return this;
    }

    public Matrix2 add(float scalar) {
        return add(
                scalar, scalar,
                scalar, scalar);
    }

    public Matrix2 add(Matrix2 other) {
        return add(
                other.val[0], other.val[2],
                other.val[1], other.val[3]);
    }

    // Subtraction \\

    public Matrix2 subtract(
            float m00, float m01,
            float m10, float m11) {

        val[0] -= m00;
        val[1] -= m10;
        val[2] -= m01;
        val[3] -= m11;

        return this;
    }

    public Matrix2 subtract(float scalar) {
        return subtract(
                scalar, scalar,
                scalar, scalar);
    }

    public Matrix2 subtract(Matrix2 other) {
        return subtract(
                other.val[0], other.val[2],
                other.val[1], other.val[3]);
    }

    // Multiplication \\

    public Matrix2 multiply(
            float m00, float m01,
            float m10, float m11) {

        // Extract original values to prevent self-overwrite
        float a00 = val[0], a10 = val[1];
        float a01 = val[2], a11 = val[3];

        float r00 = a00 * m00 + a01 * m10;
        float r10 = a10 * m00 + a11 * m10;
        float r01 = a00 * m01 + a01 * m11;
        float r11 = a10 * m01 + a11 * m11;

        val[0] = r00;
        val[1] = r10;
        val[2] = r01;
        val[3] = r11;

        return this;
    }

    public Matrix2 multiply(float scalar) {
        return multiply(
                scalar, 0,
                0, scalar);
    }

    public Matrix2 multiply(Matrix2 other) {
        return multiply(
                other.val[0], other.val[2],
                other.val[1], other.val[3]);
    }

    // Division \\

    public Matrix2 divide(
            float m00, float m01,
            float m10, float m11) {

        // Compute determinant
        float det = m00 * m11 - m01 * m10;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        float invDet = 1.0f / det;

        // Invert the inputs
        float i00 = m11 * invDet;
        float i01 = -m01 * invDet;
        float i10 = -m10 * invDet;
        float i11 = m00 * invDet;

        // Multiply current matrix by the inverted values using master multiply
        return multiply(i00, i01, i10, i11);
    }

    public Matrix2 divide(float scalar) {

        if (scalar == 0) // TODO: Add my own error
            throw new ArithmeticException("Division by zero");

        return multiply(
                1.0f / scalar, 0,
                0, 1.0f / scalar);
    }

    public Matrix2 divide(Matrix2 other) {
        return divide(
                other.val[0], other.val[2],
                other.val[1], other.val[3]);
    }

    // Inversion \\

    public Matrix2 inverse() {

        float a00 = val[0], a10 = val[1];
        float a01 = val[2], a11 = val[3];

        float det = a00 * a11 - a01 * a10;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        float invDet = 1.0f / det;

        val[0] = a11 * invDet;
        val[1] = -a10 * invDet;
        val[2] = -a01 * invDet;
        val[3] = a00 * invDet;

        return this;
    }

    // Utility \\

    public boolean hasValues() {
        return val[0] != 0 || val[1] != 0 ||
                val[2] != 0 || val[3] != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Matrix2 other)
            return val[0] == other.val[0] && val[1] == other.val[1] &&
                    val[2] == other.val[2] && val[3] == other.val[3];

        return false;
    }

    @Override
    public int hashCode() {

        int r = 17;

        r = 31 * r + Float.hashCode(val[0]);
        r = 31 * r + Float.hashCode(val[1]);
        r = 31 * r + Float.hashCode(val[2]);
        r = 31 * r + Float.hashCode(val[3]);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix2(" +
                "[" + val[0] + ", " + val[2] + "], " +
                "[" + val[1] + ", " + val[3] + "]" +
                ")";
    }
}
