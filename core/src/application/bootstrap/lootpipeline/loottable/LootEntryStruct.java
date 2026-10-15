package application.bootstrap.lootpipeline.loottable;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.StructPackage;

public class LootEntryStruct extends StructPackage {

    /*
     * One entry of a loot table: the item a roll landing on it yields, and the
     * least and most of it that roll gives, split into as many stacks as the
     * item's stack size needs.
     */

    // Item
    private final ItemDefinitionHandle itemDefinitionHandle;

    // Count
    private final int countMin;
    private final int countMax;

    // Constructor \\

    public LootEntryStruct(ItemDefinitionHandle itemDefinitionHandle, int countMin, int countMax) {

        // Item
        this.itemDefinitionHandle = itemDefinitionHandle;

        // Count
        this.countMin = countMin;
        this.countMax = countMax;
    }

    // Accessible \\

    public ItemDefinitionHandle getItemDefinitionHandle() {
        return itemDefinitionHandle;
    }

    public int getCountMin() {
        return countMin;
    }

    public int getCountMax() {
        return countMax;
    }
}
