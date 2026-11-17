package editor.bootstrap.toolpipeline.tool;

import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.bootstrap.toolpipeline.util.ToolOptionKind;
import editor.runtime.EditorSetting;
import engine.root.StructPackage;

public class ToolOptionStruct extends StructPackage {

    /*
     * One option a canvas honours, with the range it is held to there, the
     * step the Tools panel and the brush size keys move it by, and the value
     * it starts at. A slider sets it by its share of the range, landing on
     * the nearest step. An option of named steps — a switch's off and on, or
     * a choice such as how bones are drawn — shows and is typed by the name
     * of each whole step from its low end. Its kind says how the Tools panel
     * shows it: a number on a slider, a switch, or a drop-down of choices.
     */

    // Option
    private final ToolOption option;
    private final ToolOptionKind kind;
    private final float min;
    private final float max;
    private final float step;
    private final float defaultValue;

    // Names — one for each whole step from the low end, null for an option shown as its number
    private final String[] names;

    // Constructor \\

    public ToolOptionStruct(ToolOption option, float min, float max, float step, float defaultValue) {
        this(option, ToolOptionKind.NUMBER, min, max, step, defaultValue, null);
    }

    // An option of named steps, a switch or a choice, from 0 to the last name, starting at the step given
    public ToolOptionStruct(ToolOption option, ToolOptionKind kind, String[] names, int defaultIndex) {
        this(option, kind, 0f, names.length - 1, 1f, defaultIndex, names);
    }

    private ToolOptionStruct(
            ToolOption option,
            ToolOptionKind kind,
            float min,
            float max,
            float step,
            float defaultValue,
            String[] names) {

        // Option
        this.option = option;
        this.kind = kind;
        this.min = min;
        this.max = max;
        this.step = step;
        this.defaultValue = defaultValue;

        // Names
        this.names = names;
    }

    // Range \\

    // A value one step from another, held to the range
    public float step(float value, int direction) {
        return clamp(value + direction * step);
    }

    // The value one step on from another, past the high end back to the low, as a flip turns over
    public float cycle(float value) {

        float next = value + step;

        return next > max ? min : clamp(next);
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

    // A named step by its name, whole numbers as they are, fractions to the precision the Tools panel shows and keeps
    public String format(float value) {

        if (names != null)
            return names[Math.round(clamp(value))];

        float precision = EditorSetting.TOOL_OPTION_PRECISION;

        return value == Math.round(value)
                ? Integer.toString(Math.round(value))
                : Float.toString(Math.round(value * precision) / precision);
    }

    // The value typed: a named step by its name, ignoring case, else a number; not a number for neither
    public float parse(String text) {

        if (names != null)
            for (int i = 0; i < names.length; i++)
                if (names[i].equalsIgnoreCase(text.trim()))
                    return i;

        try {
            float value = Float.parseFloat(text.trim());
            return Float.isFinite(value) ? value : Float.NaN;
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    public boolean accepts(String text) {
        return !Float.isNaN(parse(text));
    }

    // Accessible \\

    public ToolOption getOption() {
        return option;
    }

    public ToolOptionKind getKind() {
        return kind;
    }

    // How many named steps a switch or a choice has, none for a number
    public int getChoiceCount() {
        return names != null ? names.length : 0;
    }

    public float getDefaultValue() {
        return defaultValue;
    }
}
