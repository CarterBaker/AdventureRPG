package com.AdventureRPG.core.util.mathematics.quaternions;

import com.AdventureRPG.core.util.mathematics.vectors.Vector3;

public class Quaternion {

    // Data
    public final float[] val = new float[4];

    // Constructors \\

    public Quaternion(
            float w, float x, float y, float z) {
        val[0] = w;
        val[1] = x;
        val[2] = y;
        val[3] = z;
    }

    public Quaternion() {
        this(
                1, 0, 0, 0);
    }

    public Quaternion(float scalar) {
        this(
                scalar, 0, 0, 0);
    }

    public Quaternion(Quaternion other) {
        System.arraycopy(other.val, 0, val, 0, 4);
    }

    public Quaternion(float[] array) {

        if (array == null || array.length != 4) // TODO: Add my own error
            throw new IllegalArgumentException("Quaternion array must have exactly 4 elements");

        val[0] = array[0]; // w
        val[1] = array[1]; // x
        val[2] = array[2]; // y
        val[3] = array[3]; // z
    }

    // Accessors \\

    public float getW() {
        return val[0];
    }

    public float getX() {
        return val[1];
    }

    public float getY() {
        return val[2];
    }

    public float getZ() {
        return val[3];
    }

    public void setW(float value) {
        val[0] = value;
    }

    public void setX(float value) {
        val[1] = value;
    }

    public void setY(float value) {
        val[2] = value;
    }

    public void setZ(float value) {
        val[3] = value;
    }

    // Set \\

    public Quaternion set(
            float w, float x, float y, float z) {

        val[0] = w;
        val[1] = x;
        val[2] = y;
        val[3] = z;

        return this;
    }

    public Quaternion set(float scalar) {
        return set(
                scalar, 0, 0, 0);
    }

    public Quaternion set(Quaternion other) {

        System.arraycopy(other.val, 0, val, 0, 4);

        return this;
    }

    // Addition \\

    public Quaternion add(
            float w, float x, float y, float z) {

        val[0] += w;
        val[1] += x;
        val[2] += y;
        val[3] += z;

        return this;
    }

    public Quaternion add(float scalar) {
        return add(
                scalar, scalar,
                scalar, scalar);
    }

    public Quaternion add(Quaternion other) {
        return add(
                other.val[0], other.val[1],
                other.val[2], other.val[3]);
    }

    // Subtraction \\

    public Quaternion subtract(
            float w, float x, float y, float z) {

        val[0] -= w;
        val[1] -= x;
        val[2] -= y;
        val[3] -= z;

        return this;
    }

    public Quaternion subtract(float scalar) {
        return subtract(
                scalar, scalar,
                scalar, scalar);
    }

    public Quaternion subtract(Quaternion other) {
        return subtract(
                other.val[0], other.val[1],
                other.val[2], other.val[3]);
    }

    // Multiplication \\

    public Quaternion multiply(
            float w, float x, float y, float z) {

        // Extract original values to prevent self-overwrite
        float aw = val[0], ax = val[1];
        float ay = val[2], az = val[3];

        float rw = aw * w - ax * x - ay * y - az * z;
        float rx = aw * x + ax * w + ay * z - az * y;
        float ry = aw * y - ax * z + ay * w + az * x;
        float rz = aw * z + ax * y - ay * x + az * w;

        val[0] = rw;
        val[1] = rx;
        val[2] = ry;
        val[3] = rz;

        return this;
    }

    public Quaternion multiply(float scalar) {
        return multiply(
                scalar, 0, 0, 0);
    }

    public Quaternion multiply(Quaternion other) {
        return multiply(
                other.val[0], other.val[1],
                other.val[2], other.val[3]);
    }

    // Division \\

    public Quaternion divide(
            float w, float x, float y, float z) {

        // Compute determinant
        float normSq = w * w + x * x + y * y + z * z;

        if (normSq == 0) // TODO: Add my own error
            throw new ArithmeticException("Quaternion not invertible");

        float invNormSq = 1.0f / normSq;

        // Invert the inputs
        float iw = w * invNormSq;
        float ix = -x * invNormSq;
        float iy = -y * invNormSq;
        float iz = -z * invNormSq;

        // Multiply current quaternion by the inverted values using master multiply
        return multiply(iw, ix, iy, iz);
    }

