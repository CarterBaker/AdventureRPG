package editor.bootstrap.worldeditorpipeline.util;

import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;

public enum WorldEditorLayer {

    /*
     * Which of the world's images the world editor paints: Biomes, the world
     * image whose colors place every biome, or Heights, the grey height layer
     * beside it that raises and lowers the ground, shown over the biomes.
     * Each is its own tool canvas, keeping its own tool and brush size.
     */

    BIOMES(EditorSetting.WORLD_EDITOR_LAYER_BIOMES, ToolCanvas.BIOMES),
    HEIGHTS(EditorSetting.WORLD_EDITOR_LAYER_HEIGHTS, ToolCanvas.HEIGHTS);

    // Internal
    private final String label;
    private final ToolCanvas toolCanvas;

    // Constructor \\

    WorldEditorLayer(String label, ToolCanvas toolCanvas) {
        this.label = label;
        this.toolCanvas = toolCanvas;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public ToolCanvas getToolCanvas() {
        return toolCanvas;
    }

    // The two layers take turns
    public WorldEditorLayer toggle() {
        return this == BIOMES ? HEIGHTS : BIOMES;
    }
}
