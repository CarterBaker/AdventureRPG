package editor.bootstrap.toolpipeline.util;

import editor.runtime.EditorSetting;

public enum ToolOption {

    /*
     * The settings a tool works with, shown under it in the Tools panel the
     * way a paint program's tool options are. Size is the brush tip's radius,
     * Strength how far each stamp takes what lies under it, and Twist how
     * far, in degrees, a swirl turns what lies at its centre; Scale is how
     * large a body part is placed, Turn how far it turns about the loop it
     * joins by, Comb which way laid fur lies along the surface, and Detail
     * the face size the topology brush evens faces toward. Mirror makes
     * every edit to one side of a character to its twin across the body as
     * well, X-Ray picks through the surface as if it were glass, Wireframe
     * draws the edges of the mesh worked over its surface, and Bones chooses
     * how the rig's bones are drawn. Each canvas declares the range, step and
     * starting value of every option it honours, since each measures in its
     * own units. Most options belong to the tools that carry them; a canvas
     * option belongs to every tool on the canvases that honour it, and is
     * listed under the tool's own.
     */

    SIZE(EditorSetting.TOOL_OPTION_SIZE),
    STRENGTH(EditorSetting.TOOL_OPTION_STRENGTH),
    TWIST(EditorSetting.TOOL_OPTION_TWIST),
    SCALE(EditorSetting.TOOL_OPTION_SCALE),
    COMB(EditorSetting.TOOL_OPTION_COMB),
    TURN(EditorSetting.TOOL_OPTION_TURN),
    DETAIL(EditorSetting.TOOL_OPTION_DETAIL),
    MIRROR(EditorSetting.TOOL_OPTION_MIRROR, true),
    XRAY(EditorSetting.TOOL_OPTION_XRAY, true),
    WIREFRAME(EditorSetting.TOOL_OPTION_WIREFRAME, true),
    BONES(EditorSetting.TOOL_OPTION_BONES, true);

    // Values
    public static final ToolOption[] VALUES = values();

    // Internal
    private final String label;
    private final boolean canvasWide;

    // Constructor \\

    ToolOption(String label) {
        this(label, false);
    }

    ToolOption(String label, boolean canvasWide) {
        this.label = label;
        this.canvasWide = canvasWide;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // Whether every tool on a canvas that honours the option takes it, rather than only the tools that carry it
    public boolean isCanvasWide() {
        return canvasWide;
    }
}
