package editor.bootstrap.infopipeline.infoschema;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoRuleStruct extends StructPackage {

    /*
     * The checks a field's value must pass beyond its type. A number keeps
     * within its bounds, each inclusive or exclusive, and never falls below
     * the sibling number it must be at least; an array or map holds no fewer
     * values than its minimum count and no more than its maximum. A field becomes required, and a list non-empty,
     * unless the sibling it names is set — present, and true when a boolean —
     * and becomes required when a sibling holds one of the values it lists; a
     * field may also never be set together with the sibling it conflicts with.
     */

    // Bounds
    private final double min;
    private final double max;
    private final boolean minExclusive;
    private final boolean maxExclusive;
    private final String atLeastField;

    // Count
    private final int minCount;
    private final int maxCount;

    // Condition
    private final String requiredUnless;
    private final String requiredWhenKey;
    private final ObjectArrayList<String> requiredWhenValues;
    private final String conflictsWith;

    // Constructor \\

    public InfoRuleStruct(
            double min,
            double max,
            boolean minExclusive,
            boolean maxExclusive,
            String atLeastField,
            int minCount,
            int maxCount,
            String requiredUnless,
            String requiredWhenKey,
            ObjectArrayList<String> requiredWhenValues,
            String conflictsWith) {

        // Bounds
        this.min = min;
        this.max = max;
        this.minExclusive = minExclusive;
        this.maxExclusive = maxExclusive;
        this.atLeastField = atLeastField;

        // Count
        this.minCount = minCount;
        this.maxCount = maxCount;

        // Condition
        this.requiredUnless = requiredUnless;
        this.requiredWhenKey = requiredWhenKey;
        this.requiredWhenValues = requiredWhenValues;
        this.conflictsWith = conflictsWith;
    }

    // Bounds \\

    public boolean isWithinBounds(double value) {

        boolean aboveMin = minExclusive ? value > min : value >= min;
        boolean belowMax = maxExclusive ? value < max : value <= max;

        return aboveMin && belowMax;
    }

    public boolean hasMin() {
        return min != Double.NEGATIVE_INFINITY;
    }

    public boolean hasMax() {
        return max != Double.POSITIVE_INFINITY;
    }

    public boolean hasAtLeastField() {
        return atLeastField != null;
    }

    public boolean hasRequiredUnless() {
        return requiredUnless != null;
    }

    public boolean hasRequiredWhen() {
        return requiredWhenKey != null;
    }

    // True when the sibling value given is one the field is required for, matched ignoring case as enums are
    public boolean isRequiredFor(String siblingValue) {

        for (int i = 0; i < requiredWhenValues.size(); i++)
            if (requiredWhenValues.get(i).equalsIgnoreCase(siblingValue))
                return true;

        return false;
    }

    public boolean hasConflict() {
        return conflictsWith != null;
    }

    // Accessible \\

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public boolean isMinExclusive() {
        return minExclusive;
    }

    public boolean isMaxExclusive() {
        return maxExclusive;
    }

    public String getAtLeastField() {
        return atLeastField;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public String getRequiredUnless() {
        return requiredUnless;
    }

    public String getRequiredWhenKey() {
        return requiredWhenKey;
    }

    public String getConflictsWith() {
        return conflictsWith;
    }
}
