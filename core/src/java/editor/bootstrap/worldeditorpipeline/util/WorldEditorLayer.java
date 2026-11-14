package editor.bootstrap.worldeditorpipeline.util;

import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;

public enum WorldEditorLayer {

    /*
     * Which of the world's images a window paints, each in its own tab:
     * Biomes, the world image whose colors place every biome, painted in the
     * World Editor, which also places what its mode places, or Heights, the
     * grey height layer beside it that raises and lowers the ground, worked
     * in the Height Editor, which draws it over the biomes. Each is its own
     * tool canvas, keeping its own tool and brush size.
     */

    BIOMES(EditorSetting.WORLD_EDITOR_LAYER_BIOMES, ToolCanvas.BIOMES, true, false),
    HEIGHTS(EditorSetting.WORLD_EDITOR_LAYER_HEIGHTS, ToolCanvas.HEIGHTS, false, true);

    // Internal
    private final String label;
    private final ToolCanvas toolCanvas;
    private final boolean placing;
    private final boolean overlay;

    // Constructor \\

    WorldEditorLayer(String label, ToolCanvas toolCanvas, boolean placing, boolean overlay) {
        this.label = label;
        this.toolCanvas = toolCanvas;
        this.placing = placing;
        this.overlay = overlay;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public ToolCanvas getToolCanvas() {
        return toolCanvas;
    }

    // True for the layer whose window also places what the World Editor's mode places
    public boolean isPlacing() {
        return placing;
    }

    // True for the layer its window draws over the biomes
    public boolean isOverlay() {
        return overlay;
    }
}
