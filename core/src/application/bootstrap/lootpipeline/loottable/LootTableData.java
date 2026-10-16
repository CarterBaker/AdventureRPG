package application.bootstrap.lootpipeline.loottable;

import engine.root.DataPackage;

public class LootTableData extends DataPackage {

    /*
     * Immutable loot table built from ARPG by LootTableBuilder: the entries
     * every draw gives, the least and most rolls one draw makes, the entries a
     * roll may land on, and the running weight totals a roll picks among them
     * with, empty when nothing is rolled for. A table with an empty weight
     * carries one total past its last entry, and a roll landing there yields
     * nothing. Every roll is salted by the name seed, so a table draws alike
     * whatever order it loads in. Gear a table gives is forged at the level of
     * whoever it is drawn for, or, for a container found in the world, at a
     * level drawn from the table's own range, its tier tilted by the table's
     * bias.
     */

    // Identity
    private final String lootTableName;
    private final short lootTableID;
    private final int nameSeed;

    // Rolls
    private final int rollsMin;
    private final int rollsMax;

    // Gear
    private final int levelMin;
    private final int levelMax;
    private final float tierBias;

    // Table
    private final LootEntryStruct[] guaranteedEntries;
    private final LootEntryStruct[] entries;
    private final float[] cumulativeWeights;

    // Constructor \\

    public LootTableData(
            String lootTableName,
            short lootTableID,
            int nameSeed,
            int rollsMin,
            int rollsMax,
            int levelMin,
            int levelMax,
            float tierBias,
            LootEntryStruct[] guaranteedEntries,
            LootEntryStruct[] entries,
            float[] cumulativeWeights) {

        // Identity
        this.lootTableName = lootTableName;
        this.lootTableID = lootTableID;
        this.nameSeed = nameSeed;

        // Rolls
        this.rollsMin = rollsMin;
        this.rollsMax = rollsMax;

        // Gear
        this.levelMin = levelMin;
        this.levelMax = levelMax;
        this.tierBias = tierBias;

        // Table
        this.guaranteedEntries = guaranteedEntries;
        this.entries = entries;
        this.cumulativeWeights = cumulativeWeights;
    }

    // Accessible \\

    public String getLootTableName() {
        return lootTableName;
    }

    public short getLootTableID() {
        return lootTableID;
    }

    public int getNameSeed() {
        return nameSeed;
    }

    public int getRollsMin() {
        return rollsMin;
    }

    public int getRollsMax() {
        return rollsMax;
    }

    public int getLevelMin() {
        return levelMin;
    }

    public int getLevelMax() {
        return levelMax;
    }

    public float getTierBias() {
        return tierBias;
    }

    public LootEntryStruct[] getGuaranteedEntries() {
        return guaranteedEntries;
    }

    public LootEntryStruct[] getEntries() {
        return entries;
    }

    public boolean hasRolls() {
        return cumulativeWeights.length > 0;
    }

    public float[] getCumulativeWeights() {
        return cumulativeWeights;
    }
}
