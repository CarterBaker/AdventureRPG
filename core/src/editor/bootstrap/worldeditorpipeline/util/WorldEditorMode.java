package editor.bootstrap.worldeditorpipeline.util;

import editor.runtime.EditorSetting;

public enum WorldEditorMode {

    /*
     * What the world editor's canvas is working on: Paint, the world's
     * images, painted with the biome and height palette, or Settlements, the
     * settlements and structures placed by hand on the world, chosen in the
     * Settlement Planner. The mode follows what is selected to work on, so
     * it is never chosen by hand.
     */

    PAINT(EditorSetting.WORLD_EDITOR_MODE_PAINT),
    SETTLEMENTS(EditorSetting.WORLD_EDITOR_MODE_SETTLEMENTS);

    // Internal
    private final String label;

    // Constructor \\

    WorldEditorMode(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
