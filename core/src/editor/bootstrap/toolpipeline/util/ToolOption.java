package editor.bootstrap.toolpipeline.util;

import editor.runtime.EditorSetting;

public enum ToolOption {

    /*
     * The settings a tool works with, shown under it in the Tools panel the
     * way a paint program's tool options are. Size is the brush tip's radius,
     * Strength how far each stamp takes what lies under it, and Twist how
     * far, in degrees, a swirl turns what lies at its centre. Each canvas
     * declares the range, step and starting value of every option it
     * honours, since each measures in its own units.
     */

    SIZE(EditorSetting.TOOL_OPTION_SIZE),
    STRENGTH(EditorSetting.TOOL_OPTION_STRENGTH),
    TWIST(EditorSetting.TOOL_OPTION_TWIST);

    // Values
    public static final ToolOption[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    ToolOption(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
