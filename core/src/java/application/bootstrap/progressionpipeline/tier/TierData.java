package application.bootstrap.progressionpipeline.tier;

import engine.graphics.color.Color;
import engine.root.DataPackage;

public class TierData extends DataPackage {

    /*
     * Immutable gear tier definition loaded from ARPG. Its rank is its place
     * from the lowest tier, counted from zero. A tier multiplies the stats of
     * every piece of gear it is rolled on, grants that many rolled bonuses,
     * and is drawn by its weight. Its color frames the gear and tints its
     * name; glow, pulse, sweep, motes and sheen, each from 0 to 1, are how
     * strongly its frame shows each of the frame shader's effects. Owned by
     * TierHandle for the engine lifetime.
     */

    // Identity
    private final String tierName;
    private final short tierID;
    private final int rank;

    // Presentation
    private final String displayName;
    private final Color color;

    // Gear
    private final float multiplier;
    private final int bonusCount;
    private final float weight;

    // Frame
    private final float glow;
    private final float pulse;
    private final float sweep;
    private final float motes;
    private final float sheen;

    // Constructor \\

    public TierData(
            String tierName,
            short tierID,
            int rank,
            String displayName,
            Color color,
            float multiplier,
            int bonusCount,
            float weight,
            float glow,
            float pulse,
            float sweep,
            float motes,
            float sheen) {

        // Identity
        this.tierName = tierName;
        this.tierID = tierID;
        this.rank = rank;

        // Presentation
        this.displayName = displayName;
        this.color = color;

        // Gear
        this.multiplier = multiplier;
        this.bonusCount = bonusCount;
        this.weight = weight;

        // Frame
        this.glow = glow;
        this.pulse = pulse;
        this.sweep = sweep;
        this.motes = motes;
        this.sheen = sheen;
    }

    // Accessible \\

    public String getTierName() {
        return tierName;
    }

    public short getTierID() {
        return tierID;
    }

    public int getRank() {
        return rank;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getColor() {
        return color;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public int getBonusCount() {
        return bonusCount;
    }

    public float getWeight() {
        return weight;
    }

    public float getGlow() {
        return glow;
    }

    public float getPulse() {
        return pulse;
    }

    public float getSweep() {
        return sweep;
    }

    public float getMotes() {
        return motes;
    }

    public float getSheen() {
        return sheen;
    }
}
