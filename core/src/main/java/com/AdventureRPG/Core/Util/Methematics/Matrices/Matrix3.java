package com.AdventureRPG.Core.Util.Methematics.Matrices;

public class Matrix3 {

    // Data
    public float m00, m01, m02;
    public float m10, m11, m12;
    public float m20, m21, m22;

    // Constructors \\

    public Matrix3(
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22) {

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

    public Matrix3() {
        this(1, 0, 0,
                0, 1, 0,
                0, 0, 1);
    }

    public Matrix3(float scalar) {
        this(scalar, 0, 0,
                0, scalar, 0,
                0, 0, scalar);
    }

    public Matrix3(Matrix3 other) {
        this(other.m00, other.m01, other.m02,
                other.m10, other.m11, other.m12,
                other.m20, other.m21, other.m22);
    }

    public Matrix3(float[] array) {
        this(
                array[0], array[1], array[2],
                array[3], array[4], array[5],
                array[6], array[7], array[8]);

        if (array == null || array.length != 9) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix3 array must have exactly 9 elements");
    }

    // Set \\

    public Matrix3 set(
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22) {

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

    public Matrix3 set(float scalar) {
        return set(
                scalar, 0, 0,
                0, scalar, 0,
                0, 0, scalar);
    }

    public Matrix3 set(Matrix3 other) {
        return set(
                other.m00, other.m01, other.m02,
                other.m10, other.m11, other.m12,
                other.m20, other.m21, other.m22);
    }

    // Addition \\

    public Matrix3 add(
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22) {

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

    public Matrix3 add(float scalar) {
        return add(
                scalar, scalar, scalar,
                scalar, scalar, scalar,
                scalar, scalar, scalar);
    }

    public Matrix3 add(Matrix3 other) {
        return add(
                other.m00, other.m01, other.m02,
                other.m10, other.m11, other.m12,
                other.m20, other.m21, other.m22);
    }

    // Subtraction \\

    public Matrix3 subtract(
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22) {

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

    public Matrix3 subtract(float scalar) {
        return subtract(
                scalar, scalar, scalar,
                scalar, scalar, scalar,
                scalar, scalar, scalar);
    }

    public Matrix3 subtract(Matrix3 other) {
        return subtract(
                other.m00, other.m01, other.m02,
                other.m10, other.m11, other.m12,
                other.m20, other.m21, other.m22);
    }

    // Multiplication \\

    public Matrix3 multiply(Matrix3 other) {

        // Prevent self-multiply corruption
        float a00 = m00, a01 = m01, a02 = m02;
        float a10 = m10, a11 = m11, a12 = m12;
        float a20 = m20, a21 = m21, a22 = m22;

        float r00 = a00 * other.m00 + a01 * other.m10 + a02 * other.m20;
        float r01 = a00 * other.m01 + a01 * other.m11 + a02 * other.m21;
        float r02 = a00 * other.m02 + a01 * other.m12 + a02 * other.m22;

        float r10 = a10 * other.m00 + a11 * other.m10 + a12 * other.m20;
        float r11 = a10 * other.m01 + a11 * other.m11 + a12 * other.m21;
        float r12 = a10 * other.m02 + a11 * other.m12 + a12 * other.m22;

        float r20 = a20 * other.m00 + a21 * other.m10 + a22 * other.m20;
        float r21 = a20 * other.m01 + a21 * other.m11 + a22 * other.m21;
        float r22 = a20 * other.m02 + a21 * other.m12 + a22 * other.m22;

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

    public Matrix3 multiply(float s) {

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

    public Matrix3 divide(Matrix3 other) {

        Matrix3 inv = new Matrix3(other).inverse();

        return multiply(inv);
    }

    public Matrix3 divide(float scalar) {

        if (scalar == 0) // TODO: Add my own error
            throw new ArithmeticException("Division by zero");

        return multiply(1.0f / scalar);
    }

    // Inversion \\

    public Matrix3 inverse() {

        float a00 = m00, a01 = m01, a02 = m02;
        float a10 = m10, a11 = m11, a12 = m12;
        float a20 = m20, a21 = m21, a22 = m22;

        float det = a00 * (a11 * a22 - a12 * a21) -
                a01 * (a10 * a22 - a12 * a20) +
                a02 * (a10 * a21 - a11 * a20);

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        float invDet = 1.0f / det;

        float r00 = (a11 * a22 - a12 * a21) * invDet;
        float r01 = (a02 * a21 - a01 * a22) * invDet;
        float r02 = (a01 * a12 - a02 * a11) * invDet;

        float r10 = (a12 * a20 - a10 * a22) * invDet;
        float r11 = (a00 * a22 - a02 * a20) * invDet;
        float r12 = (a02 * a10 - a00 * a12) * invDet;

        float r20 = (a10 * a21 - a11 * a20) * invDet;
        float r21 = (a01 * a20 - a00 * a21) * invDet;
        float r22 = (a00 * a11 - a01 * a10) * invDet;

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
        if (obj instanceof Matrix3) {
            Matrix3 other = (Matrix3) obj;
            return m00 == other.m00 && m01 == other.m01 && m02 == other.m02 &&
                    m10 == other.m10 && m11 == other.m11 && m12 == other.m12 &&
                    m20 == other.m20 && m21 == other.m21 && m22 == other.m22;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int r = 17;

        r = 31 * r + Float.hashCode(m00);
        r = 31 * r + Float.hashCode(m01);
        r = 31 * r + Float.hashCode(m02);

        r = 31 * r + Float.hashCode(m10);
        r = 31 * r + Float.hashCode(m11);
        r = 31 * r + Float.hashCode(m12);

        r = 31 * r + Float.hashCode(m20);
        r = 31 * r + Float.hashCode(m21);
        r = 31 * r + Float.hashCode(m22);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix3(" +
                "[" + m00 + ", " + m01 + ", " + m02 + "], " +
                "[" + m10 + ", " + m11 + ", " + m12 + "], " +
                "[" + m20 + ", " + m21 + ", " + m22 + "]" +
                ")";
    }
}
