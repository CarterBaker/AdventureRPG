package editor.bootstrap.treeplannerpipeline.treeplanner;

public enum TreePlannerNode {

    /*
     * The generator a tree species grows by, as the Tree Planner draws it:
     * one node for each stage a tree passes through from its seed, each
     * owning the fields of the species file that shape that stage, in the
     * order the generator runs them.
     */

    TREE("Tree"), // The form, which decides where limbs set out from the trunk and where the leaves gather
    TRUNK("Trunk"), // Its height, girth and taper, the flare at its foot and how it leans and wanders
    LIMBS("Limbs"), // The limbs the trunk throws out: how many, where up the trunk, how long and at what angle
    TWIGS("Twigs"), // The levels of branching below each limb and how each level shortens and spreads
    LEAVES("Leaves"), // The leaf clusters at the tips: their size, squash, density and accents
    WOOD("Wood"), // The bark's depth and the tool tier felling it takes
    GROWTH("Growth"); // How long a tree takes to grow, how old wild trees stand and how often it seeds

    public final String displayName;

    TreePlannerNode(String displayName) {
        this.displayName = displayName;
    }
}
