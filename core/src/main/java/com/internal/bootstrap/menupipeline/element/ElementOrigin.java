package com.internal.bootstrap.menupipeline.element;

public enum ElementOrigin {
    BOTTOM_LEFT(0f, 0f),
    BOTTOM_CENTER(0.5f, 0f),
    BOTTOM_RIGHT(1f, 0f),
    CENTER_LEFT(0f, 0.5f),
    CENTER(0.5f, 0.5f),
    CENTER_RIGHT(1f, 0.5f),
    TOP_LEFT(0f, 1f),
    TOP_CENTER(0.5f, 1f),
    TOP_RIGHT(1f, 1f);

    public final float x;
    public final float y;

    ElementOrigin(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static ElementOrigin fromString(String name) {
        for (ElementOrigin o : values())
            if (o.name().equalsIgnoreCase(name))
                return o;
        throw new IllegalArgumentException("Unknown origin: '" + name + "'");
    }
}