package editor.bootstrap.charactereditorpipeline.util;

import editor.runtime.EditorSetting;

public enum CharacterEditorMode {

    /*
     * What the character editor's panels work on: Appearance the default
     * features, colors and palettes, Build the build curve and the preview's
     * body, Equipment the anchors worn and held gear hangs from, Mesh the
     * vertices and faces of a worn part, and Weights the bones each of that
     * part's vertices follows. The two mesh modes edit the part's mesh file
     * and take viewport input.
     */

    APPEARANCE(EditorSetting.CHARACTER_EDITOR_MODE_APPEARANCE, false),
    BUILD(EditorSetting.CHARACTER_EDITOR_MODE_BUILD, false),
    EQUIPMENT(EditorSetting.CHARACTER_EDITOR_MODE_EQUIPMENT, false),
    MESH(EditorSetting.CHARACTER_EDITOR_MODE_MESH, true),
    WEIGHTS(EditorSetting.CHARACTER_EDITOR_MODE_WEIGHTS, true);

    // Internal
    private final String label;
    private final boolean meshMode;

    // Constructor \\

    CharacterEditorMode(String label, boolean meshMode) {
        this.label = label;
        this.meshMode = meshMode;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isMeshMode() {
        return meshMode;
    }
}
