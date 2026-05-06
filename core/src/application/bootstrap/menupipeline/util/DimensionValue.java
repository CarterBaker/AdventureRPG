package application.bootstrap.menupipeline.util;

import engine.root.StructPackage;

public class DimensionValue extends StructPackage {

    /*
     * A single resolved dimension — either a percentage of the parent
     * dimension or an absolute pixel value, with an optional pixel offset.
     * Created via parse() from JSON string values like "50%", "100px", "32",
     * or "calc(100% - 24px)".
     */

    // Internal
    private final float value;
    private final boolean percentage;
    private final float offset;

    // Constructor \\

    private DimensionValue(float value, boolean percentage, float offset) {
        this.value = value;
        this.percentage = percentage;
        this.offset = offset;
    }

    // Factory \\

    public static DimensionValue parse(String raw) {

        raw = raw.trim();

        if (raw.startsWith("calc(") && raw.endsWith(")")) {

            String expr = raw.substring(5, raw.length() - 1).trim();

            int plusIdx = expr.lastIndexOf('+');
            int minusIdx = expr.lastIndexOf('-');
            int opIdx = Math.max(plusIdx, minusIdx);

            if (opIdx < 1)
                throw new IllegalArgumentException("Unsupported calc() expression: " + raw);

            float sign = opIdx == plusIdx ? 1f : -1f;
            float pct = Float.parseFloat(expr.substring(0, opIdx).trim().replace("%", ""));
            float px = Float.parseFloat(expr.substring(opIdx + 1).trim().replace("px", ""));

            return new DimensionValue(pct, true, sign * px);
        }

        if (raw.endsWith("%"))
            return new DimensionValue(
                    Float.parseFloat(raw.substring(0, raw.length() - 1)), true, 0f);

        if (raw.endsWith("px"))
            return new DimensionValue(
                    Float.parseFloat(raw.substring(0, raw.length() - 2)), false, 0f);

        if (raw.endsWith("p"))
            return new DimensionValue(
                    Float.parseFloat(raw.substring(0, raw.length() - 1)), false, 0f);

        return new DimensionValue(Float.parseFloat(raw), false, 0f);
    }

    public static DimensionValue ofAbsolute(float value) {
        return new DimensionValue(value, false, 0f);
    }

    // Accessible \\

    public float resolve(float parentDimension) {
        return (percentage ? (value / 100f) * parentDimension : value) + offset;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public float getRawValue() {
        return value;
    }
}