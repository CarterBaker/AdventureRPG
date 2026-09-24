package application.bootstrap.menupipeline.util;

import engine.settings.Settings;

public enum ThemeColor {

    /*
     * Named interface color slots. A menu element that names a slot instead of
     * a literal color is shaded from the matching user Settings value each
     * frame, so a palette change applies to every open menu immediately.
     */

    BACKGROUND,
    PANEL,
    HEADER,
    CONTROL,
    CONTROL_HOVER,
    ACCENT,
    ACCENT_HOVER,
    OUTLINE,
    SHADOW,
    TEXT,
    TEXT_MUTED,
    TEXT_ON_ACCENT,
    DANGER;

    public static ThemeColor fromString(String s) {
        return switch (s.toLowerCase()) {
            case "background" -> BACKGROUND;
            case "panel" -> PANEL;
            case "header" -> HEADER;
            case "control" -> CONTROL;
            case "control_hover" -> CONTROL_HOVER;
            case "accent" -> ACCENT;
            case "accent_hover" -> ACCENT_HOVER;
            case "outline" -> OUTLINE;
            case "shadow" -> SHADOW;
            case "text" -> TEXT;
            case "text_muted" -> TEXT_MUTED;
            case "text_on_accent" -> TEXT_ON_ACCENT;
            case "danger" -> DANGER;
            default -> null;
        };
    }

    public float[] resolve(Settings settings) {
        return switch (this) {
            case BACKGROUND -> settings.uiColorBackground;
            case PANEL -> settings.uiColorPanel;
            case HEADER -> settings.uiColorHeader;
            case CONTROL -> settings.uiColorControl;
            case CONTROL_HOVER -> settings.uiColorControlHover;
            case ACCENT -> settings.uiColorAccent;
            case ACCENT_HOVER -> settings.uiColorAccentHover;
            case OUTLINE -> settings.uiColorOutline;
            case SHADOW -> settings.uiColorShadow;
            case TEXT -> settings.uiColorText;
            case TEXT_MUTED -> settings.uiColorTextMuted;
            case TEXT_ON_ACCENT -> settings.uiColorTextOnAccent;
            case DANGER -> settings.uiColorDanger;
        };
    }
}
