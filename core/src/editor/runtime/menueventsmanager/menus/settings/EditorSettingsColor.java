package editor.runtime.menueventsmanager.menus.settings;

import application.bootstrap.menupipeline.util.ThemeColor;
import editor.runtime.EditorSetting;
import engine.settings.Settings;

public enum EditorSettingsColor {

    /*
     * The editor's color scheme as the Colors page lists it: every theme
     * slot the editor's menus shade from, in the order listed and grouped
     * under its section, the frame every editor window draws in place of the
     * OS one included. A slot's color lives in Settings, so an edit shows on
     * every open menu from the next frame.
     */

    BACKGROUND(EditorSetting.SETTINGS_SECTION_SURFACES, EditorSetting.SETTINGS_COLOR_BACKGROUND,
            ThemeColor.BACKGROUND),
    PANEL(EditorSetting.SETTINGS_SECTION_SURFACES, EditorSetting.SETTINGS_COLOR_PANEL,
            ThemeColor.PANEL),
    HEADER(EditorSetting.SETTINGS_SECTION_SURFACES, EditorSetting.SETTINGS_COLOR_HEADER,
            ThemeColor.HEADER),
    OUTLINE(EditorSetting.SETTINGS_SECTION_SURFACES, EditorSetting.SETTINGS_COLOR_OUTLINE,
            ThemeColor.OUTLINE),
    SHADOW(EditorSetting.SETTINGS_SECTION_SURFACES, EditorSetting.SETTINGS_COLOR_SHADOW,
            ThemeColor.SHADOW),
    CONTROL(EditorSetting.SETTINGS_SECTION_CONTROLS, EditorSetting.SETTINGS_COLOR_CONTROL,
            ThemeColor.CONTROL),
    CONTROL_HOVER(EditorSetting.SETTINGS_SECTION_CONTROLS, EditorSetting.SETTINGS_COLOR_CONTROL_HOVER,
            ThemeColor.CONTROL_HOVER),
    ACCENT(EditorSetting.SETTINGS_SECTION_CONTROLS, EditorSetting.SETTINGS_COLOR_ACCENT,
            ThemeColor.ACCENT),
    ACCENT_HOVER(EditorSetting.SETTINGS_SECTION_CONTROLS, EditorSetting.SETTINGS_COLOR_ACCENT_HOVER,
            ThemeColor.ACCENT_HOVER),
    DANGER(EditorSetting.SETTINGS_SECTION_CONTROLS, EditorSetting.SETTINGS_COLOR_DANGER,
            ThemeColor.DANGER),
    TEXT(EditorSetting.SETTINGS_SECTION_TEXT, EditorSetting.SETTINGS_COLOR_TEXT,
            ThemeColor.TEXT),
    TEXT_MUTED(EditorSetting.SETTINGS_SECTION_TEXT, EditorSetting.SETTINGS_COLOR_TEXT_MUTED,
            ThemeColor.TEXT_MUTED),
    TEXT_ON_ACCENT(EditorSetting.SETTINGS_SECTION_TEXT, EditorSetting.SETTINGS_COLOR_TEXT_ON_ACCENT,
            ThemeColor.TEXT_ON_ACCENT),
    FRAME(EditorSetting.SETTINGS_SECTION_FRAME, EditorSetting.SETTINGS_COLOR_FRAME,
            ThemeColor.FRAME),
    FRAME_OUTLINE(EditorSetting.SETTINGS_SECTION_FRAME, EditorSetting.SETTINGS_COLOR_FRAME_OUTLINE,
            ThemeColor.FRAME_OUTLINE);

    // Values
    public static final EditorSettingsColor[] VALUES = values();

    // Internal
    private final String section;
    private final String label;
    private final ThemeColor themeColor;

    // Constructor \\

    EditorSettingsColor(String section, String label, ThemeColor themeColor) {
        this.section = section;
        this.label = label;
        this.themeColor = themeColor;
    }

    // Accessible \\

    public String getSection() {
        return section;
    }

    public String getLabel() {
        return label;
    }

    public ThemeColor getThemeColor() {
        return themeColor;
    }

    // The slot's live RGBA in the given Settings, edited in place
    public float[] resolve(Settings settings) {
        return themeColor.resolve(settings);
    }
}
