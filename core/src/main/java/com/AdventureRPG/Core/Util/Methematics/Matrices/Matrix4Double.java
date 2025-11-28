package com.AdventureRPG.Core.Util.Methematics.Matrices;

public class Matrix4Double {

    // Data
    public double m00, m01, m02, m03;
    public double m10, m11, m12, m13;
    public double m20, m21, m22, m23;
    public double m30, m31, m32, m33;

    // Constructors \\

    public Matrix4Double(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;

        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
    }

    public Matrix4Double() {
        this(1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }

    public Matrix4Double(double scalar) {
        this(scalar, 0, 0, 0,
                0, scalar, 0, 0,
                0, 0, scalar, 0,
                0, 0, 0, scalar);
    }

    public Matrix4Double(Matrix4Double other) {
        this(other.m00, other.m01, other.m02, other.m03,
                other.m10, other.m11, other.m12, other.m13,
                other.m20, other.m21, other.m22, other.m23,
                other.m30, other.m31, other.m32, other.m33);
    }

    public Matrix4Double(double[] array) {
        this(
                array[0], array[1], array[2], array[3],
                array[4], array[5], array[6], array[7],
                array[8], array[9], array[10], array[11],
                array[12], array[13], array[14], array[15]);

        if (array == null || array.length != 16) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix4 array must have exactly 16 elements");
    }

    // Set \\

    public Matrix4Double set(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;

        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;

        return this;
    }

    public Matrix4Double set(double scalar) {
        return set(
                scalar, 0, 0, 0,
                0, scalar, 0, 0,
                0, 0, scalar, 0,
                0, 0, 0, scalar);
    }

    public Matrix4Double set(Matrix4Double other) {
        return set(
                other.m00, other.m01, other.m02, other.m03,
                other.m10, other.m11, other.m12, other.m13,
                other.m20, other.m21, other.m22, other.m23,
                other.m30, other.m31, other.m32, other.m33);
    }

    // Addition \\

    public Matrix4Double add(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {

        this.m00 += m00;
        this.m01 += m01;
        this.m02 += m02;
        this.m03 += m03;

        this.m10 += m10;
        this.m11 += m11;
        this.m12 += m12;
        this.m13 += m13;

        this.m20 += m20;
        this.m21 += m21;
        this.m22 += m22;
        this.m23 += m23;

        this.m30 += m30;
        this.m31 += m31;
        this.m32 += m32;
        this.m33 += m33;

        return this;
    }

    public Matrix4Double add(double scalar) {
        return add(
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar);
    }

    public Matrix4Double add(Matrix4Double other) {
        return add(
                other.m00, other.m01, other.m02, other.m03,
                other.m10, other.m11, other.m12, other.m13,
                other.m20, other.m21, other.m22, other.m23,
                other.m30, other.m31, other.m32, other.m33);
    }

    // Subtraction \\

    public Matrix4Double subtract(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {

        this.m00 -= m00;
        this.m01 -= m01;
        this.m02 -= m02;
        this.m03 -= m03;

        this.m10 -= m10;
        this.m11 -= m11;
        this.m12 -= m12;
        this.m13 -= m13;

        this.m20 -= m20;
        this.m21 -= m21;
        this.m22 -= m22;
        this.m23 -= m23;

        this.m30 -= m30;
        this.m31 -= m31;
        this.m32 -= m32;
        this.m33 -= m33;

        return this;
    }

    public Matrix4Double subtract(double scalar) {
        return subtract(
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar);
    }

    public Matrix4Double subtract(Matrix4Double other) {
        return subtract(
                other.m00, other.m01, other.m02, other.m03,
                other.m10, other.m11, other.m12, other.m13,
                other.m20, other.m21, other.m22, other.m23,
                other.m30, other.m31, other.m32, other.m33);
    }

    // Multiplication \\

    public Matrix4Double multiply(Matrix4Double other) {

        // Prevent self-multiply corruption
        double a00 = m00, a01 = m01, a02 = m02, a03 = m03;
        double a10 = m10, a11 = m11, a12 = m12, a13 = m13;
        double a20 = m20, a21 = m21, a22 = m22, a23 = m23;
        double a30 = m30, a31 = m31, a32 = m32, a33 = m33;

        double r00 = a00 * other.m00 + a01 * other.m10 + a02 * other.m20 + a03 * other.m30;
        double r01 = a00 * other.m01 + a01 * other.m11 + a02 * other.m21 + a03 * other.m31;
        double r02 = a00 * other.m02 + a01 * other.m12 + a02 * other.m22 + a03 * other.m32;
        double r03 = a00 * other.m03 + a01 * other.m13 + a02 * other.m23 + a03 * other.m33;

        double r10 = a10 * other.m00 + a11 * other.m10 + a12 * other.m20 + a13 * other.m30;
        double r11 = a10 * other.m01 + a11 * other.m11 + a12 * other.m21 + a13 * other.m31;
        double r12 = a10 * other.m02 + a11 * other.m12 + a12 * other.m22 + a13 * other.m32;
        double r13 = a10 * other.m03 + a11 * other.m13 + a12 * other.m23 + a13 * other.m33;

        double r20 = a20 * other.m00 + a21 * other.m10 + a22 * other.m20 + a23 * other.m30;
        double r21 = a20 * other.m01 + a21 * other.m11 + a22 * other.m21 + a23 * other.m31;
        double r22 = a20 * other.m02 + a21 * other.m12 + a22 * other.m22 + a23 * other.m32;
        double r23 = a20 * other.m03 + a21 * other.m13 + a22 * other.m23 + a23 * other.m33;

        double r30 = a30 * other.m00 + a31 * other.m10 + a32 * other.m20 + a33 * other.m30;
        double r31 = a30 * other.m01 + a31 * other.m11 + a32 * other.m21 + a33 * other.m31;
        double r32 = a30 * other.m02 + a31 * other.m12 + a32 * other.m22 + a33 * other.m32;
        double r33 = a30 * other.m03 + a31 * other.m13 + a32 * other.m23 + a33 * other.m33;

        m00 = r00;
        m01 = r01;
        m02 = r02;
        m03 = r03;
        m10 = r10;
        m11 = r11;
        m12 = r12;
        m13 = r13;
        m20 = r20;
        m21 = r21;
        m22 = r22;
        m23 = r23;
        m30 = r30;
        m31 = r31;
        m32 = r32;
        m33 = r33;

        return this;
    }

    // Scalar Multiplication \\

    public Matrix4Double multiply(double s) {

        m00 *= s;
        m01 *= s;
        m02 *= s;
        m03 *= s;
        m10 *= s;
        m11 *= s;
        m12 *= s;
        m13 *= s;
        m20 *= s;
        m21 *= s;
        m22 *= s;
        m23 *= s;
        m30 *= s;
        m31 *= s;
        m32 *= s;
        m33 *= s;

        return this;
    }

    // Division \\

    public Matrix4Double divide(Matrix4Double other) {

        Matrix4Double inv = new Matrix4Double(other).inverse();

        return multiply(inv);
    }

    public Matrix4Double divide(double scalar) {

        if (scalar == 0) // TODO: Add my own error
            throw new ArithmeticException("Division by zero");

        return multiply(1.0f / scalar);
    }

    // Inversion \\

    public Matrix4Double inverse() {

        double a00 = m00, a01 = m01, a02 = m02, a03 = m03;
        double a10 = m10, a11 = m11, a12 = m12, a13 = m13;
        double a20 = m20, a21 = m21, a22 = m22, a23 = m23;
        double a30 = m30, a31 = m31, a32 = m32, a33 = m33;

        double b00 = a00 * a11 - a01 * a10;
        double b01 = a00 * a12 - a02 * a10;
        double b02 = a00 * a13 - a03 * a10;
        double b03 = a01 * a12 - a02 * a11;
        double b04 = a01 * a13 - a03 * a11;
        double b05 = a02 * a13 - a03 * a12;
        double b06 = a20 * a31 - a21 * a30;
        double b07 = a20 * a32 - a22 * a30;
        double b08 = a20 * a33 - a23 * a30;
        double b09 = a21 * a32 - a22 * a31;
        double b10 = a21 * a33 - a23 * a31;
        double b11 = a22 * a33 - a23 * a32;

        double det = b00 * b11 - b01 * b10 + b02 * b09 +
                b03 * b08 - b04 * b07 + b05 * b06;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        double invDet = 1.0f / det;

        double r00 = (a11 * b11 - a12 * b10 + a13 * b09) * invDet;
        double r01 = (-a01 * b11 + a02 * b10 - a03 * b09) * invDet;
        double r02 = (a31 * b05 - a32 * b04 + a33 * b03) * invDet;
        double r03 = (-a21 * b05 + a22 * b04 - a23 * b03) * invDet;

        double r10 = (-a10 * b11 + a12 * b08 - a13 * b07) * invDet;
        double r11 = (a00 * b11 - a02 * b08 + a03 * b07) * invDet;
        double r12 = (-a30 * b05 + a32 * b02 - a33 * b01) * invDet;
        double r13 = (a20 * b05 - a22 * b02 + a23 * b01) * invDet;

        double r20 = (a10 * b10 - a11 * b08 + a13 * b06) * invDet;
        double r21 = (-a00 * b10 + a01 * b08 - a03 * b06) * invDet;
        double r22 = (a30 * b04 - a31 * b02 + a33 * b00) * invDet;
        double r23 = (-a20 * b04 + a21 * b02 - a23 * b00) * invDet;

        double r30 = (-a10 * b09 + a11 * b07 - a12 * b06) * invDet;
        double r31 = (a00 * b09 - a01 * b07 + a02 * b06) * invDet;
        double r32 = (-a30 * b03 + a31 * b01 - a32 * b00) * invDet;
        double r33 = (a20 * b03 - a21 * b01 + a22 * b00) * invDet;

        m00 = r00;
        m01 = r01;
        m02 = r02;
        m03 = r03;
        m10 = r10;
        m11 = r11;
        m12 = r12;
        m13 = r13;
        m20 = r20;
        m21 = r21;
        m22 = r22;
        m23 = r23;
        m30 = r30;
        m31 = r31;
        m32 = r32;
        m33 = r33;

        return this;
    }

    // Utility \\

    public boolean hasValues() {
        return m00 != 0 || m01 != 0 || m02 != 0 || m03 != 0 ||
                m10 != 0 || m11 != 0 || m12 != 0 || m13 != 0 ||
                m20 != 0 || m21 != 0 || m22 != 0 || m23 != 0 ||
                m30 != 0 || m31 != 0 || m32 != 0 || m33 != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Matrix4Double) {
            Matrix4Double other = (Matrix4Double) obj;
            return m00 == other.m00 && m01 == other.m01 && m02 == other.m02 && m03 == other.m03 &&
                    m10 == other.m10 && m11 == other.m11 && m12 == other.m12 && m13 == other.m13 &&
                    m20 == other.m20 && m21 == other.m21 && m22 == other.m22 && m23 == other.m23 &&
                    m30 == other.m30 && m31 == other.m31 && m32 == other.m32 && m33 == other.m33;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int r = 17;

        r = 31 * r + Double.hashCode(m00);
        r = 31 * r + Double.hashCode(m01);
        r = 31 * r + Double.hashCode(m02);
        r = 31 * r + Double.hashCode(m03);

        r = 31 * r + Double.hashCode(m10);
        r = 31 * r + Double.hashCode(m11);
        r = 31 * r + Double.hashCode(m12);
        r = 31 * r + Double.hashCode(m13);

        r = 31 * r + Double.hashCode(m20);
        r = 31 * r + Double.hashCode(m21);
        r = 31 * r + Double.hashCode(m22);
        r = 31 * r + Double.hashCode(m23);

        r = 31 * r + Double.hashCode(m30);
        r = 31 * r + Double.hashCode(m31);
        r = 31 * r + Double.hashCode(m32);
        r = 31 * r + Double.hashCode(m33);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix4Double(" +
                "[" + m00 + ", " + m01 + ", " + m02 + ", " + m03 + "], " +
                "[" + m10 + ", " + m11 + ", " + m12 + ", " + m13 + "], " +
                "[" + m20 + ", " + m21 + ", " + m22 + ", " + m23 + "], " +
                "[" + m30 + ", " + m31 + ", " + m32 + ", " + m33 + "]" +
                ")";
    }
}
