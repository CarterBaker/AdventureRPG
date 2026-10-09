package editor.bootstrap.worldeditorpipeline.util;

import editor.runtime.EditorSetting;

public enum WorldEditorTool {

    /*
     * What a world image click does: Brush paints the selected biome's color
     * along the drag, Fill floods the run of one color under the pointer with
     * it, Pick selects the biome that paints the pixel under the pointer,
     * Place stands the selected settlement or structure on the block column
     * under the pointer, and Clear takes away the hand placement nearest it.
     */

    BRUSH(EditorSetting.WORLD_EDITOR_TOOL_BRUSH, false),
    FILL(EditorSetting.WORLD_EDITOR_TOOL_FILL, false),
    PICK(EditorSetting.WORLD_EDITOR_TOOL_PICK, false),
    PLACE(EditorSetting.WORLD_EDITOR_TOOL_PLACE, true),
    CLEAR(EditorSetting.WORLD_EDITOR_TOOL_CLEAR, true);

    // Internal
    private final String label;
    private final boolean placement;

    // Constructor \\

    WorldEditorTool(String label, boolean placement) {
        this.label = label;
        this.placement = placement;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // True for the tools that place by hand on the world rather than paint its image
    public boolean isPlacement() {
        return placement;
    }
}
