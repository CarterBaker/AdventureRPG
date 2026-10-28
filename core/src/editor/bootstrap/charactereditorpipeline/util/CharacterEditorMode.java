package editor.bootstrap.charactereditorpipeline.util;

import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;

public enum CharacterEditorMode {

    /*
     * What the character editor's panels work on: Appearance the default
     * features, colors and palettes, Build the build curve and the preview's
     * body, Equipment the anchors worn and held gear hangs from, Mesh the
     * vertices and faces of a worn part, and Weights the bones each of that
     * part's vertices follows. The two mesh modes edit the part's mesh file
     * and take viewport input, each its own tool canvas.
     */

    APPEARANCE(EditorSetting.CHARACTER_EDITOR_MODE_APPEARANCE, null),
    BUILD(EditorSetting.CHARACTER_EDITOR_MODE_BUILD, null),
    EQUIPMENT(EditorSetting.CHARACTER_EDITOR_MODE_EQUIPMENT, null),
    MESH(EditorSetting.CHARACTER_EDITOR_MODE_MESH, ToolCanvas.CHARACTER_MESH),
    WEIGHTS(EditorSetting.CHARACTER_EDITOR_MODE_WEIGHTS, ToolCanvas.CHARACTER_WEIGHTS);

    // Internal
    private final String label;
    private final ToolCanvas toolCanvas;

    // Constructor \\

    CharacterEditorMode(String label, ToolCanvas toolCanvas) {
        this.label = label;
        this.toolCanvas = toolCanvas;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isMeshMode() {
        return toolCanvas != null;
    }

    // The tool canvas the viewport is while this mode is open, null for a mode that takes no tools
    public ToolCanvas getToolCanvas() {
        return toolCanvas;
    }
}
