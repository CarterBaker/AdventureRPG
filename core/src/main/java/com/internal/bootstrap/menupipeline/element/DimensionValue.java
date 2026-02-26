package com.internal.bootstrap.menupipeline.element;

public class DimensionValue {

    private final float value;
    private final boolean percentage;

    private DimensionValue(float value, boolean percentage) {
        this.value = value;
        this.percentage = percentage;
    }

    public static DimensionValue parse(String raw) {
        raw = raw.trim();
        if (raw.endsWith("%"))
            return new DimensionValue(Float.parseFloat(raw.substring(0, raw.length() - 1)), true);
        if (raw.endsWith("px"))
            return new DimensionValue(Float.parseFloat(raw.substring(0, raw.length() - 2)), false);
        if (raw.endsWith("p"))
            return new DimensionValue(Float.parseFloat(raw.substring(0, raw.length() - 1)), false);
        return new DimensionValue(Float.parseFloat(raw), false);
    }

    public float resolve(float parentDimension) {
        return percentage ? (value / 100f) * parentDimension : value;
    }
}