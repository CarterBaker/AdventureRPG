package editor.bootstrap.brushpipeline.util;

import editor.runtime.EditorSetting;

public enum BrushField {

    /*
     * The numbers a brush preset carries, each with its key in the preset
     * file, its label, the step the Brushes panel moves it by and the range
     * it is held to. Hardness is the share of the radius stamped at full
     * strength before the edge fades, Opacity the strength at the centre,
     * Density the share of covered pixels each stamp touches, Spacing the
     * gap between stamps as a share of the tip's width, and Twist how far, in
     * degrees, a swirl turns what lies at the centre of each stamp.
     */

    HARDNESS(
            EditorSetting.BRUSH_KEY_HARDNESS,
            EditorSetting.BRUSH_LABEL_HARDNESS,
            EditorSetting.BRUSH_UNIT_STEP,
            0f,
            1f,
            EditorSetting.BRUSH_HARDNESS_DEFAULT),
    OPACITY(
            EditorSetting.BRUSH_KEY_OPACITY,
            EditorSetting.BRUSH_LABEL_OPACITY,
            EditorSetting.BRUSH_UNIT_STEP,
            EditorSetting.BRUSH_UNIT_STEP,
            1f,
            EditorSetting.BRUSH_OPACITY_DEFAULT),
    DENSITY(
            EditorSetting.BRUSH_KEY_DENSITY,
            EditorSetting.BRUSH_LABEL_DENSITY,
            EditorSetting.BRUSH_UNIT_STEP,
            EditorSetting.BRUSH_UNIT_STEP,
            1f,
            EditorSetting.BRUSH_DENSITY_DEFAULT),
    SPACING(
            EditorSetting.BRUSH_KEY_SPACING,
            EditorSetting.BRUSH_LABEL_SPACING,
            EditorSetting.BRUSH_UNIT_STEP,
            EditorSetting.BRUSH_SPACING_MIN,
            1f,
            EditorSetting.BRUSH_SPACING_DEFAULT),
    TWIST(
            EditorSetting.BRUSH_KEY_TWIST,
            EditorSetting.BRUSH_LABEL_TWIST,
            EditorSetting.BRUSH_TWIST_STEP,
            -EditorSetting.BRUSH_TWIST_MAX,
            EditorSetting.BRUSH_TWIST_MAX,
            EditorSetting.BRUSH_TWIST_DEFAULT);

    // Values
    public static final BrushField[] VALUES = values();

    // Internal
    private final String key;
    private final String label;
    private final float step;
    private final float min;
    private final float max;
    private final float defaultValue;

    // Constructor \\

    BrushField(String key, String label, float step, float min, float max, float defaultValue) {
        this.key = key;
        this.label = label;
        this.step = step;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    // Range \\

    public float clamp(float value) {
        return Math.max(min, Math.min(max, value));
    }

    // Accessible \\

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public float getStep() {
        return step;
    }

    public float getDefaultValue() {
        return defaultValue;
    }
}