    public Quaternion divide(float scalar) {

        if (scalar == 0)
            throw new ArithmeticException("Division by zero");

        return multiply(
                1.0f / scalar, 0,
                0, 0);
    }

    public Quaternion divide(Quaternion other) {
        return divide(
                other.val[0], other.val[1],
                other.val[2], other.val[3]);
    }

    // Conjugate \\

    public Quaternion conjugate() {

        val[1] = -val[1];
        val[2] = -val[2];
        val[3] = -val[3];

        return this;
    }

    // Inversion \\

    public Quaternion inverse() {

        float w = val[0], x = val[1];
        float y = val[2], z = val[3];

        float normSq = w * w + x * x + y * y + z * z;

        if (normSq == 0) // TODO: Add my own error
            throw new ArithmeticException("Quaternion not invertible");

        float invNormSq = 1.0f / normSq;

        val[0] = w * invNormSq;
        val[1] = -x * invNormSq;
        val[2] = -y * invNormSq;
        val[3] = -z * invNormSq;

        return this;
    }

    // Normalization \\

    public Quaternion normalize() {

        float w = val[0], x = val[1];
        float y = val[2], z = val[3];

        float norm = (float) Math.sqrt(w * w + x * x + y * y + z * z);

        if (norm == 0) // TODO: Add my own error
            throw new ArithmeticException("Cannot normalize zero quaternion");

        float invNorm = 1.0f / norm;

        val[0] = w * invNorm;
        val[1] = x * invNorm;
        val[2] = y * invNorm;
        val[3] = z * invNorm;

        return this;
    }

    // Special Functions \\

    // Set from Euler angles (in degrees: yaw, pitch, roll)
    public Quaternion setFromEulerAngles(float yaw, float pitch, float roll) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float rollRad = (float) Math.toRadians(roll);

        // Compute half-angles
        float cy = (float) Math.cos(yawRad * 0.5f);
        float sy = (float) Math.sin(yawRad * 0.5f);
        float cp = (float) Math.cos(pitchRad * 0.5f);
        float sp = (float) Math.sin(pitchRad * 0.5f);
        float cr = (float) Math.cos(rollRad * 0.5f);
        float sr = (float) Math.sin(rollRad * 0.5f);

        // YXZ rotation order (yaw around Y, pitch around X, roll around Z)
        val[0] = cr * cp * cy + sr * sp * sy; // w
        val[1] = cr * sp * cy + sr * cp * sy; // x
        val[2] = cr * cp * sy - sr * sp * cy; // y
        val[3] = sr * cp * cy - cr * sp * sy; // z

        return this;
    }

    // Transform a Vector3 by this quaternion (rotation)
    public Vector3 transform(Vector3 v) {
        float qw = val[0], qx = val[1], qy = val[2], qz = val[3];
        float vx = v.x, vy = v.y, vz = v.z;

        // Calculate q * v * q^(-1)
        // Using the formula: v' = v + 2 * r × (r × v + w * v)
        // where q = (w, r) and r = (x, y, z)

        // First cross: r × v
        float cx = qy * vz - qz * vy;
        float cy = qz * vx - qx * vz;
        float cz = qx * vy - qy * vx;

        // Second part: r × v + w * v
        float tx = cx + qw * vx;
        float ty = cy + qw * vy;
        float tz = cz + qw * vz;

        // Final cross: r × t
        float rx = qy * tz - qz * ty;
        float ry = qz * tx - qx * tz;
        float rz = qx * ty - qy * tx;

        // Final result: v + 2 * (r × t)
        v.x = vx + 2.0f * rx;
        v.y = vy + 2.0f * ry;
        v.z = vz + 2.0f * rz;

        return v;
    }

    // Utility \\

    public float norm() {
        return (float) Math.sqrt(val[0] * val[0] + val[1] * val[1] + val[2] * val[2] + val[3] * val[3]);
    }

    public float normSquared() {
        return val[0] * val[0] + val[1] * val[1] + val[2] * val[2] + val[3] * val[3];
    }

    public boolean hasValues() {
        return val[0] != 0 || val[1] != 0 ||
                val[2] != 0 || val[3] != 0;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Quaternion other)
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
        return "Quaternion(" +
                val[0] + " + " +
                val[1] + "i + " +
                val[2] + "j + " +
                val[3] + "k" +
                ")";
    }
}