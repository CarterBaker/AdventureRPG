package editor.bootstrap.worldeditorpipeline.util;

import editor.runtime.EditorSetting;

public enum WorldEditorTool {

    /*
     * What a world image click does, each tool belonging to one mode. In
     * Paint, Brush paints the selected biome's color, or on the height layer
     * the selected level, along the drag, Fill floods the run of one color
     * under the pointer with it, Pick selects the biome or level that paints
     * the pixel under the pointer, and Smooth softens the height layer along
     * the drag. In Settlements, Place stands the Settlement Planner's design
     * on the block column under the pointer, Select lists every hand
     * placement in the pixel under it in the planner and selects the first,
     * Move stands the selected placement on the column under it instead, and
     * Clear takes away the placement nearest it.
     */

    BRUSH(EditorSetting.WORLD_EDITOR_TOOL_BRUSH, WorldEditorMode.PAINT),
    FILL(EditorSetting.WORLD_EDITOR_TOOL_FILL, WorldEditorMode.PAINT),
    PICK(EditorSetting.WORLD_EDITOR_TOOL_PICK, WorldEditorMode.PAINT),
    SMOOTH(EditorSetting.WORLD_EDITOR_TOOL_SMOOTH, WorldEditorMode.PAINT),
    PLACE(EditorSetting.WORLD_EDITOR_TOOL_PLACE, WorldEditorMode.SETTLEMENTS),
    SELECT(EditorSetting.WORLD_EDITOR_TOOL_SELECT, WorldEditorMode.SETTLEMENTS),
    MOVE(EditorSetting.WORLD_EDITOR_TOOL_MOVE, WorldEditorMode.SETTLEMENTS),
    CLEAR(EditorSetting.WORLD_EDITOR_TOOL_CLEAR, WorldEditorMode.SETTLEMENTS);

    // Internal
    private final String label;
    private final WorldEditorMode mode;

    // Constructor \\

    WorldEditorTool(String label, WorldEditorMode mode) {
        this.label = label;
        this.mode = mode;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public WorldEditorMode getMode() {
        return mode;
    }

    // True for the tools that place by hand on the world rather than paint its image
    public boolean isPlacement() {
        return mode == WorldEditorMode.SETTLEMENTS;
    }
}
