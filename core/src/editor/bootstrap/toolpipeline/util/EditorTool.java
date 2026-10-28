package editor.bootstrap.toolpipeline.util;

import editor.runtime.EditorSetting;

public enum EditorTool {

    /*
     * The editor's tools, the same in every window that takes them; what
     * each does is the window's own, and each canvas takes only those that
     * mean something there. Select picks things to work on; Brush strokes
     * the brush over what it works on, laying the chosen value down, and
     * Erase strokes it away; Smooth softens what lies under the brush,
     * Swirl twists it about the brush's centre and Scatter shuffles it;
     * Fill floods a run of one value and Pick takes up the value under the
     * pointer; Place stands a new thing where it clicks and Wall a thin wall
     * on the face it clicks; Move, Rotate and Scale drag what is selected;
     * Cut cuts a clip in two where it clicks. The stroking tools work along
     * a drag with a brush and carry the options that shape their strokes;
     * the rest act once per press.
     */

    SELECT(EditorSetting.TOOL_SELECT, false),
    BRUSH(EditorSetting.TOOL_BRUSH, true, ToolOption.SIZE, ToolOption.STRENGTH),
    ERASE(EditorSetting.TOOL_ERASE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    SMOOTH(EditorSetting.TOOL_SMOOTH, true, ToolOption.SIZE, ToolOption.STRENGTH),
    SWIRL(EditorSetting.TOOL_SWIRL, true, ToolOption.SIZE, ToolOption.STRENGTH, ToolOption.TWIST),
    SCATTER(EditorSetting.TOOL_SCATTER, true, ToolOption.SIZE, ToolOption.STRENGTH),
    FILL(EditorSetting.TOOL_FILL, false),
    PICK(EditorSetting.TOOL_PICK, false),
    PLACE(EditorSetting.TOOL_PLACE, false),
    WALL(EditorSetting.TOOL_WALL, false),
    MOVE(EditorSetting.TOOL_MOVE, false),
    ROTATE(EditorSetting.TOOL_ROTATE, false),
    SCALE(EditorSetting.TOOL_SCALE, false),
    CUT(EditorSetting.TOOL_CUT, false);

    // Values
    public static final EditorTool[] VALUES = values();

    // Internal
    private final String label;
    private final boolean stroke;
    private final ToolOption[] options;

    // Constructor \\

    EditorTool(String label, boolean stroke, ToolOption... options) {
        this.label = label;
        this.stroke = stroke;
        this.options = options;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isStroke() {
        return stroke;
    }

    public ToolOption[] getOptions() {
        return options;
    }
}
