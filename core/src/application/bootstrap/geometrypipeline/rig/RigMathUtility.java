package application.bootstrap.geometrypipeline.rig;

import engine.root.EngineUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class RigMathUtility extends EngineUtility {

    /*
     * Stateless bone-transform composition shared by RigData's bind-pose
     * bake and AnimationStateHandle's per-frame pose evaluation. Public
     * rather than package-private, unlike the standard GLSLUtility pattern
     * — this is genuinely needed from both the rig package and the
     * entity-side animation package, and duplicating it would break the
     * "one true composition function" both the bind bake and the runtime
     * pose depend on producing identical results from.
     *
     * Rotation is composed in XYZ order — a vertex rotates about its local
     * X axis first, then Y, then Z — matching standard Blockbench-style
     * cuboid rig authoring. All angles are degrees in, matching the JSON
     * schema.
     *
     * composeLocal writes into caller-supplied output and scratch matrices
     * instead of allocating — this runs once per bone per entity per
     * frame, a genuine hot path. Callers own three pre-allocated Matrix4
     * instances (out, scratchA, scratchB) and reuse them forever.
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

    private static void setScale(Matrix4 out, Vector3 scale) {
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