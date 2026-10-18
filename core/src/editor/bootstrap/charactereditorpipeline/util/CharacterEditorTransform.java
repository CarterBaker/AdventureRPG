package editor.bootstrap.charactereditorpipeline.util;

import editor.runtime.EditorSetting;

public enum CharacterEditorTransform {

    /*
     * The ways a mesh selection follows the pointer while a transform is
     * held: Grab moves it, Rotate turns it about its centre, and Scale grows
     * or shrinks it from its centre, each free or held to one axis.
     */

    GRAB(EditorSetting.CHARACTER_EDITOR_TRANSFORM_GRAB),
    ROTATE(EditorSetting.CHARACTER_EDITOR_TRANSFORM_ROTATE),
    SCALE(EditorSetting.CHARACTER_EDITOR_TRANSFORM_SCALE);

    // Internal
    private final String label;

    // Constructor \\

    CharacterEditorTransform(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
