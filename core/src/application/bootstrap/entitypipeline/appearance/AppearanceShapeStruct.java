package application.bootstrap.entitypipeline.appearance;

import engine.root.StructPackage;

public class AppearanceShapeStruct extends StructPackage {

    /*
     * One slider of a character's appearance: the morph target of the
     * character mesh it weighs, the group a creator lists it under, and the
     * range and default of its value — the weight the morph is applied at, so
     * a negative value pushes the shape the other way.
     */

    // Identity
    private final String morphName;
    private final String groupName;
    private final int morphIndex;

    // Range
    private final float min;
    private final float max;
    private final float defaultValue;

    // Constructor \\

    public AppearanceShapeStruct(
            String morphName,
            String groupName,
            int morphIndex,
            float min,
            float max,
            float defaultValue) {

        // Identity
        this.morphName = morphName;
        this.groupName = groupName;
        this.morphIndex = morphIndex;

        // Range
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    // Utility \\

    public float clamp(float value) {
        return Math.max(min, Math.min(max, value));
    }

    // Accessible \\

    public String getMorphName() {
        return morphName;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getMorphIndex() {
        return morphIndex;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getDefaultValue() {
        return defaultValue;
    }
}
