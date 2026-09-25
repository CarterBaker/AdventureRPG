package application.runtime.menueventsmanager.menus.settings;

import application.runtime.RuntimeSetting;

public enum SettingsSlider {

    /*
     * The settings the menu shows as sliders. Each slider's name rides as its
     * drag argument, so the one drag handler knows which setting the track it
     * received controls.
     */

    MOUSE_SENSITIVITY(RuntimeSetting.SETTINGS_SLIDER_MOUSE_SENSITIVITY);

    // Internal
    private final String label;

    // Constructor \\

    SettingsSlider(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
