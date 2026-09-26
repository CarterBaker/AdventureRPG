package editor.bootstrap.itemeditorpipeline.util;

import editor.runtime.EditorSetting;

public enum ItemEditorTool {

    /*
     * What a viewport click does: Place adds a cube of the selected part, Erase
     * removes the struck cube, Paint moves the struck cube into the selected part.
     */

    PLACE(EditorSetting.ITEM_EDITOR_TOOL_PLACE),
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
