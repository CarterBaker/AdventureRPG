package editor.bootstrap.toolpipeline.tool;

import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import engine.root.StructPackage;

public class ToolOptionStruct extends StructPackage {

    /*
     * One option a canvas honours, with the range it is held to there, the
     * step the Tools panel and the brush size keys move it by, and the value
     * it starts at. A slider sets it by its share of the range, landing on
     * the nearest step.
     */

    // Option
    private final ToolOption option;
    private final float min;
    private final float max;
    private final float step;
    private final float defaultValue;

    // Constructor \\

    public ToolOptionStruct(ToolOption option, float min, float max, float step, float defaultValue) {

        // Option
        this.option = option;
        this.min = min;
        this.max = max;
        this.step = step;
        this.defaultValue = defaultValue;
    }

    // Range \\

    // A value one step from another, held to the range
    public float step(float value, int direction) {
        return clamp(value + direction * step);
    }

    public float clamp(float value) {
        return Math.max(min, Math.min(max, value));
    }

    // The value a share of the range stands for, on the step nearest it
    public float fromFraction(float fraction) {
        return clamp(min + Math.round(Math.clamp(fraction, 0f, 1f) * (max - min) / step) * step);
    }

    // The share of the range a value lies at
    public float toFraction(float value) {
        return max > min ? (clamp(value) - min) / (max - min) : 0f;
    }

    // Text \\

    // Whole numbers as they are, fractions to the precision the Tools panel shows and keeps
    public String format(float value) {

        float precision = EditorSetting.TOOL_OPTION_PRECISION;

        return value == Math.round(value)
                ? Integer.toString(Math.round(value))
                : Float.toString(Math.round(value * precision) / precision);
    }

    // Accessible \\

    public ToolOption getOption() {
        return option;
    }

    public float getDefaultValue() {
        return defaultValue;
    }
}
