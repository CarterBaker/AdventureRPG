package application.bootstrap.progressionpipeline.profession;

import engine.root.StructPackage;

public class RequirementGroupStruct extends StructPackage {

    /*
     * One way to unlock a profession: every requirement in the group must hold
     * at once. A profession lists one or more groups, and meeting any one of
     * them unlocks it.
     */

    // Requirements
    private final RequirementStruct[] requirements;

    // Constructor \\

    public RequirementGroupStruct(RequirementStruct[] requirements) {

        // Requirements
        this.requirements = requirements;
    }

    // Accessible \\

    public int size() {
        return requirements.length;
    }

    public RequirementStruct get(int index) {
        return requirements[index];
    }
}
