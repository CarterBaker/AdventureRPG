package editor.bootstrap.imagepipeline.util;

import editor.runtime.EditorSetting;

public enum MapGenerateOption {

    /*
     * Every setting that turns an image into one of its maps, as a map
     * generator (Materialize, Bitmap2Material) offers them: how the height is
     * read off the image's brightness (the size of the forms kept, a
     * softening blur, its contrast), Invert, which reads the brightness the
     * other way round (dark high, so a normal's relief turns inside out, or a
     * roughness, glossiness or opacity read from the darks), where a
     * displacement's level stands, the fine detail a bump keeps, how strongly
     * the normal bends, how dark and how wide occlusion and cavities gather,
     * how sharply curvature shows, how far thickness reaches, the
     * roughness's base and how far the image's darks raise it, the metallic
     * and specular levels, where and how brightly the image glows, how
     * opacity is read and cut, and how much a tint mask takes. Each map takes
     * the settings ImageMap lists for it, its own copy of each; every setting
     * has a range, a step and a default, and a switch is 0 or 1. Radii are in
     * the image's own pixels.
     */

    HEIGHT_DETAIL("Height Detail", 2f, 64f, 1f, 24f, false),
    HEIGHT_SOFTNESS("Height Softness", 0f, 8f, 1f, 1f, false),
    HEIGHT_CONTRAST("Height Contrast", 0.2f, 4f, 0.05f, 1.5f, false),
    HEIGHT_INVERT("Invert", 0f, 1f, 1f, 0f, true),
    MID_LEVEL("Mid Level", 0f, 1f, 0.01f, 0.5f, false),
    BUMP_DETAIL("Bump Detail", 1f, 16f, 1f, 2f, false),
    NORMAL_STRENGTH("Normal Strength", 0f, 16f, 0.25f, 4f, false),
    OCCLUSION_STRENGTH("Occlusion Strength", 0f, 4f, 0.05f, 1.5f, false),
    OCCLUSION_RADIUS("Occlusion Radius", 1f, 32f, 1f, 8f, false),
    CAVITY_RADIUS("Cavity Radius", 1f, 8f, 1f, 2f, false),
    CURVATURE_STRENGTH("Curvature Strength", 0f, 16f, 0.25f, 4f, false),
    THICKNESS_RADIUS("Thickness Radius", 1f, 64f, 1f, 16f, false),
    ROUGHNESS_BASE("Roughness", 0f, 1f, 0.01f, 0.6f, false),
    ROUGHNESS_VARIATION("Roughness Variation", 0f, 1f, 0.01f, 0.3f, false),
    METALLIC("Metallic", 0f, 1f, 0.01f, 0f, false),
    SPECULAR_LEVEL("Specular Level", 0f, 1f, 0.01f, 0.5f, false),
    GLOW_THRESHOLD("Glow Threshold", 0f, 1f, 0.01f, 0.85f, false),
    GLOW_SOFTNESS("Glow Softness", 0f, 0.5f, 0.01f, 0.05f, false),
    GLOW_STRENGTH("Glow Strength", 0f, 4f, 0.05f, 1f, false),
    OPACITY_FROM_BRIGHTNESS("Opacity From Brightness", 0f, 1f, 1f, 0f, true),
    OPACITY_CUTOFF("Opacity Cutoff", 0f, 1f, 0.01f, 0f, false),
    TINT_AMOUNT("Tint Amount", 0f, 1f, 0.01f, 1f, false);

    // Values
    public static final MapGenerateOption[] VALUES = values();

    // Internal
    private final String label;
    private final float min;
    private final float max;
    private final float step;
    private final float defaultValue;
    private final boolean toggle;

    // Constructor \\

    MapGenerateOption(String label, float min, float max, float step, float defaultValue, boolean toggle) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.defaultValue = defaultValue;
        this.toggle = toggle;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getStep() {
        return step;
    }

    public float getDefault() {
        return defaultValue;
    }

    public boolean isToggle() {
        return toggle;
    }

    // Every setting at its default, indexed by ordinal, as a map starts
    public static float[] createDefaults() {

        float[] values = new float[VALUES.length];

        for (int i = 0; i < VALUES.length; i++)
            values[i] = VALUES[i].defaultValue;

        return values;
    }

    // This setting's value among a map's settings, and whether a switch there is on
    public float get(float[] values) {
        return values[ordinal()];
    }

    public boolean isOn(float[] values) {
        return values[ordinal()] >= EditorSetting.MAP_GENERATE_TOGGLE_ON;
    }

    // A value held to the range and laid on the step
    public float clamp(float value) {
        return Math.clamp(min + Math.round((value - min) / step) * step, min, max);
    }

    public String formatValue(float value) {

        if (!toggle)
            return String.format(EditorSetting.MAP_GENERATE_VALUE_FORMAT, value);

        return value >= EditorSetting.MAP_GENERATE_TOGGLE_ON
                ? EditorSetting.CHARACTER_VIEW_YES
                : EditorSetting.CHARACTER_VIEW_NO;
    }
}
