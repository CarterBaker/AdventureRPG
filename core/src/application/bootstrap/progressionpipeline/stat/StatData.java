package application.bootstrap.progressionpipeline.stat;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.DataPackage;

public class StatData extends DataPackage {

    /*
     * Immutable stat definition loaded from ARPG. A scaled stat is measured
     * against its owner's level, so the growth of levels applies to it and a
     * level gap between two beings multiplies it; a plain stat is the same at
     * every level. Every being starts from the base value, each attribute
     * point above the starting value adds its fraction from the attribute
     * feeds, and the total is held between min and max. A bonus weight above
     * zero lets gear roll the stat as a bonus, worth up to the bonus value
     * before the gear's tier multiplies it. Owned by StatHandle for the engine
     * lifetime.
     */

    // Identity
    private final String statName;
    private final short statID;

    // Presentation
    private final String displayName;
    private final String description;
    private final StatFormat format;
    private final boolean shown;

    // Value
    private final boolean scaled;
    private final float base;
    private final float min;
    private final float max;

    // Feeds
    private final ValueMapStruct attributeFeeds;

    // Gear
    private final float bonusWeight;
    private final float bonusValue;

    // Constructor \\

    public StatData(
            String statName,
            short statID,
            String displayName,
            String description,
            StatFormat format,
            boolean shown,
            boolean scaled,
            float base,
            float min,
            float max,
            ValueMapStruct attributeFeeds,
            float bonusWeight,
            float bonusValue) {

        // Identity
        this.statName = statName;
        this.statID = statID;

        // Presentation
        this.displayName = displayName;
        this.description = description;
        this.format = format;
        this.shown = shown;

        // Value
        this.scaled = scaled;
        this.base = base;
        this.min = min;
        this.max = max;

        // Feeds
        this.attributeFeeds = attributeFeeds;

        // Gear
        this.bonusWeight = bonusWeight;
        this.bonusValue = bonusValue;
    }

    // Accessible \\

    public String getStatName() {
        return statName;
    }

    public short getStatID() {
        return statID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public StatFormat getFormat() {
        return format;
    }

    public boolean isShown() {
        return shown;
    }

    public boolean isScaled() {
        return scaled;
    }

    public float getBase() {
        return base;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public ValueMapStruct getAttributeFeeds() {
        return attributeFeeds;
    }

    public float getBonusWeight() {
        return bonusWeight;
    }

    public float getBonusValue() {
        return bonusValue;
    }
}
