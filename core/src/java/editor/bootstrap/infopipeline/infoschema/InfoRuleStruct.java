package editor.bootstrap.infopipeline.infoschema;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoRuleStruct extends StructPackage {

    /*
     * The checks a field's value must pass beyond its type. A number keeps
     * within its bounds, each inclusive or exclusive, never falls below the
     * sibling number it must be at least, is one of the numbers it lists, and
     * indexes the top-level list of its entry it names; inside the elements of
     * a list it may also start from a value and climb from one element to the
     * next. An array or map holds no fewer values than its minimum count and
     * no more than its maximum, and an array of whole numbers may read as
     * records. A field becomes required, and a list non-empty, unless the
     * sibling it names is set — present, and true when a boolean — and becomes
     * required when a sibling holds one of the values it lists; a field may
     * also never be set together with the sibling it conflicts with. A string
     * naming an entry of its own schema may be acyclic: no entry reaches
     * itself through it, at any depth.
     */

    // Bounds
    private final double min;
    private final double max;
    private final boolean minExclusive;
    private final boolean maxExclusive;
    private final String atLeastField;
    private final DoubleArrayList oneOf;
    private final String indexInto;

    // Order — the value the first element starts from, each later one above the one before; NaN for none
    private final double ascendingFrom;

    // Count
    private final int minCount;
    private final int maxCount;
    private final InfoRecordStruct record;

    // Condition
    private final String requiredUnless;
    private final String requiredWhenKey;
    private final ObjectArrayList<String> requiredWhenValues;
    private final String conflictsWith;

    // Reference
    private final boolean acyclic;

    // Constructor \\

    public InfoRuleStruct(
            double min,
            double max,
            boolean minExclusive,
            boolean maxExclusive,
            String atLeastField,
            DoubleArrayList oneOf,
            String indexInto,
            double ascendingFrom,
            int minCount,
            int maxCount,
            InfoRecordStruct record,
            String requiredUnless,
            String requiredWhenKey,
            ObjectArrayList<String> requiredWhenValues,
            String conflictsWith,
            boolean acyclic) {

        // Bounds
        this.min = min;
        this.max = max;
        this.minExclusive = minExclusive;
        this.maxExclusive = maxExclusive;
        this.atLeastField = atLeastField;
        this.oneOf = oneOf;
        this.indexInto = indexInto;

        // Order
        this.ascendingFrom = ascendingFrom;

        // Count
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.record = record;

        // Condition
        this.requiredUnless = requiredUnless;
        this.requiredWhenKey = requiredWhenKey;
        this.requiredWhenValues = requiredWhenValues;
        this.conflictsWith = conflictsWith;

        // Reference
        this.acyclic = acyclic;
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

    public boolean hasOneOf() {
        return !oneOf.isEmpty();
    }

    // True when the number is one the field lists, or the field lists none
    public boolean isOneOf(double value) {
        return oneOf.isEmpty() || oneOf.contains(value);
    }

    public boolean hasIndexInto() {
        return indexInto != null;
    }

    public boolean hasAscendingFrom() {
        return !Double.isNaN(ascendingFrom);
    }

    public boolean hasRecord() {
        return record != null;
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

    public DoubleArrayList getOneOf() {
        return oneOf;
    }

    public String getIndexInto() {
        return indexInto;
    }

    public double getAscendingFrom() {
        return ascendingFrom;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public InfoRecordStruct getRecord() {
        return record;
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

    public boolean isAcyclic() {
        return acyclic;
    }
}
