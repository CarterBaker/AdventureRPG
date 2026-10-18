package editor.bootstrap.charactereditorpipeline.util;

import editor.runtime.EditorSetting;

public enum CharacterEditorWeightTool {

    /*
     * What dragging over the viewport does in Weights mode: Select picks
     * vertices as in Mesh mode, Paint draws the selected bone's weight toward
     * the target weight, and Smooth blends each vertex's weights toward its
     * neighbours', both under a brush that falls off from its centre.
     */

    SELECT(EditorSetting.CHARACTER_EDITOR_WEIGHT_TOOL_SELECT),
    PAINT(EditorSetting.CHARACTER_EDITOR_WEIGHT_TOOL_PAINT),
    SMOOTH(EditorSetting.CHARACTER_EDITOR_WEIGHT_TOOL_SMOOTH);

    // Values
    public static final CharacterEditorWeightTool[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    CharacterEditorWeightTool(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isBrush() {
        return this != SELECT;
    }
}
