package editor.runtime.menueventsmanager.menus.settings;

public enum EditorSettingsAction {

    /*
     * The settings dialog's action rows. Each action's name rides as its
     * row's click argument, so the one action handler knows which was
     * pressed and hands it to the page that owns it.
     */

    RESET_CONTROLS,
    ENTER_HEX,
    RESET_COLOR,
    RESET_COLORS
}
