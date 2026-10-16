package application.bootstrap.progressionpipeline.profession;

import engine.root.StructPackage;

public class RequirementStruct extends StructPackage {

    /*
     * One condition a profession is unlocked by: what it reads, the trade,
     * skill, deed or profession it names when it names one, and the range the
     * value read must lie in, both ends included.
     */

    // Condition
    private final RequirementKind kind;
    private final short targetID;
    private final float min;
    private final float max;

    // Constructor \\

    public RequirementStruct(
            RequirementKind kind,
            short targetID,
            float min,
            float max) {

        // Condition
        this.kind = kind;
        this.targetID = targetID;
        this.min = min;
        this.max = max;
    }

    // Accessible \\

    public boolean accepts(float value) {
        return value >= min && value <= max;
    }

    public RequirementKind getKind() {
        return kind;
    }

    public short getTargetID() {
        return targetID;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }
}
