package application.bootstrap.entitypipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class BoneControlCurveUtility extends EngineUtility {

    /*
     * The curve a bone slider follows as a control moves: a smooth monotone
     * cubic (Fritsch and Carlson) through the start (0, 0), the drive's keys
     * and the end (1, 1), a key at either end taking that end's place, so
     * the curve passes through every key without overshooting between them.
     * The caller hands in a scratch array of at least
     * BONE_CONTROL_CURVE_SCRATCH_FLOATS floats, so evaluating allocates
     * nothing.
     */

    // Curve \\

    // The share of the slider's range reached at a place along the control, both from 0 to 1
    public static float evaluate(float[] keys, float place, float[] scratch) {

        int count = gatherPoints(keys, scratch);
        int x = 0;
        int y = EngineSetting.BONE_CONTROL_CURVE_POINTS;
        int m = EngineSetting.BONE_CONTROL_CURVE_POINTS * 2;
        int d = EngineSetting.BONE_CONTROL_CURVE_POINTS * 3;
        float t = Math.max(0f, Math.min(1f, place));

        if (count == 1)
            return scratch[y];

        resolveTangents(scratch, count, x, y, m, d);

        int segment = 0;

        while (segment < count - 2 && t > scratch[x + segment + 1])
            segment++;

        float x0 = scratch[x + segment];
        float span = scratch[x + segment + 1] - x0;
        float s = span > 0f ? (t - x0) / span : 0f;
        float s2 = s * s;
        float s3 = s2 * s;

        return (2f * s3 - 3f * s2 + 1f) * scratch[y + segment]
                + (s3 - 2f * s2 + s) * span * scratch[m + segment]
                + (-2f * s3 + 3f * s2) * scratch[y + segment + 1]
                + (s3 - s2) * span * scratch[m + segment + 1];
    }

    // The ends and the keys in order of their places into the scratch's first two rows, a key on an end replacing it
    private static int gatherPoints(float[] keys, float[] scratch) {

        int count = 0;
        int keyCount = keys.length / 2;
        boolean startKeyed = keyCount > 0 && keys[0] <= 0f;
        boolean endKeyed = keyCount > 0 && keys[keys.length - 2] >= 1f;

        if (!startKeyed)
            count = putPoint(scratch, count, 0f, 0f);

        for (int k = 0; k < keyCount; k++)
            count = putPoint(scratch, count, keys[k * 2], keys[k * 2 + 1]);

        if (!endKeyed)
            count = putPoint(scratch, count, 1f, 1f);

        return count;
    }

    private static int putPoint(float[] scratch, int count, float place, float share) {
        scratch[count] = place;
        scratch[EngineSetting.BONE_CONTROL_CURVE_POINTS + count] = share;
        return count + 1;
    }

    // Fritsch-Carlson tangents: the mean slope at each inner point, none at a turn, held so no segment overshoots
    private static void resolveTangents(float[] scratch, int count, int x, int y, int m, int d) {

        for (int i = 0; i < count - 1; i++) {

            float span = scratch[x + i + 1] - scratch[x + i];
            scratch[d + i] = span > 0f ? (scratch[y + i + 1] - scratch[y + i]) / span : 0f;
        }

        scratch[m] = scratch[d];
        scratch[m + count - 1] = scratch[d + count - 2];

        for (int i = 1; i < count - 1; i++)
            scratch[m + i] = scratch[d + i - 1] * scratch[d + i] <= 0f
                    ? 0f
                    : (scratch[d + i - 1] + scratch[d + i]) * 0.5f;

        for (int i = 0; i < count - 1; i++) {

            float slope = scratch[d + i];

            if (slope == 0f) {
                scratch[m + i] = 0f;
                scratch[m + i + 1] = 0f;
                continue;
            }

            float a = scratch[m + i] / slope;
            float b = scratch[m + i + 1] / slope;
            float length = a * a + b * b;

            if (length > 9f) {
                float tau = 3f / (float) Math.sqrt(length);
                scratch[m + i] = tau * a * slope;
                scratch[m + i + 1] = tau * b * slope;
            }
        }
    }
}
