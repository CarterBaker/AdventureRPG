package editor.runtime.worldmap.layer;

import editor.runtime.worldmap.WorldMapSetting;

public enum WorldMapLayer {

    /*
     * The World Map's layers, bottom first, as the Layers tab stacks them:
     * Day And Night, the shared darkness laid over the night side, Weather,
     * the shared cloud and rain over the land, and Player, the arrow marking
     * the character. Each is shown or hidden by its eye, the overlays hidden
     * and the character shown when the map opens.
     */

    DAY_NIGHT(WorldMapSetting.LAYER_DAY_NIGHT, false),
    WEATHER(WorldMapSetting.LAYER_WEATHER, false),
    PLAYER(WorldMapSetting.LAYER_PLAYER, true);

    // Values
    public static final WorldMapLayer[] VALUES = values();

    // Internal
    private final String label;
    private final boolean shownByDefault;

    // Constructor \\

    WorldMapLayer(String label, boolean shownByDefault) {
        this.label = label;
        this.shownByDefault = shownByDefault;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isShownByDefault() {
        return shownByDefault;
    }
}
