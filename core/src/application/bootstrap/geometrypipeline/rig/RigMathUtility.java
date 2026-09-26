package application.bootstrap.geometrypipeline.rig;

import engine.root.EngineUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class RigMathUtility extends EngineUtility {

    /*
     * Stateless bone-transform composition shared by the rig's bind-pose bake
     * and AnimationStateHandle's per-frame pose, so both always agree. Rotation
     * composes in XYZ order from degrees, and composeLocal() writes into
     * caller-owned output and scratch matrices so the per-bone hot path never
     * allocates.
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