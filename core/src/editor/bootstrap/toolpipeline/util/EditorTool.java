package editor.bootstrap.toolpipeline.util;

import editor.runtime.EditorSetting;

public enum EditorTool {

    /*
     * The editor's tools, the same in every window that takes them; what
     * each does is the window's own. Select picks things to work on, Brush
     * strokes the brush preset over what it works on, Erase strokes it away,
     * Fill floods a run of one value, Pick takes up the value under the
     * pointer, Place stands a new thing where it clicks, and Wall stands a
     * thin wall on the face it clicks. Brush and Erase stroke along a drag;
     * the rest act once per click.
     */

    SELECT(EditorSetting.TOOL_SELECT, false),
    BRUSH(EditorSetting.TOOL_BRUSH, true),
    ERASE(EditorSetting.TOOL_ERASE, true),
    FILL(EditorSetting.TOOL_FILL, false),
    PICK(EditorSetting.TOOL_PICK, false),
    PLACE(EditorSetting.TOOL_PLACE, false),
    WALL(EditorSetting.TOOL_WALL, false);

    // Internal
    private final String label;
    private final boolean stroke;

    // Constructor \\

    EditorTool(String label, boolean stroke) {
        this.label = label;
        this.stroke = stroke;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isStroke() {
        return stroke;
    }
}
