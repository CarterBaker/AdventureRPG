package application.runtime.menueventsmanager.menus.settings;

import application.runtime.RuntimeSetting;

public enum SettingsOption {

    /*
     * The settings the menu shows as stepper rows. A toggle flips on either
     * arrow; a stepped value moves one step per arrow within its range. Each
     * option's name rides as its arrows' click argument, so the one step
     * handler knows which setting the row controls.
     */

    FULLSCREEN(RuntimeSetting.SETTINGS_OPTION_FULLSCREEN),
    VSYNC(RuntimeSetting.SETTINGS_OPTION_VSYNC),
    FIELD_OF_VIEW(RuntimeSetting.SETTINGS_OPTION_FIELD_OF_VIEW),
    RENDER_DISTANCE(RuntimeSetting.SETTINGS_OPTION_RENDER_DISTANCE),
    TERRAIN_DETAIL(RuntimeSetting.SETTINGS_OPTION_TERRAIN_DETAIL);

    // Internal
    private final String label;

    // Constructor \\

    SettingsOption(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
