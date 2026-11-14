package application.bootstrap.entitypipeline.appearance;

import engine.root.StructPackage;

public class AppearanceShapeStruct extends StructPackage {

    /*
     * One slider of a character's appearance: the name saves and presets
     * know it by, the group a creator lists it under, what it drives — a
     * morph target of the body meshes, the bones it moves, turns and scales,
     * or both, the way a face control drives a face rig — and the range and
     * default of its value, the weight the morph is applied at and the share
     * of every bone target taken, so a negative value pushes the shape the
     * other way. The morph is named here and resolved per body mesh by
     * AppearanceData.
     */

    // Identity
    private final String shapeName;
    private final String groupName;
    private final String morphName;

    // Targets
    private final AppearanceBoneTargetStruct[] boneTargets;

    // Range
    private final float min;
    private final float max;
    private final float defaultValue;

    // Constructor \\

    public AppearanceShapeStruct(
            String shapeName,
            String groupName,
            String morphName,
            AppearanceBoneTargetStruct[] boneTargets,
            float min,
            float max,
            float defaultValue) {

        // Identity
        this.shapeName = shapeName;
        this.groupName = groupName;
        this.morphName = morphName;

        // Targets
        this.boneTargets = boneTargets;

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

    public String getShapeName() {
        return shapeName;
    }

    public String getGroupName() {
        return groupName;
    }

    // Null for a slider that drives only bones
    public String getMorphName() {
        return morphName;
    }

    public boolean hasMorph() {
        return morphName != null;
    }

    public int getBoneTargetCount() {
        return boneTargets.length;
    }

    public AppearanceBoneTargetStruct getBoneTarget(int targetIndex) {
        return boneTargets[targetIndex];
    }

    public boolean hasBoneTargets() {
        return boneTargets.length > 0;
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
