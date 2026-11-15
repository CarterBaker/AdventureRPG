package application.bootstrap.dialoguepipeline.relation;

import engine.root.DataPackage;

public class RelationData extends DataPackage {

    /*
     * Immutable relation definition loaded from ARPG: what one being can be to
     * another, such as its child, its mate or a fellow at work. The bond is the
     * regard the holder has for a being that is this to it before anything else is
     * weighed, and the inverse is what the holder is to that being in turn. The
     * seed is the stable number of its name the dialogue models weigh it by. Owned
     * by RelationHandle for the engine lifetime.
     */

    // Identity
    private final String relationName;
    private final short relationID;
    private final int seed;

    // Display
    private final String displayName;
    private final String description;

    // Bond
    private final float bond;
    private final short inverseID;

    // Constructor \\

    public RelationData(
            String relationName,
            short relationID,
            int seed,
            String displayName,
            String description,
            float bond,
            short inverseID) {

        // Identity
        this.relationName = relationName;
        this.relationID = relationID;
        this.seed = seed;

        // Display
        this.displayName = displayName;
        this.description = description;

        // Bond
        this.bond = bond;
        this.inverseID = inverseID;
    }

    // Accessible \\

    public String getRelationName() {
        return relationName;
    }

    public short getRelationID() {
        return relationID;
    }

    public int getSeed() {
        return seed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public float getBond() {
        return bond;
    }

    public short getInverseID() {
        return inverseID;
    }
}
