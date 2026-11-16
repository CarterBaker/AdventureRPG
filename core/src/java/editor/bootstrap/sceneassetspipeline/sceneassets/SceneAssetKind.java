package editor.bootstrap.sceneassetspipeline.sceneassets;

import editor.runtime.EditorSetting;

public enum SceneAssetKind {

    /*
     * The sections the Scene Assets tab sorts what the Dev window shows into,
     * in the order it lists them: what is pointed at under the crosshair,
     * then everything in view — the blocks, the biomes they lie in, the
     * textures their faces are drawn with, and the materials and shaders that
     * draw them.
     */

    POINTED(EditorSetting.SCENE_ASSETS_LABEL_POINTED),
    BLOCKS(EditorSetting.SCENE_ASSETS_LABEL_BLOCKS),
    BIOMES(EditorSetting.SCENE_ASSETS_LABEL_BIOMES),
    TEXTURES(EditorSetting.SCENE_ASSETS_LABEL_TEXTURES),
    MATERIALS(EditorSetting.SCENE_ASSETS_LABEL_MATERIALS),
    SHADERS(EditorSetting.SCENE_ASSETS_LABEL_SHADERS);

    // Values
    public static final SceneAssetKind[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    SceneAssetKind(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
