package application.bootstrap.weatherpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;

public final class SkyColorUtility extends EngineUtility {

    /*
     * Stateless color and curve math shared by the sky palette: scalar and
     * color interpolation, eased ramps and bell curves over solar
     * elevation, and luminance-preserving saturation and haze. Every color
     * operation writes in place so nothing allocates per frame.
     */

    // Scalar \\

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    public static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static float remapClamped(float value, float low, float high) {

        if (high <= low)
            return value >= high ? 1f : 0f;

        return clamp01((value - low) / (high - low));
    }

    public static float smoothstep(float edge0, float edge1, float value) {
        float t = remapClamped(value, edge0, edge1);
        return t * t * (3f - 2f * t);
    }

    // Rises from start to a full peak, then falls back to zero at end.
    public static float bell(float value, float start, float peak, float end) {

        if (value <= start || value >= end)
            return 0f;

        return value <= peak
                ? smoothstep(start, peak, value)
                : 1f - smoothstep(peak, end, value);
    }

    // Color \\

    public static void lerp(Vector3 target, Vector3 from, Vector3 to, float t) {
        target.set(
                lerp(from.x, to.x, t),
                lerp(from.y, to.y, t),
                lerp(from.z, to.z, t));
    }

    public static void lerpTowards(Vector3 target, Vector3 goal, float t) {
        lerp(target, target, goal, t);
    }

    public static float luminance(Vector3 color) {
        return color.x * EngineSetting.SKY_LUMINANCE_R
                + color.y * EngineSetting.SKY_LUMINANCE_G
                + color.z * EngineSetting.SKY_LUMINANCE_B;
    }

    // Scales each channel's distance from the color's own luminance — below
    // 1 washes toward grey, above 1 deepens the hue, brightness unchanged.
    public static void saturate(Vector3 color, float amount) {

        float gray = luminance(color);

        color.set(
                gray + (color.x - gray) * amount,
                gray + (color.y - gray) * amount,
                gray + (color.z - gray) * amount);
    }

    // Washes a color toward a pale, brightened grey of its own luminance,
    // the way humid or hot air flattens a sky toward the horizon.
    public static void haze(Vector3 color, float amount) {

        float gray = luminance(color);
        float target = gray + (1f - gray) * EngineSetting.SKY_HAZE_LIFT;

        color.set(
                lerp(color.x, target, amount),
                lerp(color.y, target, amount),
                lerp(color.z, target, amount));
    }

    // Rotates a color's hue around the grey axis, keeping its overall
    // brightness and saturation. Positive degrees turn red toward yellow and green;
    // negative degrees turn orange toward red, pink, and violet.
    public static void rotateHue(Vector3 color, float degrees) {

        double radians = Math.toRadians(degrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float shared = (1f - cos) / 3f;
        float axial = sin * EngineSetting.SKY_HUE_AXIS_INVERSE_ROOT;

        float diagonal = cos + shared;
        float leading = shared + axial;
        float trailing = shared - axial;

        color.set(
                color.x * diagonal + color.y * trailing + color.z * leading,
                color.x * leading + color.y * diagonal + color.z * trailing,
                color.x * trailing + color.y * leading + color.z * diagonal);

        clampPositive(color);
    }

    public static void clampPositive(Vector3 color) {
        color.set(Math.max(0f, color.x), Math.max(0f, color.y), Math.max(0f, color.z));
    }
}
