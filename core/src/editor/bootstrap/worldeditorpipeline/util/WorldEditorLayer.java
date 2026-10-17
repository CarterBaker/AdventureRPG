package editor.bootstrap.worldeditorpipeline.util;

import editor.runtime.EditorSetting;

public enum WorldEditorLayer {

    /*
     * Which of the world's images the world editor paints: Biomes, the world
     * image whose colors place every biome, or Heights, the grey height layer
     * beside it that raises and lowers the ground, shown over the biomes.
     */

    BIOMES(EditorSetting.WORLD_EDITOR_LAYER_BIOMES),
    HEIGHTS(EditorSetting.WORLD_EDITOR_LAYER_HEIGHTS);

    // Internal
    private final String label;

    // Constructor \\

    WorldEditorLayer(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // The two layers take turns
    public WorldEditorLayer toggle() {
        return this == BIOMES ? HEIGHTS : BIOMES;
    }
}
