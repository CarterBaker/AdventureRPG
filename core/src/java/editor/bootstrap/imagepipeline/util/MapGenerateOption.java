package editor.bootstrap.imagepipeline.util;

import editor.runtime.EditorSetting;

public enum MapGenerateOption {

    /*
     * The settings that turn an albedo into the material maps beside it, as
     * a map generator (Materialize, Bitmap2Material) offers them: how the
     * height is read off the image's brightness (the size of the forms kept,
     * a softening blur, its contrast, and whether dark is high), how strongly
     * the normal map bends, how dark and how wide the occlusion gathers in
     * the height's hollows, the roughness's base and how far the image's
     * darks raise it, the metallic value, and which maps are written. Each
     * has a range, a step and a default; a switch is 0 or 1.
     */

    HEIGHT_DETAIL("Height Detail", 2f, 64f, 1f, 24f, false),
    HEIGHT_SOFTNESS("Height Softness", 0f, 8f, 1f, 1f, false),
    HEIGHT_CONTRAST("Height Contrast", 0.2f, 4f, 0.05f, 1.5f, false),
    HEIGHT_INVERT("Dark Is High", 0f, 1f, 1f, 0f, true),
    NORMAL_STRENGTH("Normal Strength", 0f, 16f, 0.25f, 4f, false),
    OCCLUSION_STRENGTH("Occlusion Strength", 0f, 4f, 0.05f, 1.5f, false),
    OCCLUSION_RADIUS("Occlusion Radius", 1f, 32f, 1f, 8f, false),
    ROUGHNESS_BASE("Roughness", 0f, 1f, 0.01f, 0.6f, false),
    ROUGHNESS_VARIATION("Roughness Variation", 0f, 1f, 0.01f, 0.3f, false),
    METALLIC("Metallic", 0f, 1f, 0.01f, 0f, false),
    WRITE_NORMAL("Write Normal", 0f, 1f, 1f, 1f, true),
    WRITE_HEIGHT("Write Height", 0f, 1f, 1f, 1f, true),
    WRITE_OCCLUSION("Write Occlusion", 0f, 1f, 1f, 1f, true),
    WRITE_ROUGHNESS("Write Roughness", 0f, 1f, 1f, 1f, true),
    WRITE_METALLIC("Write Metallic", 0f, 1f, 1f, 0f, true);

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
