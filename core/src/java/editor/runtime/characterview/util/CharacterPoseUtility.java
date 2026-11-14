package editor.runtime.characterview.util;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class CharacterPoseUtility extends EngineUtility {

    /*
     * Stateless geometry the character tools' viewport gestures share: a
     * point's place in a window and a window pixel's ray through the window's
     * camera, where a ray meets a plane, the joint a matrix places, a rotation
     * built from and read back into the Euler degrees bones are stored in —
     * composed Z, then Y, then X as the rig composes them — and a turn about
     * a world axis carried into a bone's own rotation under its parent. An
     * angle read back is the one of its two equal Euler forms, each wrapped
     * by whole turns, nearest the angle it replaces, so a key never spins
     * the long way round to its neighbour.
     */

    // Window \\

    // A point's place in a window, in pixels from its bottom left; false for a point behind the camera
    public static boolean project(WindowInstance window, float x, float y, float z, Vector2 out) {

        float[] m = window.getActiveCamera().getViewProjection().val;
        float clipX = m[0] * x + m[4] * y + m[8] * z + m[12];
        float clipY = m[1] * x + m[5] * y + m[9] * z + m[13];
        float clipW = m[3] * x + m[7] * y + m[11] * z + m[15];

        if (clipW <= 0f)
            return false;

        out.set(
                (clipX / clipW + 1f) * 0.5f * window.getWidth(),
                (clipY / clipW + 1f) * 0.5f * window.getHeight());
        return true;
    }

    // The ray from a window's camera through one of its pixels
    public static void resolveRay(
            WindowInstance window,
            float pixelX,
            float pixelY,
            Vector3 origin,
            Vector3 direction) {
        window.getActiveCamera().getPickRay(
                pixelX / window.getWidth() * 2f - 1f,
                pixelY / window.getHeight() * 2f - 1f,
                origin,
                direction);
    }

    // Rays \\

    // Where a ray meets the plane through a point facing along a normal; the point itself when the ray runs along it
    public static void resolvePlaneHit(
            Vector3 origin,
            Vector3 direction,
            Vector3 point,
            Vector3 normal,
            Vector3 out) {

        float facing = direction.x * normal.x + direction.y * normal.y + direction.z * normal.z;

        if (Math.abs(facing) < EditorSetting.CHARACTER_VIEW_RAY_EPSILON) {
            out.set(point);
            return;
        }

        float t = ((point.x - origin.x) * normal.x
                + (point.y - origin.y) * normal.y
                + (point.z - origin.z) * normal.z) / facing;

        out.set(origin.x + direction.x * t, origin.y + direction.y * t, origin.z + direction.z * t);
    }

    // Matrices \\

    public static void getTranslation(Matrix4 matrix, Vector3 out) {
        out.set(matrix.val[12], matrix.val[13], matrix.val[14]);
    }

    // A point carried through a matrix
    public static void transformPoint(Matrix4 matrix, float x, float y, float z, Vector3 out) {

        float[] m = matrix.val;

        out.set(
                m[0] * x + m[4] * y + m[8] * z + m[12],
                m[1] * x + m[5] * y + m[9] * z + m[13],
                m[2] * x + m[6] * y + m[10] * z + m[14]);
    }

    // A direction carried through a matrix's turn and scale, without its translation
    public static void transformDirection(Matrix4 matrix, float x, float y, float z, Vector3 out) {

        float[] m = matrix.val;

        out.set(
                m[0] * x + m[4] * y + m[8] * z,
                m[1] * x + m[5] * y + m[9] * z,
                m[2] * x + m[6] * y + m[10] * z);
    }

    // The pure turn a matrix holds, its axes made unit and its translation dropped
    public static void extractRotation(Matrix4 matrix, Matrix4 out) {

        float[] m = matrix.val;
        float sx = length(m[0], m[1], m[2]);
        float sy = length(m[4], m[5], m[6]);
        float sz = length(m[8], m[9], m[10]);

        out.set(
                m[0] / sx, m[4] / sy, m[8] / sz, 0f,
                m[1] / sx, m[5] / sy, m[9] / sz, 0f,
                m[2] / sx, m[6] / sy, m[10] / sz, 0f,
                0f, 0f, 0f, 1f);
    }

    private static float length(float x, float y, float z) {

        float length = (float) Math.sqrt(x * x + y * y + z * z);

        return length > EditorSetting.CHARACTER_VIEW_RAY_EPSILON ? length : 1f;
    }

    // Rotation \\

    // The turn of Euler degrees, composed Z, then Y, then X as the rig composes a bone's local rotation
    public static void toRotation(Vector3 degrees, Matrix4 out) {

        double x = Math.toRadians(degrees.x);
        double y = Math.toRadians(degrees.y);
        double z = Math.toRadians(degrees.z);
        float cx = (float) Math.cos(x);
        float sx = (float) Math.sin(x);
        float cy = (float) Math.cos(y);
        float sy = (float) Math.sin(y);
        float cz = (float) Math.cos(z);
        float sz = (float) Math.sin(z);

        out.set(
                cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx, 0f,
                sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx, 0f,
                -sy, cy * sx, cy * cx, 0f,
                0f, 0f, 0f, 1f);
    }

    // A turn about a unit axis by an angle in radians
    public static void toAxisRotation(Vector3 axis, float radians, Matrix4 out) {

        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float t = 1f - cos;
        float x = axis.x;
        float y = axis.y;
        float z = axis.z;

        out.set(
                t * x * x + cos, t * x * y - sin * z, t * x * z + sin * y, 0f,
                t * x * y + sin * z, t * y * y + cos, t * y * z - sin * x, 0f,
                t * x * z - sin * y, t * y * z + sin * x, t * z * z + cos, 0f,
                0f, 0f, 0f, 1f);
    }

    // A bone's local turn once a world turn is applied about its joint: inverse(parent) * turn * parent * local
    public static void applyWorldTurn(
            Matrix4 parentRotation,
            Matrix4 turn,
            Matrix4 local,
            Matrix4 out,
            Matrix4 scratch) {

        scratch.set(parentRotation).inverse();
        out.set(scratch).multiply(turn).multiply(parentRotation).multiply(local);
    }

    // A bone's local turn as the pose composes it: its bind rotation, then a keyed turn within it
    public static void composeBindTurn(Vector3 bindDegrees, Vector3 keyDegrees, Matrix4 out, Matrix4 scratch) {
        toRotation(bindDegrees, out);
        toRotation(keyDegrees, scratch);
        out.multiply(scratch);
    }

    // The keyed turn in Euler degrees that carries a bone from its bind rotation to a local turn, nearest a reference
    public static void toKeyEuler(
            Matrix4 local,
            Vector3 bindDegrees,
            Vector3 reference,
            Vector3 out,
            Matrix4 scratchA,
            Matrix4 scratchB) {

        toRotation(bindDegrees, scratchA);
        scratchB.set(scratchA).inverse().multiply(local);
        toEuler(scratchB, reference, out);
    }

    // Euler degrees of a pure turn composed Z, Y then X, in the form nearest a reference angle by angle
    public static void toEuler(Matrix4 rotation, Vector3 reference, Vector3 out) {

        float[] m = rotation.val;
        float sinY = Math.max(-1f, Math.min(1f, -m[2]));
        float y = (float) Math.asin(sinY);
        float x;
        float z;

        if (Math.abs(sinY) < EditorSetting.CHARACTER_VIEW_GIMBAL_LIMIT) {
            x = (float) Math.atan2(m[6], m[10]);
            z = (float) Math.atan2(m[1], m[0]);
        } else {
            x = 0f;
            z = (float) Math.atan2(-m[4], m[5]);
        }

        float ax = (float) Math.toDegrees(x);
        float ay = (float) Math.toDegrees(y);
        float az = (float) Math.toDegrees(z);
        float half = EditorSetting.CHARACTER_VIEW_HALF_TURN_DEGREES;
        float bx = unwrap(ax + half, reference.x);
        float by = unwrap(half - ay, reference.y);
        float bz = unwrap(az + half, reference.z);

        ax = unwrap(ax, reference.x);
        ay = unwrap(ay, reference.y);
        az = unwrap(az, reference.z);

        float first = Math.abs(ax - reference.x) + Math.abs(ay - reference.y) + Math.abs(az - reference.z);
        float second = Math.abs(bx - reference.x) + Math.abs(by - reference.y) + Math.abs(bz - reference.z);

        if (first <= second)
            out.set(ax, ay, az);
        else
            out.set(bx, by, bz);
    }

    // An angle moved by whole turns to lie within half a turn of a reference
    public static float unwrap(float degrees, float reference) {

        float turn = EditorSetting.CHARACTER_VIEW_HALF_TURN_DEGREES * 2f;

        return degrees + Math.round((reference - degrees) / turn) * turn;
    }
}
