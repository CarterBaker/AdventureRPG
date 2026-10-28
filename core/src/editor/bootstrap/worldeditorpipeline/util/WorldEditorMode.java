package editor.bootstrap.worldeditorpipeline.util;

import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;

public enum WorldEditorMode {

    /*
     * What the world editor's canvas is working on: Paint, the world's
     * images, painted with the biome and height palette, Settlements, the
     * settlements and structures placed by hand on the world, chosen in the
     * Settlement Planner, or Cameras, the views the main menu looks out
     * from, designed in the Camera Planner. A placing mode is its own tool
     * canvas and holds only its own kinds of placement, so its tools never
     * reach another's. The mode follows what is selected to work on, so it
     * is never chosen by hand.
     */

    PAINT(EditorSetting.WORLD_EDITOR_MODE_PAINT, null),
    SETTLEMENTS(
            EditorSetting.WORLD_EDITOR_MODE_SETTLEMENTS,
            ToolCanvas.SETTLEMENTS,
            WorldPlacementKind.SETTLEMENT,
            WorldPlacementKind.STRUCTURE),
    CAMERAS(EditorSetting.WORLD_EDITOR_MODE_CAMERAS, ToolCanvas.CAMERAS, WorldPlacementKind.CAMERA);

    // Internal
    private final String label;
    private final ToolCanvas toolCanvas;
    private final boolean[] kind2Held;

    // Constructor \\

    WorldEditorMode(String label, ToolCanvas toolCanvas, WorldPlacementKind... kinds) {

        this.label = label;
        this.toolCanvas = toolCanvas;
        this.kind2Held = new boolean[WorldPlacementKind.VALUES.length];

        for (int i = 0; i < kinds.length; i++)
            kind2Held[kinds[i].ordinal()] = true;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // The canvas a placing mode works on, null while painting, which works on the layer painted
    public ToolCanvas getToolCanvas() {
        return toolCanvas;
    }

    public boolean isPlacing() {
        return toolCanvas != null;
    }

    // True for a kind of placement this mode's tools select, move and erase
    public boolean holds(WorldPlacementKind kind) {
        return kind2Held[kind.ordinal()];
    }
}
