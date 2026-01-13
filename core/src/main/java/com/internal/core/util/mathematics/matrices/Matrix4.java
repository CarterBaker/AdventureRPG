package com.internal.core.util.mathematics.matrices;

public class Matrix4 {

    // Data
    public final float[] val = new float[16];

    // Constructors \\

    public Matrix4(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {
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

    public Matrix4() {
        this(1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }

    public Matrix4(float scalar) {
        this(scalar, 0, 0, 0,
                0, scalar, 0, 0,
                0, 0, scalar, 0,
                0, 0, 0, scalar);
    }

    public Matrix4(Matrix4 other) {
        System.arraycopy(other.val, 0, val, 0, 16);
    }

    public Matrix4(float[] array) {

        if (array == null || array.length != 16) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix4 array must have exactly 16 elements");

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

    public float getM00() {
        return val[0];
    }

    public float getM10() {
        return val[1];
    }

    public float getM20() {
        return val[2];
    }

    public float getM30() {
        return val[3];
    }

    public float getM01() {
        return val[4];
    }

    public float getM11() {
        return val[5];
    }

    public float getM21() {
        return val[6];
    }

    public float getM31() {
        return val[7];
    }

    public float getM02() {
        return val[8];
    }

    public float getM12() {
        return val[9];
    }

    public float getM22() {
        return val[10];
    }

    public float getM32() {
        return val[11];
    }

    public float getM03() {
        return val[12];
    }

    public float getM13() {
        return val[13];
    }

    public float getM23() {
        return val[14];
    }

    public float getM33() {
        return val[15];
    }

    public void setM00(float v) {
        val[0] = v;
    }

    public void setM10(float v) {
        val[1] = v;
    }

    public void setM20(float v) {
        val[2] = v;
    }

    public void setM30(float v) {
        val[3] = v;
    }

    public void setM01(float v) {
        val[4] = v;
    }

    public void setM11(float v) {
        val[5] = v;
    }

    public void setM21(float v) {
        val[6] = v;
    }

    public void setM31(float v) {
        val[7] = v;
    }

    public void setM02(float v) {
        val[8] = v;
    }

    public void setM12(float v) {
        val[9] = v;
    }

    public void setM22(float v) {
        val[10] = v;
    }

    public void setM32(float v) {
        val[11] = v;
    }

    public void setM03(float v) {
        val[12] = v;
    }

    public void setM13(float v) {
        val[13] = v;
    }

    public void setM23(float v) {
        val[14] = v;
    }

    public void setM33(float v) {
        val[15] = v;
    }

    // Conversion \\

    public Matrix4 fromGDX(com.badlogic.gdx.math.Matrix4 other) {

        System.arraycopy(other.val, 0, val, 0, 16);

        return this;
    }

    // Set \\

    public Matrix4 set(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {

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

    public Matrix4 set(float scalar) {
        return set(
                scalar, 0, 0, 0,
                0, scalar, 0, 0,
                0, 0, scalar, 0,
                0, 0, 0, scalar);
    }

    public Matrix4 set(Matrix4 other) {

        System.arraycopy(other.val, 0, val, 0, 16);

        return this;
    }

    // Addition \\

    public Matrix4 add(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {

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

    public Matrix4 add(float scalar) {
        return add(
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar);
    }

    public Matrix4 add(Matrix4 other) {
        return add(
                other.val[0], other.val[4], other.val[8], other.val[12],
                other.val[1], other.val[5], other.val[9], other.val[13],
                other.val[2], other.val[6], other.val[10], other.val[14],
                other.val[3], other.val[7], other.val[11], other.val[15]);
    }

    // Subtraction \\

    public Matrix4 subtract(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {

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

    public Matrix4 subtract(float scalar) {
        return subtract(
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar,
                scalar, scalar, scalar, scalar);
    }

    public Matrix4 subtract(Matrix4 other) {
        return subtract(
                other.val[0], other.val[4], other.val[8], other.val[12],
                other.val[1], other.val[5], other.val[9], other.val[13],
                other.val[2], other.val[6], other.val[10], other.val[14],
                other.val[3], other.val[7], other.val[11], other.val[15]);
    }

    // Multiplication \\

    public Matrix4 multiply(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {

        // Extract original values to prevent self-overwrite
        float a00 = val[0], a10 = val[1], a20 = val[2], a30 = val[3];
        float a01 = val[4], a11 = val[5], a21 = val[6], a31 = val[7];
        float a02 = val[8], a12 = val[9], a22 = val[10], a32 = val[11];
        float a03 = val[12], a13 = val[13], a23 = val[14], a33 = val[15];

        float r00 = a00 * m00 + a01 * m10 + a02 * m20 + a03 * m30;
        float r10 = a10 * m00 + a11 * m10 + a12 * m20 + a13 * m30;
        float r20 = a20 * m00 + a21 * m10 + a22 * m20 + a23 * m30;
        float r30 = a30 * m00 + a31 * m10 + a32 * m20 + a33 * m30;

        float r01 = a00 * m01 + a01 * m11 + a02 * m21 + a03 * m31;
        float r11 = a10 * m01 + a11 * m11 + a12 * m21 + a13 * m31;
        float r21 = a20 * m01 + a21 * m11 + a22 * m21 + a23 * m31;
        float r31 = a30 * m01 + a31 * m11 + a32 * m21 + a33 * m31;

        float r02 = a00 * m02 + a01 * m12 + a02 * m22 + a03 * m32;
        float r12 = a10 * m02 + a11 * m12 + a12 * m22 + a13 * m32;
        float r22 = a20 * m02 + a21 * m12 + a22 * m22 + a23 * m32;
        float r32 = a30 * m02 + a31 * m12 + a32 * m22 + a33 * m32;

        float r03 = a00 * m03 + a01 * m13 + a02 * m23 + a03 * m33;
        float r13 = a10 * m03 + a11 * m13 + a12 * m23 + a13 * m33;
        float r23 = a20 * m03 + a21 * m13 + a22 * m23 + a23 * m33;
        float r33 = a30 * m03 + a31 * m13 + a32 * m23 + a33 * m33;

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

    public Matrix4 multiply(float scalar) {
        return multiply(
                scalar, 0, 0, 0,
                0, scalar, 0, 0,
                0, 0, scalar, 0,
                0, 0, 0, scalar);
    }

    public Matrix4 multiply(Matrix4 other) {
        return multiply(
                other.val[0], other.val[4], other.val[8], other.val[12],
                other.val[1], other.val[5], other.val[9], other.val[13],
                other.val[2], other.val[6], other.val[10], other.val[14],
                other.val[3], other.val[7], other.val[11], other.val[15]);
    }

    // Division \\

    public Matrix4 divide(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {

        float b00 = m00 * m11 - m01 * m10;
        float b01 = m00 * m12 - m02 * m10;
        float b02 = m00 * m13 - m03 * m10;
        float b03 = m01 * m12 - m02 * m11;
        float b04 = m01 * m13 - m03 * m11;
        float b05 = m02 * m13 - m03 * m12;
        float b06 = m20 * m31 - m21 * m30;
        float b07 = m20 * m32 - m22 * m30;
        float b08 = m20 * m33 - m23 * m30;
        float b09 = m21 * m32 - m22 * m31;
        float b10 = m21 * m33 - m23 * m31;
        float b11 = m22 * m33 - m23 * m32;

        float det = b00 * b11 - b01 * b10 + b02 * b09 +
                b03 * b08 - b04 * b07 + b05 * b06;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        float invDet = 1.0f / det;

        // Invert the inputs
        float i00 = (m11 * b11 - m12 * b10 + m13 * b09) * invDet;
        float i01 = (-m01 * b11 + m02 * b10 - m03 * b09) * invDet;
        float i02 = (m31 * b05 - m32 * b04 + m33 * b03) * invDet;
        float i03 = (-m21 * b05 + m22 * b04 - m23 * b03) * invDet;

        float i10 = (-m10 * b11 + m12 * b08 - m13 * b07) * invDet;
        float i11 = (m00 * b11 - m02 * b08 + m03 * b07) * invDet;
        float i12 = (-m30 * b05 + m32 * b02 - m33 * b01) * invDet;
        float i13 = (m20 * b05 - m22 * b02 + m23 * b01) * invDet;

        float i20 = (m10 * b10 - m11 * b08 + m13 * b06) * invDet;
        float i21 = (-m00 * b10 + m01 * b08 - m03 * b06) * invDet;
        float i22 = (m30 * b04 - m31 * b02 + m33 * b00) * invDet;
        float i23 = (-m20 * b04 + m21 * b02 - m23 * b00) * invDet;

        float i30 = (-m10 * b09 + m11 * b07 - m12 * b06) * invDet;
        float i31 = (m00 * b09 - m01 * b07 + m02 * b06) * invDet;
        float i32 = (-m30 * b03 + m31 * b01 - m32 * b00) * invDet;
        float i33 = (m20 * b03 - m21 * b01 + m22 * b00) * invDet;

        // Multiply current matrix by the inverted values using master multiply
        return multiply(i00, i01, i02, i03, i10, i11, i12, i13, i20, i21, i22, i23, i30, i31, i32, i33);
    }

    public Matrix4 divide(float scalar) {

        if (scalar == 0)
            throw new ArithmeticException("Division by zero");

        return multiply(1.0f / scalar, 0, 0, 0, 0, 1.0f / scalar, 0, 0, 0, 0, 1.0f / scalar, 0, 0, 0, 0, 1.0f / scalar);
    }

    public Matrix4 divide(Matrix4 other) {
        return divide(
                other.val[0], other.val[4], other.val[8], other.val[12],
                other.val[1], other.val[5], other.val[9], other.val[13],
                other.val[2], other.val[6], other.val[10], other.val[14],
                other.val[3], other.val[7], other.val[11], other.val[15]);
    }

    // Inversion \\

    public Matrix4 inverse() {

        float a00 = val[0], a10 = val[1], a20 = val[2], a30 = val[3];
        float a01 = val[4], a11 = val[5], a21 = val[6], a31 = val[7];
        float a02 = val[8], a12 = val[9], a22 = val[10], a32 = val[11];
        float a03 = val[12], a13 = val[13], a23 = val[14], a33 = val[15];

        float b00 = a00 * a11 - a01 * a10;
        float b01 = a00 * a12 - a02 * a10;
        float b02 = a00 * a13 - a03 * a10;
        float b03 = a01 * a12 - a02 * a11;
        float b04 = a01 * a13 - a03 * a11;
        float b05 = a02 * a13 - a03 * a12;
        float b06 = a20 * a31 - a21 * a30;
        float b07 = a20 * a32 - a22 * a30;
        float b08 = a20 * a33 - a23 * a30;
        float b09 = a21 * a32 - a22 * a31;
        float b10 = a21 * a33 - a23 * a31;
        float b11 = a22 * a33 - a23 * a32;

        float det = b00 * b11 - b01 * b10 + b02 * b09 +
                b03 * b08 - b04 * b07 + b05 * b06;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        float invDet = 1.0f / det;

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

        if (obj instanceof Matrix4 other)
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

        r = 31 * r + Float.hashCode(val[0]);
        r = 31 * r + Float.hashCode(val[1]);
        r = 31 * r + Float.hashCode(val[2]);
        r = 31 * r + Float.hashCode(val[3]);
        r = 31 * r + Float.hashCode(val[4]);
        r = 31 * r + Float.hashCode(val[5]);
        r = 31 * r + Float.hashCode(val[6]);
        r = 31 * r + Float.hashCode(val[7]);
        r = 31 * r + Float.hashCode(val[8]);
        r = 31 * r + Float.hashCode(val[9]);
        r = 31 * r + Float.hashCode(val[10]);
        r = 31 * r + Float.hashCode(val[11]);
        r = 31 * r + Float.hashCode(val[12]);
        r = 31 * r + Float.hashCode(val[13]);
        r = 31 * r + Float.hashCode(val[14]);
        r = 31 * r + Float.hashCode(val[15]);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix4(" +
                "[" + val[0] + ", " + val[4] + ", " + val[8] + ", " + val[12] + "], " +
                "[" + val[1] + ", " + val[5] + ", " + val[9] + ", " + val[13] + "], " +
                "[" + val[2] + ", " + val[6] + ", " + val[10] + ", " + val[14] + "], " +
                "[" + val[3] + ", " + val[7] + ", " + val[11] + ", " + val[15] + "]" +
                ")";
    }
}