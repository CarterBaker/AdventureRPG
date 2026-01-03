package com.AdventureRPG.core.util.mathematics.matrices;

public class Matrix3Double {

    // Data
    public final double[] val = new double[9];

    // Constructors \\

    public Matrix3Double(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {
        val[0] = m00;
        val[1] = m10;
        val[2] = m20;
        val[3] = m01;
        val[4] = m11;
        val[5] = m21;
        val[6] = m02;
        val[7] = m12;
        val[8] = m22;
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
        System.arraycopy(other.val, 0, val, 0, 9);
    }

    public Matrix3Double(double[] array) {

        if (array == null || array.length != 9) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix3Double array must have exactly 9 elements");

        // Row-major order [m00, m01, m02, m10, m11, m12, m20, m21, m22]
        val[0] = array[0]; // m00
        val[1] = array[3]; // m10
        val[2] = array[6]; // m20
        val[3] = array[1]; // m01
        val[4] = array[4]; // m11
        val[5] = array[7]; // m21
        val[6] = array[2]; // m02
        val[7] = array[5]; // m12
        val[8] = array[8]; // m22
    }

    // Accessors \\

    public double getM00() {
        return val[0];
    }

    public double getM10() {
        return val[1];
    }

    public double getM20() {
        return val[2];
    }

    public double getM01() {
        return val[3];
    }

    public double getM11() {
        return val[4];
    }

    public double getM21() {
        return val[5];
    }

    public double getM02() {
        return val[6];
    }

    public double getM12() {
        return val[7];
    }

    public double getM22() {
        return val[8];
    }

    public void setM00(double value) {
        val[0] = value;
    }

    public void setM10(double value) {
        val[1] = value;
    }

    public void setM20(double value) {
        val[2] = value;
    }

    public void setM01(double value) {
        val[3] = value;
    }

    public void setM11(double value) {
        val[4] = value;
    }

    public void setM21(double value) {
        val[5] = value;
    }

    public void setM02(double value) {
        val[6] = value;
    }

    public void setM12(double value) {
        val[7] = value;
    }

    public void setM22(double value) {
        val[8] = value;
    }

    // Set \\

    public Matrix3Double set(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {

        val[0] = m00;
        val[1] = m10;
        val[2] = m20;
        val[3] = m01;
        val[4] = m11;
        val[5] = m21;
        val[6] = m02;
        val[7] = m12;
        val[8] = m22;

        return this;
    }

    public Matrix3Double set(double scalar) {
        return set(
                scalar, 0, 0,
                0, scalar, 0,
                0, 0, scalar);
    }

    public Matrix3Double set(Matrix3Double other) {

        System.arraycopy(other.val, 0, val, 0, 9);

        return this;
    }

    // Addition \\

    public Matrix3Double add(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {

        val[0] += m00;
        val[1] += m10;
        val[2] += m20;
        val[3] += m01;
        val[4] += m11;
        val[5] += m21;
        val[6] += m02;
        val[7] += m12;
        val[8] += m22;

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
                other.val[0], other.val[3], other.val[6],
                other.val[1], other.val[4], other.val[7],
                other.val[2], other.val[5], other.val[8]);
    }

    // Subtraction \\

    public Matrix3Double subtract(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {

        val[0] -= m00;
        val[1] -= m10;
        val[2] -= m20;
        val[3] -= m01;
        val[4] -= m11;
        val[5] -= m21;
        val[6] -= m02;
        val[7] -= m12;
        val[8] -= m22;

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
                other.val[0], other.val[3], other.val[6],
                other.val[1], other.val[4], other.val[7],
                other.val[2], other.val[5], other.val[8]);
    }

    // Multiplication \\

    public Matrix3Double multiply(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {

        // Extract original values to prevent self-overwrite
        double a00 = val[0], a10 = val[1], a20 = val[2];
        double a01 = val[3], a11 = val[4], a21 = val[5];
        double a02 = val[6], a12 = val[7], a22 = val[8];

        double r00 = a00 * m00 + a01 * m10 + a02 * m20;
        double r10 = a10 * m00 + a11 * m10 + a12 * m20;
        double r20 = a20 * m00 + a21 * m10 + a22 * m20;

        double r01 = a00 * m01 + a01 * m11 + a02 * m21;
        double r11 = a10 * m01 + a11 * m11 + a12 * m21;
        double r21 = a20 * m01 + a21 * m11 + a22 * m21;

        double r02 = a00 * m02 + a01 * m12 + a02 * m22;
        double r12 = a10 * m02 + a11 * m12 + a12 * m22;
        double r22 = a20 * m02 + a21 * m12 + a22 * m22;

        val[0] = r00;
        val[1] = r10;
        val[2] = r20;
        val[3] = r01;
        val[4] = r11;
        val[5] = r21;
        val[6] = r02;
        val[7] = r12;
        val[8] = r22;

        return this;
    }

    public Matrix3Double multiply(double scalar) {
        return multiply(
                scalar, 0, 0,
                0, scalar, 0,
                0, 0, scalar);
    }

    public Matrix3Double multiply(Matrix3Double other) {
        return multiply(
                other.val[0], other.val[3], other.val[6],
                other.val[1], other.val[4], other.val[7],
                other.val[2], other.val[5], other.val[8]);
    }

    // Division \\

    public Matrix3Double divide(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {

        // Compute determinant
        double det = m00 * (m11 * m22 - m12 * m21) -
                m01 * (m10 * m22 - m12 * m20) +
                m02 * (m10 * m21 - m11 * m20);

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        double invDet = 1.0 / det;

        // Invert the inputs
        double i00 = (m11 * m22 - m12 * m21) * invDet;
        double i01 = (m02 * m21 - m01 * m22) * invDet;
        double i02 = (m01 * m12 - m02 * m11) * invDet;

        double i10 = (m12 * m20 - m10 * m22) * invDet;
        double i11 = (m00 * m22 - m02 * m20) * invDet;
        double i12 = (m02 * m10 - m00 * m12) * invDet;

        double i20 = (m10 * m21 - m11 * m20) * invDet;
        double i21 = (m01 * m20 - m00 * m21) * invDet;
        double i22 = (m00 * m11 - m01 * m10) * invDet;

        // Multiply current matrix by the inverted values using master multiply
        return multiply(i00, i01, i02, i10, i11, i12, i20, i21, i22);
    }

    public Matrix3Double divide(double scalar) {
        if (scalar == 0)
            throw new ArithmeticException("Division by zero");
        return multiply(1.0 / scalar, 0, 0, 0, 1.0 / scalar, 0, 0, 0, 1.0 / scalar);
    }

    public Matrix3Double divide(Matrix3Double other) {
        return divide(
                other.val[0], other.val[3], other.val[6],
                other.val[1], other.val[4], other.val[7],
                other.val[2], other.val[5], other.val[8]);
    }

    // Inversion \\

    public Matrix3Double inverse() {

        double a00 = val[0], a10 = val[1], a20 = val[2];
        double a01 = val[3], a11 = val[4], a21 = val[5];
        double a02 = val[6], a12 = val[7], a22 = val[8];

        double det = a00 * (a11 * a22 - a12 * a21) -
                a01 * (a10 * a22 - a12 * a20) +
                a02 * (a10 * a21 - a11 * a20);

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        double invDet = 1.0 / det;

        val[0] = (a11 * a22 - a12 * a21) * invDet;
        val[1] = (a12 * a20 - a10 * a22) * invDet;
        val[2] = (a10 * a21 - a11 * a20) * invDet;
        val[3] = (a02 * a21 - a01 * a22) * invDet;
        val[4] = (a00 * a22 - a02 * a20) * invDet;
        val[5] = (a01 * a20 - a00 * a21) * invDet;
        val[6] = (a01 * a12 - a02 * a11) * invDet;
        val[7] = (a02 * a10 - a00 * a12) * invDet;
        val[8] = (a00 * a11 - a01 * a10) * invDet;

        return this;
    }

    // Utility \\

    public boolean hasValues() {
        return val[0] != 0 || val[1] != 0 || val[2] != 0 ||
                val[3] != 0 || val[4] != 0 || val[5] != 0 ||
                val[6] != 0 || val[7] != 0 || val[8] != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Matrix3Double other)
            return val[0] == other.val[0] && val[1] == other.val[1] && val[2] == other.val[2] &&
                    val[3] == other.val[3] && val[4] == other.val[4] && val[5] == other.val[5] &&
                    val[6] == other.val[6] && val[7] == other.val[7] && val[8] == other.val[8];

        return false;
    }

    @Override
    public int hashCode() {

        int r = 17;

        r = 31 * r + Double.hashCode(val[0]);
        r = 31 * r + Double.hashCode(val[1]);
        r = 31 * r + Double.hashCode(val[2]);
        r = 31 * r + Double.hashCode(val[3]);
        r = 31 * r + Double.hashCode(val[4]);
        r = 31 * r + Double.hashCode(val[5]);
        r = 31 * r + Double.hashCode(val[6]);
        r = 31 * r + Double.hashCode(val[7]);
        r = 31 * r + Double.hashCode(val[8]);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix3Double(" +
                "[" + val[0] + ", " + val[3] + ", " + val[6] + "], " +
                "[" + val[1] + ", " + val[4] + ", " + val[7] + "], " +
                "[" + val[2] + ", " + val[5] + ", " + val[8] + "]" +
                ")";
    }
}