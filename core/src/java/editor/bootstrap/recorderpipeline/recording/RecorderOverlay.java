package editor.bootstrap.recorderpipeline.recording;

import application.runtime.RuntimeSetting;

public enum RecorderOverlay {

    /*
     * The targets a window draws over the world, each on its own layer: the
     * vitality effects closing in from the edges, every menu and the HUD,
     * and the inventory's own scene. The interface channel captures each
     * one's pixels as the window drew it and plays them back through a
     * target made from the same definition, on the same layer, so it
     * composites exactly as it did. Recording files name overlays by their
     * lower-case constant names.
     */

    VITALITY(RuntimeSetting.FBO_VITALITY, RuntimeSetting.LAYER_VITALITY),
    MENUS(RuntimeSetting.FBO_UI, RuntimeSetting.LAYER_UI),
    INVENTORY(RuntimeSetting.FBO_INVENTORY, RuntimeSetting.LAYER_INVENTORY);

    // Values
    public static final RecorderOverlay[] VALUES = values();

    // Internal
    private final String fboName;
    private final int layer;

    // Constructor \\

    RecorderOverlay(String fboName, int layer) {
        this.fboName = fboName;
        this.layer = layer;
    }

    // Accessible \\

    public String getFboName() {
        return fboName;
    }

    public int getLayer() {
        return layer;
    }
}
