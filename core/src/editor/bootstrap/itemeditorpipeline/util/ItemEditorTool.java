package editor.bootstrap.itemeditorpipeline.util;

import editor.runtime.EditorSetting;

public enum ItemEditorTool {

    /*
     * What a viewport click does: Place adds a cube of the selected part, Wall
     * adds a double-sided wall on the struck face or, near its edge, standing
     * on that edge, Erase removes the struck cube or wall, and Paint moves the
     * struck cube or wall into the selected part.
     */

    PLACE(EditorSetting.ITEM_EDITOR_TOOL_PLACE),
    WALL(EditorSetting.ITEM_EDITOR_TOOL_WALL),
    ERASE(EditorSetting.ITEM_EDITOR_TOOL_ERASE),
    PAINT(EditorSetting.ITEM_EDITOR_TOOL_PAINT);

    // Internal
    private final String label;

    // Constructor \\

    ItemEditorTool(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
