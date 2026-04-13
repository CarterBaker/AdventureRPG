package program.core.util.mathematics.quaternions;

import program.core.engine.EngineUtility;
import program.core.util.mathematics.vectors.Vector3;

public class Quaternion extends EngineUtility {

    // Data
    public final float[] val = new float[4];

    // Constructors \\

    public Quaternion(float w, float x, float y, float z) {
        val[0] = w;
        val[1] = x;
        val[2] = y;
        val[3] = z;
    }

    public Quaternion() {
        this(1, 0, 0, 0);
    }

    public Quaternion(float scalar) {
        this(scalar, 0, 0, 0);
    }

    public Quaternion(Quaternion other) {
        System.arraycopy(other.val, 0, val, 0, 4);
    }

    public Quaternion(float[] array) {
        if (array == null || array.length != 4)
            throwException("Expected 4-element array");
        val[0] = array[0];
        val[1] = array[1];
        val[2] = array[2];
        val[3] = array[3];
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

    public Quaternion set(float w, float x, float y, float z) {
        val[0] = w;
        val[1] = x;
        val[2] = y;
        val[3] = z;
        return this;
    }

    public Quaternion set(float scalar) {
        return set(scalar, 0, 0, 0);
    }

    public Quaternion set(Quaternion other) {
        System.arraycopy(other.val, 0, val, 0, 4);
        return this;
    }

    // Addition \\

    public Quaternion add(float w, float x, float y, float z) {
        val[0] += w;
        val[1] += x;
        val[2] += y;
        val[3] += z;
        return this;
    }

    public Quaternion add(float scalar) {
        return add(scalar, scalar, scalar, scalar);
    }

    public Quaternion add(Quaternion other) {
        return add(other.val[0], other.val[1], other.val[2], other.val[3]);
    }

    // Subtraction \\

    public Quaternion subtract(float w, float x, float y, float z) {
        val[0] -= w;
        val[1] -= x;
        val[2] -= y;
        val[3] -= z;
        return this;
    }

    public Quaternion subtract(float scalar) {
        return subtract(scalar, scalar, scalar, scalar);
    }

    public Quaternion subtract(Quaternion other) {
        return subtract(other.val[0], other.val[1], other.val[2], other.val[3]);
    }

    // Multiplication \\

    public Quaternion multiply(float w, float x, float y, float z) {
        float aw = val[0], ax = val[1], ay = val[2], az = val[3];

        val[0] = aw * w - ax * x - ay * y - az * z;
        val[1] = aw * x + ax * w + ay * z - az * y;
        val[2] = aw * y - ax * z + ay * w + az * x;
        val[3] = aw * z + ax * y - ay * x + az * w;

        return this;
    }

    public Quaternion multiply(float scalar) {
        return multiply(scalar, 0, 0, 0);
    }

    public Quaternion multiply(Quaternion other) {
        return multiply(other.val[0], other.val[1], other.val[2], other.val[3]);
    }

    // Division \\

    public Quaternion divide(float w, float x, float y, float z) {
        float normSq = w * w + x * x + y * y + z * z;
        if (normSq == 0)
            throwException("Quaternion not invertible");

        float inv = 1.0f / normSq;
        return multiply(w * inv, -x * inv, -y * inv, -z * inv);
    }

    public Quaternion divide(float scalar) {
        if (scalar == 0)
            throwException("Division by zero");
        return multiply(1.0f / scalar, 0, 0, 0);
    }

    public Quaternion divide(Quaternion other) {
        return divide(other.val[0], other.val[1], other.val[2], other.val[3]);
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
        float normSq = val[0] * val[0] + val[1] * val[1] + val[2] * val[2] + val[3] * val[3];
        if (normSq == 0)
            throwException("Quaternion not invertible");

        float inv = 1.0f / normSq;
        val[0] = val[0] * inv;
        val[1] = -val[1] * inv;
        val[2] = -val[2] * inv;
        val[3] = -val[3] * inv;

        return this;
    }

    // Normalization \\

    public Quaternion normalize() {
        float norm = (float) Math.sqrt(val[0] * val[0] + val[1] * val[1] + val[2] * val[2] + val[3] * val[3]);
        if (norm == 0)
            throwException("Cannot normalize zero quaternion");

        float inv = 1.0f / norm;
        val[0] *= inv;
        val[1] *= inv;
        val[2] *= inv;
        val[3] *= inv;

        return this;
    }

    // Special Functions \\

    public Quaternion setFromEulerAngles(float yaw, float pitch, float roll) {
        float cy = (float) Math.cos(Math.toRadians(yaw) * 0.5);
        float sy = (float) Math.sin(Math.toRadians(yaw) * 0.5);
        float cp = (float) Math.cos(Math.toRadians(pitch) * 0.5);
        float sp = (float) Math.sin(Math.toRadians(pitch) * 0.5);
        float cr = (float) Math.cos(Math.toRadians(roll) * 0.5);
        float sr = (float) Math.sin(Math.toRadians(roll) * 0.5);

        val[0] = cr * cp * cy + sr * sp * sy; // w
        val[1] = cr * sp * cy + sr * cp * sy; // x
        val[2] = cr * cp * sy - sr * sp * cy; // y
        val[3] = sr * cp * cy - cr * sp * sy; // z

        return this;
    }

    public Vector3 transform(Vector3 v) {
        float qw = val[0], qx = val[1], qy = val[2], qz = val[3];
        float vx = v.x, vy = v.y, vz = v.z;

        float cx = qy * vz - qz * vy;
        float cy = qz * vx - qx * vz;
        float cz = qx * vy - qy * vx;

        float tx = cx + qw * vx;
        float ty = cy + qw * vy;
        float tz = cz + qw * vz;

        v.x = vx + 2.0f * (qy * tz - qz * ty);
        v.y = vy + 2.0f * (qz * tx - qx * tz);
        v.z = vz + 2.0f * (qx * ty - qy * tx);

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
        return val[0] != 0 || val[1] != 0 || val[2] != 0 || val[3] != 0;
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
        return "Quaternion(" + val[0] + " + " + val[1] + "i + " + val[2] + "j + " + val[3] + "k)";
    }
}