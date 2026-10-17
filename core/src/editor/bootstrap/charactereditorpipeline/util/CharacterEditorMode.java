package editor.bootstrap.charactereditorpipeline.util;

import editor.runtime.EditorSetting;

public enum CharacterEditorMode {

    /*
     * What the character editor's panels work on: Appearance the default
     * features, colors and palettes, Build the build curve and the preview's
     * body, and Equipment the anchors worn and held gear hangs from.
     */

    APPEARANCE(EditorSetting.CHARACTER_EDITOR_MODE_APPEARANCE),
    BUILD(EditorSetting.CHARACTER_EDITOR_MODE_BUILD),
    EQUIPMENT(EditorSetting.CHARACTER_EDITOR_MODE_EQUIPMENT);

    // Internal
    private final String label;

    // Constructor \\

    CharacterEditorMode(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
