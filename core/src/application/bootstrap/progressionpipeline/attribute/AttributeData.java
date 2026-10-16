package application.bootstrap.progressionpipeline.attribute;

import engine.root.DataPackage;

public class AttributeData extends DataPackage {

    /*
     * Immutable attribute definition loaded from ARPG: one of the letters of
     * ADVENTURE every being spends points into. What an attribute does lives
     * on the stats it feeds, not here. Owned by AttributeHandle for the engine
     * lifetime.
     */

    // Identity
    private final String attributeName;
    private final short attributeID;

    // Presentation
    private final String displayName;
    private final String abbreviation;
    private final String description;

    // Constructor \\

    public AttributeData(
            String attributeName,
            short attributeID,
            String displayName,
            String abbreviation,
            String description) {

        // Identity
        this.attributeName = attributeName;
        this.attributeID = attributeID;

        // Presentation
        this.displayName = displayName;
        this.abbreviation = abbreviation;
        this.description = description;
    }

    // Accessible \\

    public String getAttributeName() {
        return attributeName;
    }

    public short getAttributeID() {
        return attributeID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getDescription() {
        return description;
    }
}
