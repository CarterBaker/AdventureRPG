package editor.bootstrap.toolpipeline.tool;

import editor.bootstrap.toolpipeline.util.ToolOption;
import engine.root.StructPackage;

public class ToolOptionStruct extends StructPackage {

    /*
     * One option a canvas honours, with the range it is held to there, the
     * step the Tools panel and the brush size keys move it by, and the value
     * it starts at.
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

    // Accessible \\

    public ToolOption getOption() {
        return option;
    }

    public float getDefaultValue() {
        return defaultValue;
    }
}
