package editor.bootstrap.worldeditorpipeline.util;

import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;

public enum WorldEditorLayer {

    /*
     * The World Editor's layers, bottom first, as the Layers tab stacks them:
     * Biomes, the world image whose colors place every biome, which also
     * places what the editor's mode places, and Heights, the grey height
     * layer over it that raises and lowers the ground, drawn over the biomes.
     * The layer picked is the one the editor's tools work on; each is its own
     * tool canvas, keeping its own tool and brush size, and names what the
     * Properties tab shows while it is picked: the biome palette over the
     * biome chosen, or the entry selected in the Hierarchy.
     */

    BIOMES(EditorSetting.WORLD_EDITOR_LAYER_BIOMES, ToolCanvas.BIOMES, PropertiesContent.BIOMES, true, false),
    HEIGHTS(EditorSetting.WORLD_EDITOR_LAYER_HEIGHTS, ToolCanvas.HEIGHTS, PropertiesContent.INFO, false, true);

    // Values
    public static final WorldEditorLayer[] VALUES = values();

    // Internal
    private final String label;
    private final ToolCanvas toolCanvas;
    private final PropertiesContent propertiesContent;
    private final boolean placing;
    private final boolean overlay;

    // Constructor \\

    WorldEditorLayer(
            String label,
            ToolCanvas toolCanvas,
            PropertiesContent propertiesContent,
            boolean placing,
            boolean overlay) {
        this.label = label;
        this.toolCanvas = toolCanvas;
        this.propertiesContent = propertiesContent;
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

    public PropertiesContent getPropertiesContent() {
        return propertiesContent;
    }

    // True for the layer that also places what the World Editor's mode places
    public boolean isPlacing() {
        return placing;
    }

    // True for the layer drawn over the biomes
    public boolean isOverlay() {
        return overlay;
    }
}
