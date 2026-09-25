package application.runtime.menueventsmanager.menus.settings;

import application.runtime.RuntimeSetting;

public enum SettingsTab {

    /*
     * The sections of the settings menu, in the order their tabs are listed.
     * Each tab is one page of the scrolling option list; a tab with
     * placeholder text is a section still to come and shows that text.
     */

    DISPLAY(RuntimeSetting.SETTINGS_TAB_DISPLAY, null),
    GRAPHICS(RuntimeSetting.SETTINGS_TAB_GRAPHICS, null),
    CONTROLS(RuntimeSetting.SETTINGS_TAB_CONTROLS, null),
    AUDIO(RuntimeSetting.SETTINGS_TAB_AUDIO, RuntimeSetting.SETTINGS_PLACEHOLDER_AUDIO),
    GAMEPLAY(RuntimeSetting.SETTINGS_TAB_GAMEPLAY, RuntimeSetting.SETTINGS_PLACEHOLDER_GAMEPLAY),
    ACCESSIBILITY(RuntimeSetting.SETTINGS_TAB_ACCESSIBILITY, RuntimeSetting.SETTINGS_PLACEHOLDER_ACCESSIBILITY);

    // Internal
    private final String title;
    private final String placeholderText;

    // Constructor \\

    SettingsTab(String title, String placeholderText) {
        this.title = title;
        this.placeholderText = placeholderText;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    public String getPlaceholderText() {
        return placeholderText;
    }

    public boolean isPlaceholder() {
        return placeholderText != null;
    }
}
