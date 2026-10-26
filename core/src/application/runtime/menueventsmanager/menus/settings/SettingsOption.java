package application.runtime.menueventsmanager.menus.settings;

import application.runtime.RuntimeSetting;

public enum SettingsOption {

    /*
     * The settings the menu shows as stepper rows. A toggle flips on either
     * arrow; a stepped value moves one step per arrow within its range. Each
     * option's name rides as its arrows' click argument, so the one step
     * handler knows which setting the row controls. A world setting reshapes
     * how the world streams, so its menu applies it once when it closes.
     */

    FULLSCREEN(RuntimeSetting.SETTINGS_OPTION_FULLSCREEN, false),
    VSYNC(RuntimeSetting.SETTINGS_OPTION_VSYNC, false),
    FIELD_OF_VIEW(RuntimeSetting.SETTINGS_OPTION_FIELD_OF_VIEW, false),
    RENDER_DISTANCE(RuntimeSetting.SETTINGS_OPTION_RENDER_DISTANCE, true),
    TERRAIN_DETAIL(RuntimeSetting.SETTINGS_OPTION_TERRAIN_DETAIL, true),
    TREE_DETAIL(RuntimeSetting.SETTINGS_OPTION_TREE_DETAIL, false),
    MACRO_TERRAIN(RuntimeSetting.SETTINGS_OPTION_MACRO_TERRAIN, true),
    MENU_CHUNK_STREAMING(RuntimeSetting.SETTINGS_OPTION_CHUNK_STREAMING, true),
    MENU_RENDER_DISTANCE(RuntimeSetting.SETTINGS_OPTION_RENDER_DISTANCE, true),
    MENU_TREE_DETAIL(RuntimeSetting.SETTINGS_OPTION_TREE_DETAIL, false),
    MENU_MACRO_TERRAIN(RuntimeSetting.SETTINGS_OPTION_MACRO_TERRAIN, true),
    OUTLINES(RuntimeSetting.SETTINGS_OPTION_OUTLINES, false),
    OUTLINE_THICKNESS(RuntimeSetting.SETTINGS_OPTION_OUTLINE_THICKNESS, false),
    DEPTH_OF_FIELD(RuntimeSetting.SETTINGS_OPTION_DEPTH_OF_FIELD, false),
    DEPTH_OF_FIELD_THIRD_PERSON(RuntimeSetting.SETTINGS_OPTION_DEPTH_OF_FIELD_THIRD_PERSON, false),
    ANTI_ALIASING(RuntimeSetting.SETTINGS_OPTION_ANTI_ALIASING, false),
    BLOOM(RuntimeSetting.SETTINGS_OPTION_BLOOM, false),
    CAMERA_SHAKE(RuntimeSetting.SETTINGS_OPTION_CAMERA_SHAKE, false),
    HEAD_BOB(RuntimeSetting.SETTINGS_OPTION_HEAD_BOB, false);

    // Internal
    private final String label;
    private final boolean worldSetting;

    // Constructor \\

    SettingsOption(String label, boolean worldSetting) {
        this.label = label;
        this.worldSetting = worldSetting;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isWorldSetting() {
        return worldSetting;
    }
}
