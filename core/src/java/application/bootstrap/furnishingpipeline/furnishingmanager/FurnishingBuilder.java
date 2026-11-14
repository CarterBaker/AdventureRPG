package application.bootstrap.furnishingpipeline.furnishingmanager;

import java.io.File;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingData;
import application.bootstrap.furnishingpipeline.furnishing.FurnishingHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.WeightedTableUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class FurnishingBuilder extends BuilderPackage {

    /*
     * Parses one furnishing ARPG file into a FurnishingData and wraps it in a
     * FurnishingHandle. Its "items" each name an item and an optional
     * "weight", one by default, and every item is resolved here, so a table
     * naming an unknown item fails at boot.
     */

    // Internal
    private FurnishingManager furnishingManager;
    private ItemDefinitionManager itemDefinitionManager;

    // Base \\

    @Override
    protected void get() {
        this.furnishingManager = get(FurnishingManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
    }

    // Build \\

    FurnishingHandle build(File file, String furnishingName) {

        short furnishingID = furnishingManager.registerFurnishingName(furnishingName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        ArpgArrayStruct itemsArpg = ArpgUtility.validateArray(arpg, "items");

        ItemDefinitionHandle[] items = new ItemDefinitionHandle[itemsArpg.size()];
        FloatArrayList weights = new FloatArrayList(itemsArpg.size());

        for (int i = 0; i < itemsArpg.size(); i++) {

            ArpgObjectStruct entry = itemsArpg.get(i).getAsObject();

            items[i] = itemDefinitionManager.getItemHandleFromItemName(ArpgUtility.validateString(entry, "item"));
            weights.add(ArpgUtility.getFloat(entry, "weight", 1f));
        }

        FurnishingData furnishingData = new FurnishingData(
                furnishingName,
                furnishingID,
                items,
                WeightedTableUtility.buildCumulative(weights, furnishingName));

        FurnishingHandle furnishingHandle = create(FurnishingHandle.class);
        furnishingHandle.constructor(furnishingData);

        return furnishingHandle;
    }
}
