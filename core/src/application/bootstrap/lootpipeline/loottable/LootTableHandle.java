package application.bootstrap.lootpipeline.loottable;

import engine.root.HandlePackage;

public class LootTableHandle extends HandlePackage {

    /*
     * Persistent loot table. Wraps LootTableData and delegates all access
     * through it.
     */

    // Internal
    private LootTableData lootTableData;

    // Constructor \\

    public void constructor(LootTableData lootTableData) {
        this.lootTableData = lootTableData;
    }

    // Accessible \\

    public LootTableData getLootTableData() {
        return lootTableData;
    }

    public String getLootTableName() {
        return lootTableData.getLootTableName();
    }

    public short getLootTableID() {
        return lootTableData.getLootTableID();
    }

    public int getNameSeed() {
        return lootTableData.getNameSeed();
    }

    public int getRollsMin() {
        return lootTableData.getRollsMin();
    }

    public int getRollsMax() {
        return lootTableData.getRollsMax();
    }

    public int getLevelMin() {
        return lootTableData.getLevelMin();
    }

    public int getLevelMax() {
        return lootTableData.getLevelMax();
    }

    public float getTierBias() {
        return lootTableData.getTierBias();
    }

    public LootEntryStruct[] getGuaranteedEntries() {
        return lootTableData.getGuaranteedEntries();
    }

    public LootEntryStruct[] getEntries() {
        return lootTableData.getEntries();
    }

    public boolean hasRolls() {
        return lootTableData.hasRolls();
    }

    public float[] getCumulativeWeights() {
        return lootTableData.getCumulativeWeights();
    }
}
