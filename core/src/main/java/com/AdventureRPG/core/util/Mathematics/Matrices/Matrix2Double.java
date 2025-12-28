package com.AdventureRPG.core.util.Mathematics.Matrices;

public class Matrix2Double {

    // Data
    public final double[] val = new double[4];

    // Constructors \\

    public Matrix2Double(
            double m00, double m01,
            double m10, double m11) {
        val[0] = m00;
        val[1] = m10;
        val[2] = m01;
        val[3] = m11;
    }

    public Matrix2Double() {
        this(
                1, 0,
                0, 1);
    }

    public Matrix2Double(double scalar) {
        this(
                scalar, 0,
                0, scalar);
    }

    public Matrix2Double(Matrix2Double other) {
        System.arraycopy(other.val, 0, val, 0, 4);
    }

    public Matrix2Double(double[] array) {

        if (array == null || array.length != 4) // TODO: Add my own error
            throw new IllegalArgumentException("Matrix2Double array must have exactly 4 elements");

        // Row-major order [m00, m01, m10, m11]
        val[0] = array[0]; // m00
        val[1] = array[2]; // m10
        val[2] = array[1]; // m01
        val[3] = array[3]; // m11
    }

    // Accessors \\

    public double getM00() {
        return val[0];
    }

    public double getM10() {
        return val[1];
    }

    public double getM01() {
        return val[2];
    }

    public double getM11() {
        return val[3];
    }

    public void setM00(double value) {
        val[0] = value;
    }

    public void setM10(double value) {
        val[1] = value;
    }

    public void setM01(double value) {
        val[2] = value;
    }

    public void setM11(double value) {
        val[3] = value;
    }

    // Set \\

    public Matrix2Double set(
            double m00, double m01,
            double m10, double m11) {

        val[0] = m00;
        val[1] = m10;
        val[2] = m01;
        val[3] = m11;

        return this;
    }

    public Matrix2Double set(double scalar) {
        return set(
                scalar, 0,
                0, scalar);
    }

    public Matrix2Double set(Matrix2Double other) {

        System.arraycopy(other.val, 0, val, 0, 4);

        return this;
    }

    // Addition \\

    public Matrix2Double add(
            double m00, double m01,
            double m10, double m11) {

        val[0] += m00;
        val[1] += m10;
        val[2] += m01;
        val[3] += m11;

        return this;
    }

    public Matrix2Double add(double scalar) {
        return add(
                scalar, scalar,
                scalar, scalar);
    }

    public Matrix2Double add(Matrix2Double other) {
        return add(
                other.val[0], other.val[2],
                other.val[1], other.val[3]);
    }

    // Subtraction \\

    public Matrix2Double subtract(
            double m00, double m01,
            double m10, double m11) {

        val[0] -= m00;
        val[1] -= m10;
        val[2] -= m01;
        val[3] -= m11;

        return this;
    }

    public Matrix2Double subtract(double scalar) {
        return subtract(
                scalar, scalar,
                scalar, scalar);
    }

    public Matrix2Double subtract(Matrix2Double other) {
        return subtract(
                other.val[0], other.val[2],
                other.val[1], other.val[3]);
    }

    // Multiplication \\

    public Matrix2Double multiply(
            double m00, double m01,
            double m10, double m11) {

        // Extract original values to prevent self-overwrite
        double a00 = val[0], a10 = val[1];
        double a01 = val[2], a11 = val[3];

        double r00 = a00 * m00 + a01 * m10;
        double r10 = a10 * m00 + a11 * m10;
        double r01 = a00 * m01 + a01 * m11;
        double r11 = a10 * m01 + a11 * m11;

        val[0] = r00;
        val[1] = r10;
        val[2] = r01;
        val[3] = r11;

        return this;
    }

    public Matrix2Double multiply(double scalar) {
        return multiply(
                scalar, 0,
                0, scalar);
    }

    public Matrix2Double multiply(Matrix2Double other) {
        return multiply(
                other.val[0], other.val[2],
                other.val[1], other.val[3]);
    }

    // Division \\

    public Matrix2Double divide(
            double m00, double m01,
            double m10, double m11) {

        // Compute determinant
        double det = m00 * m11 - m01 * m10;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        double invDet = 1.0 / det;

        // Invert the inputs
        double i00 = m11 * invDet;
        double i01 = -m01 * invDet;
        double i10 = -m10 * invDet;
        double i11 = m00 * invDet;

        // Multiply current matrix by the inverted values using master multiply
        return multiply(i00, i01, i10, i11);
    }

    public Matrix2Double divide(double scalar) {

        if (scalar == 0)
            throw new ArithmeticException("Division by zero");

        return multiply(
                1.0 / scalar, 0,
                0, 1.0 / scalar);
    }

    public Matrix2Double divide(Matrix2Double other) {
        return divide(
                other.val[0], other.val[2],
                other.val[1], other.val[3]);
    }

    // Inversion \\

    public Matrix2Double inverse() {

        double a00 = val[0], a10 = val[1];
        double a01 = val[2], a11 = val[3];

        double det = a00 * a11 - a01 * a10;

        if (det == 0) // TODO: Add my own error
            throw new ArithmeticException("Matrix not invertible");

        double invDet = 1.0 / det;

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

        if (obj instanceof Matrix2Double other)
            return val[0] == other.val[0] && val[1] == other.val[1] &&
                    val[2] == other.val[2] && val[3] == other.val[3];

        return false;
    }

    @Override
    public int hashCode() {

        int r = 17;

        r = 31 * r + Double.hashCode(val[0]);
        r = 31 * r + Double.hashCode(val[1]);
        r = 31 * r + Double.hashCode(val[2]);
        r = 31 * r + Double.hashCode(val[3]);

        return r;
    }

    @Override
    public String toString() {
        return "Matrix2Double(" +
                "[" + val[0] + ", " + val[2] + "], " +
                "[" + val[1] + ", " + val[3] + "]" +
                ")";
    }
}