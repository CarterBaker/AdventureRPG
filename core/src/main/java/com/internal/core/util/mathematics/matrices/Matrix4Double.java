package com.internal.core.util.mathematics.matrices;

public class Matrix4Double {

    // Data
    public final double[] val = new double[16];

    // Constructors \\

    public Matrix4Double(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {
        val[0] = m00;
        val[1] = m10;
        val[2] = m20;
        val[3] = m30;
        val[4] = m01;
        val[5] = m11;
        val[6] = m21;
        val[7] = m31;
        val[8] = m02;
        val[9] = m12;
        val[10] = m22;
        val[11] = m32;
        val[12] = m03;
        val[13] = m13;
        val[14] = m23;
        val[15] = m33;
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
        System.arraycopy(other.val, 0, val, 0, 16);
    }

    public Matrix4Double(double[] array) {

        if (array == null || array.length != 16) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix4Double array must have exactly 16 elements");

        // Row-major order
        val[0] = array[0]; // m00
        val[1] = array[4]; // m10
        val[2] = array[8]; // m20
        val[3] = array[12]; // m30
        val[4] = array[1]; // m01
        val[5] = array[5]; // m11
        val[6] = array[9]; // m21
        val[7] = array[13]; // m31
        val[8] = array[2]; // m02
        val[9] = array[6]; // m12
        val[10] = array[10]; // m22
        val[11] = array[14]; // m32
        val[12] = array[3]; // m03
        val[13] = array[7]; // m13
        val[14] = array[11]; // m23
        val[15] = array[15]; // m33
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

    public double getM30() {
        return val[3];
    }

    public double getM01() {
        return val[4];
    }

    public double getM11() {
        return val[5];
    }

    public double getM21() {
        return val[6];
    }

    public double getM31() {
        return val[7];
    }

    public double getM02() {
        return val[8];
    }

    public double getM12() {
        return val[9];
    }

    public double getM22() {
        return val[10];
    }

    public double getM32() {
        return val[11];
    }

    public double getM03() {
        return val[12];
    }

    public double getM13() {
        return val[13];
    }

    public double getM23() {
        return val[14];
    }

    public double getM33() {
        return val[15];
    }

    public void setM00(double v) {
        val[0] = v;
    }

    public void setM10(double v) {
        val[1] = v;
    }

    public void setM20(double v) {
        val[2] = v;
    }

    public void setM30(double v) {
        val[3] = v;
    }

    public void setM01(double v) {
        val[4] = v;
    }

    public void setM11(double v) {
        val[5] = v;
    }

    public void setM21(double v) {
        val[6] = v;
    }

    public void setM31(double v) {
        val[7] = v;
    }

    public void setM02(double v) {
        val[8] = v;
    }

    public void setM12(double v) {
        val[9] = v;
    }

    public void setM22(double v) {
        val[10] = v;
    }

    public void setM32(double v) {
        val[11] = v;
    }

    public void setM03(double v) {
        val[12] = v;
    }

    public void setM13(double v) {
        val[13] = v;
    }

    public void setM23(double v) {
        val[14] = v;
    }

    public void setM33(double v) {
        val[15] = v;
    }

    // Set \\

    public Matrix4Double set(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {

        val[0] = m00;
        val[1] = m10;
        val[2] = m20;
        val[3] = m30;
        val[4] = m01;
        val[5] = m11;
        val[6] = m21;
        val[7] = m31;
        val[8] = m02;
        val[9] = m12;
        val[10] = m22;
        val[11] = m32;
        val[12] = m03;
        val[13] = m13;
        val[14] = m23;
        val[15] = m33;

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

        System.arraycopy(other.val, 0, val, 0, 16);

        return this;
    }

    // Addition \\

    public Matrix4Double add(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {

        val[0] += m00;
        val[1] += m10;
        val[2] += m20;
        val[3] += m30;
        val[4] += m01;
        val[5] += m11;
        val[6] += m21;
        val[7] += m31;
        val[8] += m02;
        val[9] += m12;
        val[10] += m22;
        val[11] += m32;
        val[12] += m03;
        val[13] += m13;
        val[14] += m23;
        val[15] += m33;

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
                other.val[0], other.val[4], other.val[8], other.val[12],
                other.val[1], other.val[5], other.val[9], other.val[13],
                other.val[2], other.val[6], other.val[10], other.val[14],
                other.val[3], other.val[7], other.val[11], other.val[15]);
    }

    // Subtraction \\

    public Matrix4Double subtract(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {

        val[0] -= m00;
        val[1] -= m10;
        val[2] -= m20;
        val[3] -= m30;
        val[4] -= m01;
        val[5] -= m11;
        val[6] -= m21;
        val[7] -= m31;
        val[8] -= m02;
        val[9] -= m12;
        val[10] -= m22;
        val[11] -= m32;
        val[12] -= m03;
        val[13] -= m13;
        val[14] -= m23;
        val[15] -= m33;

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
                other.val[0], other.val[4], other.val[8], other.val[12],
                other.val[1], other.val[5], other.val[9], other.val[13],
                other.val[2], other.val[6], other.val[10], other.val[14],
                other.val[3], other.val[7], other.val[11], other.val[15]);
    }

    // Multiplication \\

    public Matrix4Double multiply(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {

        // Extract original values to prevent self-overwrite
        double a00 = val[0], a10 = val[1], a20 = val[2], a30 = val[3];
        double a01 = val[4], a11 = val[5], a21 = val[6], a31 = val[7];
        double a02 = val[8], a12 = val[9], a22 = val[10], a32 = val[11];
        double a03 = val[12], a13 = val[13], a23 = val[14], a33 = val[15];

        double r00 = a00 * m00 + a01 * m10 + a02 * m20 + a03 * m30;
        double r10 = a10 * m00 + a11 * m10 + a12 * m20 + a13 * m30;
        double r20 = a20 * m00 + a21 * m10 + a22 * m20 + a23 * m30;
        double r30 = a30 * m00 + a31 * m10 + a32 * m20 + a33 * m30;

        double r01 = a00 * m01 + a01 * m11 + a02 * m21 + a03 * m31;
        double r11 = a10 * m01 + a11 * m11 + a12 * m21 + a13 * m31;
        double r21 = a20 * m01 + a21 * m11 + a22 * m21 + a23 * m31;
        double r31 = a30 * m01 + a31 * m11 + a32 * m21 + a33 * m31;

        double r02 = a00 * m02 + a01 * m12 + a02 * m22 + a03 * m32;
        double r12 = a10 * m02 + a11 * m12 + a12 * m22 + a13 * m32;
        double r22 = a20 * m02 + a21 * m12 + a22 * m22 + a23 * m32;
        double r32 = a30 * m02 + a31 * m12 + a32 * m22 + a33 * m32;

        double r03 = a00 * m03 + a01 * m13 + a02 * m23 + a03 * m33;
        double r13 = a10 * m03 + a11 * m13 + a12 * m23 + a13 * m33;
        double r23 = a20 * m03 + a21 * m13 + a22 * m23 + a23 * m33;
        double r33 = a30 * m03 + a31 * m13 + a32 * m23 + a33 * m33;

        val[0] = r00;
        val[1] = r10;
        val[2] = r20;
        val[3] = r30;
        val[4] = r01;
        val[5] = r11;
        val[6] = r21;
        val[7] = r31;
        val[8] = r02;
        val[9] = r12;
        val[10] = r22;
        val[11] = r32;
        val[12] = r03;
        val[13] = r13;
        val[14] = r23;
        val[15] = r33;

        return this;
    }

    public Matrix4Double multiply(double scalar) {
        return multiply(
                scalar, 0, 0, 0,
                0, scalar, 0, 0,
                0, 0, scalar, 0,
                0, 0, 0, scalar);
    }

    public Matrix4Double multiply(Matrix4Double other) {
        return multiply(
                other.val[0], other.val[4], other.val[8], other.val[12],
                other.val[1], other.val[5], other.val[9], other.val[13],
                other.val[2], other.val[6], other.val[10], other.val[14],
                other.val[3], other.val[7], other.val[11], other.val[15]);
    }

    // Division \\

    public Matrix4Double divide(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33) {

        double b00 = m00 * m11 - m01 * m10;
        double b01 = m00 * m12 - m02 * m10;
        double b02 = m00 * m13 - m03 * m10;
        double b03 = m01 * m12 - m02 * m11;
        double b04 = m01 * m13 - m03 * m11;
        double b05 = m02 * m13 - m03 * m12;
        double b06 = m20 * m31 - m21 * m30;
        double b07 = m20 * m32 - m22 * m30;
        double b08 = m20 * m33 - m23 * m30;
        double b09 = m21 * m32 - m22 * m31;
        double b10 = m21 * m33 - m23 * m31;
        double b11 = m22 * m33 - m23 * m32;

        double det = b00 * b11 - b01 * b10 + b02 * b09 +
                b03 * b08 - b04 * b07 + b05 * b06;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        double invDet = 1.0 / det;

        // Invert the inputs
        double i00 = (m11 * b11 - m12 * b10 + m13 * b09) * invDet;
        double i01 = (-m01 * b11 + m02 * b10 - m03 * b09) * invDet;
        double i02 = (m31 * b05 - m32 * b04 + m33 * b03) * invDet;
        double i03 = (-m21 * b05 + m22 * b04 - m23 * b03) * invDet;

        double i10 = (-m10 * b11 + m12 * b08 - m13 * b07) * invDet;
        double i11 = (m00 * b11 - m02 * b08 + m03 * b07) * invDet;
        double i12 = (-m30 * b05 + m32 * b02 - m33 * b01) * invDet;
        double i13 = (m20 * b05 - m22 * b02 + m23 * b01) * invDet;

        double i20 = (m10 * b10 - m11 * b08 + m13 * b06) * invDet;
        double i21 = (-m00 * b10 + m01 * b08 - m03 * b06) * invDet;
        double i22 = (m30 * b04 - m31 * b02 + m33 * b00) * invDet;
        double i23 = (-m20 * b04 + m21 * b02 - m23 * b00) * invDet;

        double i30 = (-m10 * b09 + m11 * b07 - m12 * b06) * invDet;
        double i31 = (m00 * b09 - m01 * b07 + m02 * b06) * invDet;
        double i32 = (-m30 * b03 + m31 * b01 - m32 * b00) * invDet;
        double i33 = (m20 * b03 - m21 * b01 + m22 * b00) * invDet;

        // Multiply current matrix by the inverted values using master multiply
        return multiply(i00, i01, i02, i03, i10, i11, i12, i13, i20, i21, i22, i23, i30, i31, i32, i33);
    }

    public Matrix4Double divide(double scalar) {

        if (scalar == 0)
            throw new ArithmeticException("Division by zero");

        return multiply(
                1.0 / scalar, 0, 0, 0,
                0, 1.0 / scalar, 0, 0,
                0, 0, 1.0 / scalar, 0,
                0, 0, 0, 1.0 / scalar);
    }

    public Matrix4Double divide(Matrix4Double other) {
        return divide(
                other.val[0], other.val[4], other.val[8], other.val[12],
                other.val[1], other.val[5], other.val[9], other.val[13],
                other.val[2], other.val[6], other.val[10], other.val[14],
                other.val[3], other.val[7], other.val[11], other.val[15]);
    }

    // Inversion \\

    public Matrix4Double inverse() {

        double a00 = val[0], a10 = val[1], a20 = val[2], a30 = val[3];
        double a01 = val[4], a11 = val[5], a21 = val[6], a31 = val[7];
        double a02 = val[8], a12 = val[9], a22 = val[10], a32 = val[11];
        double a03 = val[12], a13 = val[13], a23 = val[14], a33 = val[15];

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

        double invDet = 1.0 / det;

        val[0] = (a11 * b11 - a12 * b10 + a13 * b09) * invDet;
        val[1] = (-a10 * b11 + a12 * b08 - a13 * b07) * invDet;
        val[2] = (a10 * b10 - a11 * b08 + a13 * b06) * invDet;
        val[3] = (-a10 * b09 + a11 * b07 - a12 * b06) * invDet;
        val[4] = (-a01 * b11 + a02 * b10 - a03 * b09) * invDet;
        val[5] = (a00 * b11 - a02 * b08 + a03 * b07) * invDet;
        val[6] = (-a00 * b10 + a01 * b08 - a03 * b06) * invDet;
        val[7] = (a00 * b09 - a01 * b07 + a02 * b06) * invDet;
        val[8] = (a31 * b05 - a32 * b04 + a33 * b03) * invDet;
        val[9] = (-a30 * b05 + a32 * b02 - a33 * b01) * invDet;
        val[10] = (a30 * b04 - a31 * b02 + a33 * b00) * invDet;
        val[11] = (-a30 * b03 + a31 * b01 - a32 * b00) * invDet;
        val[12] = (-a21 * b05 + a22 * b04 - a23 * b03) * invDet;
        val[13] = (a20 * b05 - a22 * b02 + a23 * b01) * invDet;
        val[14] = (-a20 * b04 + a21 * b02 - a23 * b00) * invDet;
        val[15] = (a20 * b03 - a21 * b01 + a22 * b00) * invDet;

        return this;
    }

    // Utility \\

    public boolean hasValues() {
        return val[0] != 0 || val[1] != 0 || val[2] != 0 || val[3] != 0 ||
                val[4] != 0 || val[5] != 0 || val[6] != 0 || val[7] != 0 ||
                val[8] != 0 || val[9] != 0 || val[10] != 0 || val[11] != 0 ||
                val[12] != 0 || val[13] != 0 || val[14] != 0 || val[15] != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Matrix4Double other)
            return val[0] == other.val[0] && val[1] == other.val[1] && val[2] == other.val[2] && val[3] == other.val[3]
                    &&
                    val[4] == other.val[4] && val[5] == other.val[5] && val[6] == other.val[6] && val[7] == other.val[7]
                    &&
                    val[8] == other.val[8] && val[9] == other.val[9] && val[10] == other.val[10]
                    && val[11] == other.val[11] &&
                    val[12] == other.val[12] && val[13] == other.val[13] && val[14] == other.val[14]
                    && val[15] == other.val[15];

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
        r = 31 * r + Double.hashCode(val[9]);
        r = 31 * r + Double.hashCode(val[10]);
        r = 31 * r + Double.hashCode(val[11]);
        r = 31 * r + Double.hashCode(val[12]);
        r = 31 * r + Double.hashCode(val[13]);
        r = 31 * r + Double.hashCode(val[14]);
        r = 31 * r + Double.hashCode(val[15]);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix4Double(" +
                "[" + val[0] + ", " + val[4] + ", " + val[8] + ", " + val[12] + "], " +
                "[" + val[1] + ", " + val[5] + ", " + val[9] + ", " + val[13] + "], " +
                "[" + val[2] + ", " + val[6] + ", " + val[10] + ", " + val[14] + "], " +
                "[" + val[3] + ", " + val[7] + ", " + val[11] + ", " + val[15] + "]" +
                ")";
    }
}