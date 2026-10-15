package application.bootstrap.lootpipeline.loottablemanager;

import java.io.File;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.lootpipeline.loottable.LootEntryStruct;
import application.bootstrap.lootpipeline.loottable.LootTableData;
import application.bootstrap.lootpipeline.loottable.LootTableHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.WeightedTableUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class LootTableBuilder extends BuilderPackage {

    /*
     * Parses one loot table ARPG file into a LootTableData and wraps it in a
     * LootTableHandle. Every draw gives each of the "guaranteed" entries, then
     * makes from "rolls_min" to "rolls_max" rolls, one of each by default,
     * every roll landing on one of the "entries" by its "weight", one by
     * default, or on nothing by the table's "empty_weight". An entry names its
     * "item" and gives from "count_min" to "count_max" of it. A table lists at
     * least one entry of either kind, and every item and count is resolved
     * and validated here, so a table naming an unknown item fails at boot.
     */

    // Internal
    private LootTableManager lootTableManager;
    private ItemDefinitionManager itemDefinitionManager;

    // Base \\

    @Override
    protected void get() {
        this.lootTableManager = get(LootTableManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
    }

    // Build \\

    LootTableHandle build(File file, String lootTableName) {

        short lootTableID = lootTableManager.registerLootTableName(lootTableName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        int rollsMin = ArpgUtility.getInt(arpg, "rolls_min", EngineSetting.DEFAULT_LOOT_ROLLS);
        int rollsMax = ArpgUtility.getInt(arpg, "rolls_max", rollsMin);
        float emptyWeight = ArpgUtility.getFloat(arpg, "empty_weight", 0f);

        if (rollsMin < 0 || rollsMax < rollsMin)
            throwException("Loot table \"" + lootTableName + "\" rolls from " + rollsMin + " to " + rollsMax
                    + " — rolls must start at 0 or more and never fall.");

        if (emptyWeight < 0f)
            throwException("Loot table \"" + lootTableName + "\" has a negative empty weight.");

        FloatArrayList weights = new FloatArrayList();
        LootEntryStruct[] guaranteedEntries = parseEntries(arpg, "guaranteed", null, lootTableName);
        LootEntryStruct[] entries = parseEntries(arpg, "entries", weights, lootTableName);

        if (guaranteedEntries.length == 0 && entries.length == 0)
            throwException("Loot table \"" + lootTableName + "\" lists no \"guaranteed\" or \"entries\" items.");

        if (emptyWeight > 0f)
            weights.add(emptyWeight);

        LootTableData lootTableData = new LootTableData(
                lootTableName,
                lootTableID,
                RegistryUtility.toNameSeed(lootTableName),
                rollsMin,
                rollsMax,
                guaranteedEntries,
                entries,
                weights.isEmpty() ? new float[0] : WeightedTableUtility.buildCumulative(weights, lootTableName));

        LootTableHandle lootTableHandle = create(LootTableHandle.class);
        lootTableHandle.constructor(lootTableData);

        return lootTableHandle;
    }

    // Parse \\

    // Every entry listed under a key, its weight collected into the list given when one is
    private LootEntryStruct[] parseEntries(
            ArpgObjectStruct arpg,
            String key,
            FloatArrayList weights,
            String lootTableName) {

        if (!ArpgUtility.hasArray(arpg, key))
            return new LootEntryStruct[0];

        ArpgArrayStruct entriesArpg = arpg.getAsArray(key);
        LootEntryStruct[] entries = new LootEntryStruct[entriesArpg.size()];

        for (int i = 0; i < entries.length; i++) {

            ArpgObjectStruct entryArpg = entriesArpg.get(i).getAsObject();

            entries[i] = parseEntry(entryArpg, lootTableName);

            if (weights != null)
                weights.add(ArpgUtility.getFloat(entryArpg, "weight", EngineSetting.DEFAULT_LOOT_WEIGHT));
        }

        return entries;
    }

    private LootEntryStruct parseEntry(ArpgObjectStruct entryArpg, String lootTableName) {

        String itemName = ArpgUtility.validateString(entryArpg, "item");
        ItemDefinitionHandle itemDefinitionHandle = itemDefinitionManager.getItemHandleFromItemName(itemName);
        int countMin = ArpgUtility.getInt(entryArpg, "count_min", EngineSetting.DEFAULT_LOOT_COUNT);
        int countMax = ArpgUtility.getInt(entryArpg, "count_max", countMin);

        if (countMin < 1 || countMax < countMin)
            throwException("Loot table \"" + lootTableName + "\" gives '" + itemName + "' from " + countMin
                    + " to " + countMax + " — a count starts at 1 or more and never falls.");

        return new LootEntryStruct(itemDefinitionHandle, countMin, countMax);
    }
}
