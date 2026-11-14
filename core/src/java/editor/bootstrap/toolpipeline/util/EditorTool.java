package editor.bootstrap.toolpipeline.util;

import editor.runtime.EditorSetting;

public enum EditorTool {

    /*
     * The editor's tools, the same in every window that takes them; what
     * each does is the window's own, and each canvas takes only those that
     * mean something there. Select picks things to work on, sweeping the
     * brush's tip over them wherever the canvas sizes a brush; Brush
     * strokes the brush over what it works on, laying the chosen value
     * down, and Erase strokes it away; Smooth softens what lies under the
     * brush, Swirl twists it about the brush's centre and Scatter shuffles
     * it; Inflate swells a surface along its own normals, Pinch draws it in
     * toward the brush's centre, Flatten levels it, a surface onto its mean
     * plane and an image to the value where the stroke began, and Nudge
     * drags it along with the pointer; Raise and Lower build what lies
     * under the brush up or wear it down a little more with every stamp;
     * Fill floods a run of one value and Pick takes up the value under the
     * pointer; Place stands a new thing where it clicks and Wall a thin
     * wall on the face it clicks; Move, Rotate and Scale drag what is
     * selected; Cut cuts a clip in two where it clicks; IK Handle drags the
     * end of a limb or digit the rig carries a handle for, and the joints
     * above it bend the way the handle's pole says; Draw draws what the
     * canvas holds where it is dragged, a note on a piano roll or a notes
     * clip across a Music file's empty lane. The stroking tools
     * work along a drag with a brush and carry the options that shape their
     * strokes; the rest act once per press.
     */

    SELECT(EditorSetting.TOOL_SELECT, false, ToolOption.SIZE, ToolOption.SNAP, ToolOption.SCALE_LOCK),
    BRUSH(EditorSetting.TOOL_BRUSH, true, ToolOption.SIZE, ToolOption.STRENGTH),
    ERASE(EditorSetting.TOOL_ERASE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    SMOOTH(EditorSetting.TOOL_SMOOTH, true, ToolOption.SIZE, ToolOption.STRENGTH),
    SWIRL(EditorSetting.TOOL_SWIRL, true, ToolOption.SIZE, ToolOption.STRENGTH, ToolOption.TWIST),
    SCATTER(EditorSetting.TOOL_SCATTER, true, ToolOption.SIZE, ToolOption.STRENGTH),
    INFLATE(EditorSetting.TOOL_INFLATE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    PINCH(EditorSetting.TOOL_PINCH, true, ToolOption.SIZE, ToolOption.STRENGTH),
    CREASE(EditorSetting.TOOL_CREASE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    RELAX(EditorSetting.TOOL_RELAX, true, ToolOption.SIZE, ToolOption.STRENGTH),
    FLATTEN(EditorSetting.TOOL_FLATTEN, true, ToolOption.SIZE, ToolOption.STRENGTH),
    NUDGE(EditorSetting.TOOL_NUDGE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    RAISE(EditorSetting.TOOL_RAISE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    LOWER(EditorSetting.TOOL_LOWER, true, ToolOption.SIZE, ToolOption.STRENGTH),
    FILL(EditorSetting.TOOL_FILL, false),
    PICK(EditorSetting.TOOL_PICK, false),
    PLACE(EditorSetting.TOOL_PLACE, false),
    WALL(EditorSetting.TOOL_WALL, false),
    MOVE(EditorSetting.TOOL_MOVE, false),
    ROTATE(EditorSetting.TOOL_ROTATE, false),
    SCALE(EditorSetting.TOOL_SCALE, false),
    CUT(EditorSetting.TOOL_CUT, false, ToolOption.SNAP),
    IK(EditorSetting.TOOL_IK, false),
    DRAW(EditorSetting.TOOL_DRAW, false, ToolOption.SNAP, ToolOption.NOTE_LENGTH, ToolOption.VELOCITY,
            ToolOption.SCALE_LOCK);

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

    public boolean hasOption(ToolOption option) {

        for (int i = 0; i < options.length; i++)
            if (options[i] == option)
                return true;

        return false;
    }
}
