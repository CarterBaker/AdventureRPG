package com.AdventureRPG.core.util.Mathematics.Matrices;

public class Matrix3Double {

    // Data
    public double m00, m01, m02;
    public double m10, m11, m12;
    public double m20, m21, m22;

    // Constructors \\

    public Matrix3Double(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    public Matrix3Double() {
        this(1, 0, 0,
                0, 1, 0,
                0, 0, 1);
    }

    public Matrix3Double(double scalar) {
        this(scalar, 0, 0,
                0, scalar, 0,
                0, 0, scalar);
    }

    public Matrix3Double(Matrix3Double other) {
        this(other.m00, other.m01, other.m02,
                other.m10, other.m11, other.m12,
                other.m20, other.m21, other.m22);
    }

    public Matrix3Double(double[] array) {
        this(
                array[0], array[1], array[2],
                array[3], array[4], array[5],
                array[6], array[7], array[8]);

        if (array == null || array.length != 9) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix3 array must have exactly 9 elements");
    }

    // Set \\

    public Matrix3Double set(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;

        return this;
    }

    public Matrix3Double set(double scalar) {
        return set(
                scalar, 0, 0,
                0, scalar, 0,
                0, 0, scalar);
    }

    public Matrix3Double set(Matrix3Double other) {
        return set(
                other.m00, other.m01, other.m02,
                other.m10, other.m11, other.m12,
                other.m20, other.m21, other.m22);
    }

    // Addition \\

    public Matrix3Double add(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {

        this.m00 += m00;
        this.m01 += m01;
        this.m02 += m02;

        this.m10 += m10;
        this.m11 += m11;
        this.m12 += m12;

        this.m20 += m20;
        this.m21 += m21;
        this.m22 += m22;

        return this;
    }

    public Matrix3Double add(double scalar) {
        return add(
                scalar, scalar, scalar,
                scalar, scalar, scalar,
                scalar, scalar, scalar);
    }

    public Matrix3Double add(Matrix3Double other) {
        return add(
                other.m00, other.m01, other.m02,
                other.m10, other.m11, other.m12,
                other.m20, other.m21, other.m22);
    }

    // Subtraction \\

    public Matrix3Double subtract(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {

        this.m00 -= m00;
        this.m01 -= m01;
        this.m02 -= m02;

        this.m10 -= m10;
        this.m11 -= m11;
        this.m12 -= m12;

        this.m20 -= m20;
        this.m21 -= m21;
        this.m22 -= m22;

        return this;
    }

    public Matrix3Double subtract(double scalar) {
        return subtract(
                scalar, scalar, scalar,
                scalar, scalar, scalar,
                scalar, scalar, scalar);
    }

    public Matrix3Double subtract(Matrix3Double other) {
        return subtract(
                other.m00, other.m01, other.m02,
                other.m10, other.m11, other.m12,
                other.m20, other.m21, other.m22);
    }

    // Multiplication \\

    public Matrix3Double multiply(Matrix3Double other) {

        // Prevent self-multiply corruption
        double a00 = m00, a01 = m01, a02 = m02;
        double a10 = m10, a11 = m11, a12 = m12;
        double a20 = m20, a21 = m21, a22 = m22;

        double r00 = a00 * other.m00 + a01 * other.m10 + a02 * other.m20;
        double r01 = a00 * other.m01 + a01 * other.m11 + a02 * other.m21;
        double r02 = a00 * other.m02 + a01 * other.m12 + a02 * other.m22;

        double r10 = a10 * other.m00 + a11 * other.m10 + a12 * other.m20;
        double r11 = a10 * other.m01 + a11 * other.m11 + a12 * other.m21;
        double r12 = a10 * other.m02 + a11 * other.m12 + a12 * other.m22;

        double r20 = a20 * other.m00 + a21 * other.m10 + a22 * other.m20;
        double r21 = a20 * other.m01 + a21 * other.m11 + a22 * other.m21;
        double r22 = a20 * other.m02 + a21 * other.m12 + a22 * other.m22;

        m00 = r00;
        m01 = r01;
        m02 = r02;
        m10 = r10;
        m11 = r11;
        m12 = r12;
        m20 = r20;
        m21 = r21;
        m22 = r22;

        return this;
    }

    // Scalar Multiplication \\

    public Matrix3Double multiply(double s) {

        m00 *= s;
        m01 *= s;
        m02 *= s;
        m10 *= s;
        m11 *= s;
        m12 *= s;
        m20 *= s;
        m21 *= s;
        m22 *= s;

        return this;
    }

    // Division \\

    public Matrix3Double divide(Matrix3Double other) {

        Matrix3Double inv = new Matrix3Double(other).inverse();

        return multiply(inv);
    }

    public Matrix3Double divide(double scalar) {

        if (scalar == 0) // TODO: Add my own error
            throw new ArithmeticException("Division by zero");

        return multiply(1.0f / scalar);
    }

    // Inversion \\

    public Matrix3Double inverse() {

        double a00 = m00, a01 = m01, a02 = m02;
        double a10 = m10, a11 = m11, a12 = m12;
        double a20 = m20, a21 = m21, a22 = m22;

        double det = a00 * (a11 * a22 - a12 * a21) -
                a01 * (a10 * a22 - a12 * a20) +
                a02 * (a10 * a21 - a11 * a20);

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        double invDet = 1.0f / det;

        double r00 = (a11 * a22 - a12 * a21) * invDet;
        double r01 = (a02 * a21 - a01 * a22) * invDet;
        double r02 = (a01 * a12 - a02 * a11) * invDet;

        double r10 = (a12 * a20 - a10 * a22) * invDet;
        double r11 = (a00 * a22 - a02 * a20) * invDet;
        double r12 = (a02 * a10 - a00 * a12) * invDet;

        double r20 = (a10 * a21 - a11 * a20) * invDet;
        double r21 = (a01 * a20 - a00 * a21) * invDet;
        double r22 = (a00 * a11 - a01 * a10) * invDet;

        m00 = r00;
        m01 = r01;
        m02 = r02;
        m10 = r10;
        m11 = r11;
        m12 = r12;
        m20 = r20;
        m21 = r21;
        m22 = r22;

        return this;
    }

    // Utility \\

    public boolean hasValues() {
        return m00 != 0 || m01 != 0 || m02 != 0 ||
                m10 != 0 || m11 != 0 || m12 != 0 ||
                m20 != 0 || m21 != 0 || m22 != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Matrix3Double) {
            Matrix3Double other = (Matrix3Double) obj;
            return m00 == other.m00 && m01 == other.m01 && m02 == other.m02 &&
                    m10 == other.m10 && m11 == other.m11 && m12 == other.m12 &&
                    m20 == other.m20 && m21 == other.m21 && m22 == other.m22;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int r = 17;

        r = 31 * r + Double.hashCode(m00);
        r = 31 * r + Double.hashCode(m01);
        r = 31 * r + Double.hashCode(m02);

        r = 31 * r + Double.hashCode(m10);
        r = 31 * r + Double.hashCode(m11);
        r = 31 * r + Double.hashCode(m12);

        r = 31 * r + Double.hashCode(m20);
        r = 31 * r + Double.hashCode(m21);
        r = 31 * r + Double.hashCode(m22);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix3Double(" +
                "[" + m00 + ", " + m01 + ", " + m02 + "], " +
                "[" + m10 + ", " + m11 + ", " + m12 + "], " +
                "[" + m20 + ", " + m21 + ", " + m22 + "]" +
                ")";
    }
}
