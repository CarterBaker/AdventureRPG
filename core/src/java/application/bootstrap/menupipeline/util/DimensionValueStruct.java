package application.bootstrap.menupipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.StructPackage;

public class DimensionValueStruct extends StructPackage {

    /*
     * A single resolved dimension — either a percentage of the parent
     * dimension or an absolute pixel value, with an optional pixel offset.
     * Created via parse() from ARPG string values like "50%", "100px", "32",
     * or "calc(100% - 24px)". A height of "fit" is a placeholder MenuBuilder
     * replaces with the height of the rows the element holds as it builds it.
     */

    // Internal
    private final float value;
    private final boolean percentage;
    private final float offset;
    private final boolean fit;

    // Constructor \\

    private DimensionValueStruct(float value, boolean percentage, float offset) {
        this(value, percentage, offset, false);
    }

    private DimensionValueStruct(float value, boolean percentage, float offset, boolean fit) {
        this.value = value;
        this.percentage = percentage;
        this.offset = offset;
        this.fit = fit;
    }

    // Factory \\

    public static DimensionValueStruct parse(String raw) {

        raw = raw.trim();

        if (raw.equals(EngineSetting.ELEMENT_SIZE_FIT))
            return new DimensionValueStruct(0f, false, 0f, true);

        if (raw.startsWith("calc(") && raw.endsWith(")")) {

            String expr = raw.substring(5, raw.length() - 1).trim();

            int plusIdx = expr.lastIndexOf('+');
            int minusIdx = expr.lastIndexOf('-');
            int opIdx = Math.max(plusIdx, minusIdx);

            if (opIdx < 1)
                EngineUtility.throwException("Unsupported calc() expression: " + raw);

            float sign = opIdx == plusIdx ? 1f : -1f;
            float pct = Float.parseFloat(expr.substring(0, opIdx).trim().replace("%", ""));
            float px = Float.parseFloat(expr.substring(opIdx + 1).trim().replace("px", ""));

            return new DimensionValueStruct(pct, true, sign * px);
        }

        if (raw.endsWith("%"))
            return new DimensionValueStruct(
                    Float.parseFloat(raw.substring(0, raw.length() - 1)), true, 0f);

        if (raw.endsWith("px"))
            return new DimensionValueStruct(
                    Float.parseFloat(raw.substring(0, raw.length() - 2)), false, 0f);

        if (raw.endsWith("p"))
            return new DimensionValueStruct(
                    Float.parseFloat(raw.substring(0, raw.length() - 1)), false, 0f);

        return new DimensionValueStruct(Float.parseFloat(raw), false, 0f);
    }

    public static DimensionValueStruct ofAbsolute(float value) {
        return new DimensionValueStruct(value, false, 0f);
    }

    public static DimensionValueStruct ofPercent(float value) {
        return new DimensionValueStruct(value, true, 0f);
    }

    public static DimensionValueStruct ofPercentWithOffset(float value, float offset) {
        return new DimensionValueStruct(value, true, offset);
    }

    // Accessible \\

    public float resolve(float parentDimension) {
        return (percentage ? (value / EngineSetting.PERCENT_MAX) * parentDimension : value) + offset;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public float getRawValue() {
        return value;
    }

    public float getOffset() {
        return offset;
    }

    public boolean isFit() {
        return fit;
    }
}