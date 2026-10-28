package editor.bootstrap.charactereditorpipeline.util;

import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.EditorSetting;

public enum CharacterEditorTransform {

    /*
     * The ways a mesh selection follows the pointer while a transform is
     * held: Grab moves it, Rotate turns it about its centre, and Scale grows
     * or shrinks it from its centre, each free or held to one axis. Each is
     * also the tool that drags it, Move, Rotate or Scale.
     */

    GRAB(EditorSetting.CHARACTER_EDITOR_TRANSFORM_GRAB, EditorTool.MOVE),
    ROTATE(EditorSetting.CHARACTER_EDITOR_TRANSFORM_ROTATE, EditorTool.ROTATE),
    SCALE(EditorSetting.CHARACTER_EDITOR_TRANSFORM_SCALE, EditorTool.SCALE);

    // Values
    public static final CharacterEditorTransform[] VALUES = values();

    // Internal
    private final String label;
    private final EditorTool tool;

    // Constructor \\

    CharacterEditorTransform(String label, EditorTool tool) {
        this.label = label;
        this.tool = tool;
    }

    // Tool \\

    // The transform a tool drags, null for a tool that drags none
    public static CharacterEditorTransform forTool(EditorTool tool) {

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].tool == tool)
                return VALUES[i];

        return null;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
