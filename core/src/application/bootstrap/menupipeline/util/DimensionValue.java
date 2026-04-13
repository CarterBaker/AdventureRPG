package application.bootstrap.menupipeline.util;

import application.core.engine.StructPackage;

public class DimensionValue extends StructPackage {

    /*
     * A single resolved dimension — either a percentage of the parent
     * dimension or an absolute pixel value. Created via parse() from
     * JSON string values like "50%", "100px", or "32".
     */

    // Internal
    private final float value;
    private final boolean percentage;

    // Constructor \\

    private DimensionValue(float value, boolean percentage) {
        this.value = value;
        this.percentage = percentage;
    }

    // Factory \\

    public static DimensionValue parse(String raw) {

        raw = raw.trim();

        if (raw.endsWith("%"))
            return new DimensionValue(
                    Float.parseFloat(raw.substring(0, raw.length() - 1)), true);

        if (raw.endsWith("px"))
            return new DimensionValue(
                    Float.parseFloat(raw.substring(0, raw.length() - 2)), false);

        if (raw.endsWith("p"))
            return new DimensionValue(
                    Float.parseFloat(raw.substring(0, raw.length() - 1)), false);

        return new DimensionValue(Float.parseFloat(raw), false);
    }

    // Accessible \\

    public float resolve(float parentDimension) {
        return percentage ? (value / 100f) * parentDimension : value;
    }
}