package application.bootstrap.worldpipeline.tree;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class TreeGrowthStruct extends StructPackage {

    /*
     * How a species grows: the game days a seed takes to reach maturity, the
     * range of ages its wild trees stand at when the world generates them,
     * and the seed it drops — the item, resolved once at load, and the chance
     * each leaf cluster of a felled tree lets one fall.
     */

    private final float days;
    private final float wildMinAge;
    private final float wildMaxAge;
    private final String seedItemName;
    private final int seedItemID;
    private final float seedChance;

    public TreeGrowthStruct(
            float days,
            float wildMinAge,
            float wildMaxAge,
            String seedItemName,
            int seedItemID,
            float seedChance) {

        this.days = days;
        this.wildMinAge = wildMinAge;
        this.wildMaxAge = wildMaxAge;
        this.seedItemName = seedItemName;
        this.seedItemID = seedItemID;
        this.seedChance = seedChance;
    }

    // Accessible \\

    public float getDays() {
        return days;
    }

    public float getWildMinAge() {
        return wildMinAge;
    }

    public float getWildMaxAge() {
        return wildMaxAge;
    }

    public String getSeedItemName() {
        return seedItemName;
    }

    public int getSeedItemID() {
        return seedItemID;
    }

    public boolean hasSeed() {
        return seedItemID != EngineSetting.REGISTRY_RESERVED_ID;
    }

    public float getSeedChance() {
        return seedChance;
    }
}
