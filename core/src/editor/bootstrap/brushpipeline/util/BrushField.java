package editor.bootstrap.brushpipeline.util;

import editor.runtime.EditorSetting;

public enum BrushField {

    /*
     * The numbers a brush tip carries, each with its key in the preset file,
     * the range it is held to and the value it takes when the file leaves it
     * out. Hardness is the share of the radius stamped at full strength
     * before the edge fades, Density the share of covered pixels each stamp
     * touches, and Spacing the gap between stamps as a share of the tip's
     * width. How strongly and how widely a tip strokes is the tool's.
     */

    HARDNESS(EditorSetting.BRUSH_KEY_HARDNESS, 0f, 1f, EditorSetting.BRUSH_HARDNESS_DEFAULT),
    DENSITY(EditorSetting.BRUSH_KEY_DENSITY, EditorSetting.BRUSH_FIELD_MIN, 1f, EditorSetting.BRUSH_DENSITY_DEFAULT),
    SPACING(EditorSetting.BRUSH_KEY_SPACING, EditorSetting.BRUSH_FIELD_MIN, 1f, EditorSetting.BRUSH_SPACING_DEFAULT);

    // Values
    public static final BrushField[] VALUES = values();

    // Internal
    private final String key;
    private final float min;
    private final float max;
    private final float defaultValue;

    // Constructor \\

    BrushField(String key, float min, float max, float defaultValue) {
        this.key = key;
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

    public float getDefaultValue() {
        return defaultValue;
    }
}
