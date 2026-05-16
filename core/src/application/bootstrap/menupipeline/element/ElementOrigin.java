package application.bootstrap.menupipeline.element;

import engine.root.EngineUtility;

public enum ElementOrigin {

    /*
     * Named anchor/pivot presets mapping semantic positions to normalized
     * [0,1] x/y coordinates.
     */

    BOTTOM_LEFT(0f, 0f),
    BOTTOM_CENTER(0.5f, 0f),
    BOTTOM_RIGHT(1f, 0f),
    BOTTOM(0.5f, 0f),
    CENTER_LEFT(0f, 0.5f),
    LEFT(0f, 0.5f),
    CENTER(0.5f, 0.5f),
    RIGHT(1f, 0.5f),
    CENTER_RIGHT(1f, 0.5f),
    TOP_LEFT(0f, 1f),
    TOP_CENTER(0.5f, 1f),
    TOP_RIGHT(1f, 1f),
    TOP(0.5f, 1f);

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
        String normalized = name.toLowerCase().replace('-', '_').replace(' ', '_');

        for (ElementOrigin o : values())
            if (o.name().equalsIgnoreCase(normalized))
                return o;

        boolean left = normalized.contains("left");
        boolean right = normalized.contains("right");
        boolean top = normalized.contains("top");
        boolean bottom = normalized.contains("bottom");

        if (left) {
            if (top)
                return TOP_LEFT;
            if (bottom)
                return BOTTOM_LEFT;
            return LEFT;
        }

        if (right) {
            if (top)
                return TOP_RIGHT;
            if (bottom)
                return BOTTOM_RIGHT;
            return RIGHT;
        }

        if (top)
            return TOP;

        if (bottom)
            return BOTTOM;

        if (normalized.contains("center") || normalized.contains("middle"))
            return CENTER;

        EngineUtility.throwException("Unknown origin: '" + name + "'");
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