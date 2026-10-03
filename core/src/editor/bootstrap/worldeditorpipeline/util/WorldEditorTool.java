package editor.bootstrap.worldeditorpipeline.util;

import editor.runtime.EditorSetting;

public enum WorldEditorTool {

    /*
     * What a world image click does: Brush paints the selected biome's color
     * along the drag, Fill floods the run of one color under the pointer with
     * it, and Pick selects the biome that paints the pixel under the pointer.
     */

    BRUSH(EditorSetting.WORLD_EDITOR_TOOL_BRUSH),
    FILL(EditorSetting.WORLD_EDITOR_TOOL_FILL),
    PICK(EditorSetting.WORLD_EDITOR_TOOL_PICK);

    // Internal
    private final String label;

    // Constructor \\

    WorldEditorTool(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
