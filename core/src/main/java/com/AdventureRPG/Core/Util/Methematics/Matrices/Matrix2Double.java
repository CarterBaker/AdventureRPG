package com.AdventureRPG.Core.Util.Methematics.Matrices;

public class Matrix2Double {

    // Data
    public double m00, m01;
    public double m10, m11;

    // Constructors \\

    public Matrix2Double(
            double m00, double m01,
            double m10, double m11) {

        this.m00 = m00;
        this.m01 = m01;

        this.m10 = m10;
        this.m11 = m11;
    }

    public Matrix2Double() {
        this(1, 0,
                0, 1);
    }

    public Matrix2Double(double scalar) {
        this(scalar, 0,
                0, scalar);
    }

    public Matrix2Double(Matrix2Double other) {
        this(other.m00, other.m01,
                other.m10, other.m11);
    }

    public Matrix2Double(double[] array) {
        this(
                array[0], array[1],
                array[2], array[3]);

        if (array == null || array.length != 9) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix3 array must have exactly 9 elements");
    }

    // Set \\

    public Matrix2Double set(
            double m00, double m01,
            double m10, double m11) {

        this.m00 = m00;
        this.m01 = m01;

        this.m10 = m10;
        this.m11 = m11;

        return this;
    }

    public Matrix2Double set(double scalar) {
        return set(
                scalar, 0,
                0, scalar);
    }

    public Matrix2Double set(Matrix2Double other) {
        return set(
                other.m00, other.m01,
                other.m10, other.m11);
    }

    // Addition \\

    public Matrix2Double add(
            double m00, double m01,
            double m10, double m11) {

        this.m00 += m00;
        this.m01 += m01;

        this.m10 += m10;
        this.m11 += m11;

        return this;
    }

    public Matrix2Double add(double scalar) {
        return add(
                scalar, scalar,
                scalar, scalar);
    }

    public Matrix2Double add(Matrix2Double other) {
        return add(
                other.m00, other.m01,
                other.m10, other.m11);
    }

    // Subtraction \\

    public Matrix2Double subtract(
            double m00, double m01,
            double m10, double m11) {

        this.m00 -= m00;
        this.m01 -= m01;

        this.m10 -= m10;
        this.m11 -= m11;

        return this;
    }

    public Matrix2Double subtract(double scalar) {
        return subtract(
                scalar, scalar,
                scalar, scalar);
    }

    public Matrix2Double subtract(Matrix2Double other) {
        return subtract(
                other.m00, other.m01,
                other.m10, other.m11);
    }

    // Multiplication \\

    public Matrix2Double multiply(Matrix2Double other) {

        // Prevent self-multiply corruption
        double a00 = m00, a01 = m01;
        double a10 = m10, a11 = m11;

        double r00 = a00 * other.m00 + a01 * other.m10;
        double r01 = a00 * other.m01 + a01 * other.m11;

        double r10 = a10 * other.m00 + a11 * other.m10;
        double r11 = a10 * other.m01 + a11 * other.m11;

        m00 = r00;
        m01 = r01;
        m10 = r10;
        m11 = r11;

        return this;
    }

    // Scalar Multiplication \\

    public Matrix2Double multiply(double s) {

        m00 *= s;
        m01 *= s;
        m10 *= s;
        m11 *= s;

        return this;
    }

    // Division \\

    public Matrix2Double divide(Matrix2Double other) {

        Matrix2Double inv = new Matrix2Double(other).inverse();

        return multiply(inv);
    }

    public Matrix2Double divide(double scalar) {

        if (scalar == 0) // TODO: Add my own error
            throw new ArithmeticException("Division by zero");

        return multiply(1.0f / scalar);
    }

    // Inversion \\

    public Matrix2Double inverse() {

        double a00 = m00, a01 = m01;
        double a10 = m10, a11 = m11;

        double det = a00 * a11 - a01 * a10;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        double invDet = 1.0f / det;

        double r00 = a11 * invDet;
        double r01 = -a01 * invDet;
        double r10 = -a10 * invDet;
        double r11 = a00 * invDet;

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
        if (obj instanceof Matrix2Double) {
            Matrix2Double other = (Matrix2Double) obj;
            return m00 == other.m00 && m01 == other.m01 &&
                    m10 == other.m10 && m11 == other.m11;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int r = 17;

        r = 31 * r + Double.hashCode(m00);
        r = 31 * r + Double.hashCode(m01);

        r = 31 * r + Double.hashCode(m10);
        r = 31 * r + Double.hashCode(m11);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix2Double(" +
                "[" + m00 + ", " + m01 + "], " +
                "[" + m10 + ", " + m11 + "]" +
                ")";
    }
}
