package application.bootstrap.menupipeline.util;

import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.settings.Settings;
import engine.util.mathematics.vectors.Vector4;

public class MenuColorStruct extends StructPackage {

    /*
     * Immutable color of one menu element. Holds either a literal RGBA color or
     * a ThemeColor slot with an alpha multiplier. Themed colors are resolved
     * against the live user Settings on every resolve, so a palette change is
     * picked up on the next frame without rebuilding any menu.
     */

    // Internal
    private final Color color;
    private final ThemeColor themeColor;
    private final float alpha;

    // Constructor \\

    public MenuColorStruct(Color color) {
        this.color = color;
        this.themeColor = null;
        this.alpha = EngineSetting.MENU_THEME_ALPHA_DEFAULT;
    }

    public MenuColorStruct(ThemeColor themeColor, float alpha) {
        this.color = null;
        this.themeColor = themeColor;
        this.alpha = alpha;
    }

    // Resolve \\

    public void resolve(Settings settings, Vector4 target) {

        if (themeColor == null) {
            target.set(color.r, color.g, color.b, color.a);
            return;
        }

        float[] rgba = themeColor.resolve(settings);
        target.set(rgba[0], rgba[1], rgba[2], rgba[3] * alpha);
    }

    // Accessible \\

    public Color getColor() {
        return color;
    }

    public ThemeColor getThemeColor() {
        return themeColor;
    }

    public float getAlpha() {
        return alpha;
    }

    public boolean isThemed() {
        return themeColor != null;
    }
}
