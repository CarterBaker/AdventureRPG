package application.bootstrap.geometrypipeline.rig;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class RigMathUtility extends EngineUtility {

    /*
     * Stateless bone-transform composition shared by the rig's bind-pose bake
     * and AnimationStateHandle's per-frame pose, so both always agree. Rotation
     * composes in XYZ order from degrees, and composeLocal() writes into
     * caller-owned output and scratch matrices so the per-bone hot path never
     * allocates; setPivotTurn() turns a bone about its joint onto a new
     * direction, as a spring chain bends its bones after the joints it
     * simulates.
     */

    // Compose \\

    public static void composeLocal(
            Vector3 position,
            Vector3 rotationDegrees,
            Vector3 scale,
            Matrix4 out,
            Matrix4 scratchA,
            Matrix4 scratchB) {

        // out = T
        setTranslation(out, position);

        // scratchA = R = Rz * Ry * Rx
        setRotationZ(scratchA, (float) Math.toRadians(rotationDegrees.z));
        setRotationY(scratchB, (float) Math.toRadians(rotationDegrees.y));
        scratchA.multiply(scratchB);
        setRotationX(scratchB, (float) Math.toRadians(rotationDegrees.x));
        scratchA.multiply(scratchB);

        // out = T * R
        out.multiply(scratchA);

        // scratchA = S ; out = T * R * S
        setScale(scratchA, scale);
        out.multiply(scratchA);
    }

    // Pivot \\

    // The turn about a pivot carrying one direction onto another along the shortest arc, written into out; none when
    // either direction has no length
    public static void setPivotTurn(Matrix4 out, Vector3 pivot, Vector3 from, Vector3 to) {

        float fromLength = from.length();
        float toLength = to.length();

        if (fromLength <= EngineSetting.RIG_SPRING_DIRECTION_EPSILON
                || toLength <= EngineSetting.RIG_SPRING_DIRECTION_EPSILON) {
            out.set(1f);
            return;
        }

        float ax = from.x / fromLength;
        float ay = from.y / fromLength;
        float az = from.z / fromLength;
        float bx = to.x / toLength;
        float by = to.y / toLength;
        float bz = to.z / toLength;
        float cos = ax * bx + ay * by + az * bz;

        if (cos <= EngineSetting.RIG_SPRING_DIRECTION_EPSILON - 1f) {
            setHalfTurn(out, pivot, ax, ay, az);
            return;
        }

        float vx = ay * bz - az * by;
        float vy = az * bx - ax * bz;
        float vz = ax * by - ay * bx;
        float k = 1f / (1f + cos);

        setPivoted(
                out, pivot,
                cos + k * vx * vx, k * vx * vy - vz, k * vx * vz + vy,
                k * vy * vx + vz, cos + k * vy * vy, k * vy * vz - vx,
                k * vz * vx - vy, k * vz * vy + vx, cos + k * vz * vz);
    }

    // Half a turn about any axis square to a direction, for two directions pointing apart
    private static void setHalfTurn(Matrix4 out, Vector3 pivot, float ax, float ay, float az) {

        float ux = Math.abs(ax) < 0.9f ? 0f : -az;
        float uy = Math.abs(ax) < 0.9f ? -az : 0f;
        float uz = Math.abs(ax) < 0.9f ? ay : ax;
        float length = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);

        ux /= length;
        uy /= length;
        uz /= length;

        setPivoted(
                out, pivot,
                2f * ux * ux - 1f, 2f * ux * uy, 2f * ux * uz,
                2f * uy * ux, 2f * uy * uy - 1f, 2f * uy * uz,
                2f * uz * ux, 2f * uz * uy, 2f * uz * uz - 1f);
    }

    // A rotation written about a pivot: the pivot stays where it is and everything else turns around it
    private static void setPivoted(
            Matrix4 out,
            Vector3 pivot,
            float r00, float r01, float r02,
            float r10, float r11, float r12,
            float r20, float r21, float r22) {

        out.set(
                r00, r01, r02, pivot.x - (r00 * pivot.x + r01 * pivot.y + r02 * pivot.z),
                r10, r11, r12, pivot.y - (r10 * pivot.x + r11 * pivot.y + r12 * pivot.z),
                r20, r21, r22, pivot.z - (r20 * pivot.x + r21 * pivot.y + r22 * pivot.z),
                0, 0, 0, 1);
    }

    // Translation \\

    private static void setTranslation(Matrix4 out, Vector3 position) {
        out.set(
                1, 0, 0, position.x,
                0, 1, 0, position.y,
                0, 0, 1, position.z,
                0, 0, 0, 1);
    }

    // Scale \\

    public static void setScale(Matrix4 out, Vector3 scale) {
        out.set(
                scale.x, 0, 0, 0,
                0, scale.y, 0, 0,
                0, 0, scale.z, 0,
                0, 0, 0, 1);
    }

    // Rotation \\

    private static void setRotationX(Matrix4 out, float radians) {

        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        out.set(
                1, 0, 0, 0,
                0, cos, -sin, 0,
                0, sin, cos, 0,
                0, 0, 0, 1);
    }

    private static void setRotationY(Matrix4 out, float radians) {

        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        out.set(
                cos, 0, sin, 0,
                0, 1, 0, 0,
                -sin, 0, cos, 0,
                0, 0, 0, 1);
    }

    private static void setRotationZ(Matrix4 out, float radians) {

        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        out.set(
                cos, -sin, 0, 0,
                sin, cos, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }
}