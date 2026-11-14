package application.bootstrap.progressionpipeline.attribute;

import engine.root.HandlePackage;

public class AttributeHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded attribute definition. Registered and
     * owned by AttributeManager. Delegates all accessors through
     * AttributeData.
     */

    // Internal
    private AttributeData attributeData;

    // Constructor \\

    public void constructor(AttributeData attributeData) {

        // Internal
        this.attributeData = attributeData;
    }

    // Accessible \\

    public AttributeData getAttributeData() {
        return attributeData;
    }

    public String getAttributeName() {
        return attributeData.getAttributeName();
    }

    public short getAttributeID() {
        return attributeData.getAttributeID();
    }

    public String getDisplayName() {
        return attributeData.getDisplayName();
    }

    public String getAbbreviation() {
        return attributeData.getAbbreviation();
    }

    public String getDescription() {
        return attributeData.getDescription();
    }
}
