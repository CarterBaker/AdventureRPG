package editor.runtime.menueventsmanager.menus.settings;

import editor.runtime.EditorSetting;

public enum EditorSettingsTab {

    /*
     * The pages of the editor's settings dialog, in the order their tabs are
     * listed, each under the group it belongs to. The editor's own pages set
     * its window, its shortcuts and its colors; the Dev Mode pages set what
     * every Dev Mode and Preview tab runs with, exactly as the game's own
     * settings menu would.
     */

    GENERAL(EditorSetting.SETTINGS_GROUP_EDITOR, EditorSetting.SETTINGS_TAB_GENERAL),
    SHORTCUTS(EditorSetting.SETTINGS_GROUP_EDITOR, EditorSetting.SETTINGS_TAB_SHORTCUTS),
    COLORS(EditorSetting.SETTINGS_GROUP_EDITOR, EditorSetting.SETTINGS_TAB_COLORS),
    GRAPHICS(EditorSetting.SETTINGS_GROUP_DEV_MODE, EditorSetting.SETTINGS_TAB_GRAPHICS),
    EFFECTS(EditorSetting.SETTINGS_GROUP_DEV_MODE, EditorSetting.SETTINGS_TAB_EFFECTS),
    GAMEPLAY(EditorSetting.SETTINGS_GROUP_DEV_MODE, EditorSetting.SETTINGS_TAB_GAMEPLAY),
    CONTROLS(EditorSetting.SETTINGS_GROUP_DEV_MODE, EditorSetting.SETTINGS_TAB_CONTROLS);

    // Values
    public static final EditorSettingsTab[] VALUES = values();

    // Internal
    private final String group;
    private final String title;

    // Constructor \\

    EditorSettingsTab(String group, String title) {
        this.group = group;
        this.title = title;
    }

    // Accessible \\

    public String getGroup() {
        return group;
    }

    public String getTitle() {
        return title;
    }
}
