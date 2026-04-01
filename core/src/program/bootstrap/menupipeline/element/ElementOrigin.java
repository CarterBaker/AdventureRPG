package program.bootstrap.menupipeline.element;

import program.core.engine.UtilityPackage;

public enum ElementOrigin {

    /*
     * Named anchor/pivot presets mapping semantic positions to normalized
     * [0,1] x/y coordinates.
     */

    BOTTOM_LEFT(0f, 0f),
    BOTTOM_CENTER(0.5f, 0f),
    BOTTOM_RIGHT(1f, 0f),
    CENTER_LEFT(0f, 0.5f),
    CENTER(0.5f, 0.5f),
    CENTER_RIGHT(1f, 0.5f),
    TOP_LEFT(0f, 1f),
    TOP_CENTER(0.5f, 1f),
    TOP_RIGHT(1f, 1f);

    // Internal
    private final float x;
    private final float y;

    // Constructor \\

    ElementOrigin(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // Factory \\

    public static ElementOrigin fromString(String name) {
        for (ElementOrigin o : values())
            if (o.name().equalsIgnoreCase(name))
                return o;
        UtilityPackage.throwException("Unknown origin: '" + name + "'");
        return null;
    }

    // Accessible \\

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}