package application.bootstrap.furnishingpipeline.furnishing;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.HandlePackage;

public class FurnishingHandle extends HandlePackage {

    /*
     * Persistent furnishing table. Wraps FurnishingData and delegates all
     * access through it.
     */

    // Internal
    private FurnishingData furnishingData;

    // Constructor \\

    public void constructor(FurnishingData furnishingData) {
        this.furnishingData = furnishingData;
    }

    // Accessible \\

    public FurnishingData getFurnishingData() {
        return furnishingData;
    }

    public String getFurnishingName() {
        return furnishingData.getFurnishingName();
    }

    public short getFurnishingID() {
        return furnishingData.getFurnishingID();
    }

    public ItemDefinitionHandle[] getItems() {
        return furnishingData.getItems();
    }

    public float[] getCumulativeWeights() {
        return furnishingData.getCumulativeWeights();
    }
}
