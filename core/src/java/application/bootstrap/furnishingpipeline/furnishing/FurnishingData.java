package application.bootstrap.furnishingpipeline.furnishing;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.DataPackage;

public class FurnishingData extends DataPackage {

    /*
     * Immutable furnishing table built from ARPG by FurnishingBuilder: the
     * items a furnished place may hold, resolved to their definitions, and
     * the running weight totals one roll picks among them with.
     */

    // Identity
    private final String furnishingName;
    private final short furnishingID;

    // Table
    private final ItemDefinitionHandle[] items;
    private final float[] cumulativeWeights;

    // Constructor \\

    public FurnishingData(
            String furnishingName,
            short furnishingID,
            ItemDefinitionHandle[] items,
            float[] cumulativeWeights) {

        // Identity
        this.furnishingName = furnishingName;
        this.furnishingID = furnishingID;

        // Table
        this.items = items;
        this.cumulativeWeights = cumulativeWeights;
    }

    // Accessible \\

    public String getFurnishingName() {
        return furnishingName;
    }

    public short getFurnishingID() {
        return furnishingID;
    }

    public ItemDefinitionHandle[] getItems() {
        return items;
    }

    public float[] getCumulativeWeights() {
        return cumulativeWeights;
    }
}
