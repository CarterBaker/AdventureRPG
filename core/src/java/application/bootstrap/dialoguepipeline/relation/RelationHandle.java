package application.bootstrap.dialoguepipeline.relation;

import engine.root.HandlePackage;

public class RelationHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded relation definition. Registered and owned
     * by RelationManager. Delegates all accessors through RelationData.
     */

    // Internal
    private RelationData relationData;

    // Constructor \\

    public void constructor(RelationData relationData) {

        // Internal
        this.relationData = relationData;
    }

    // Accessible \\

    public RelationData getRelationData() {
        return relationData;
    }

    public String getRelationName() {
        return relationData.getRelationName();
    }

    public short getRelationID() {
        return relationData.getRelationID();
    }

    public int getSeed() {
        return relationData.getSeed();
    }

    public String getDisplayName() {
        return relationData.getDisplayName();
    }

    public String getDescription() {
        return relationData.getDescription();
    }

    public float getBond() {
        return relationData.getBond();
    }

    public short getInverseID() {
        return relationData.getInverseID();
    }
}
