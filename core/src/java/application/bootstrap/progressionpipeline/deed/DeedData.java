package application.bootstrap.progressionpipeline.deed;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.DataPackage;

public class DeedData extends DataPackage {

    /*
     * Immutable deed definition loaded from ARPG: one notable action and what
     * doing it is worth. Experience is a share of one even-level kill, so a
     * deed is worth as much at level one hundred as at level one; morality
     * moves the doer's conscience; trade gains practice the trades they name;
     * and a counted deed is tallied for the requirements that read it. Owned
     * by DeedHandle for the engine lifetime.
     */

    // Identity
    private final String deedName;
    private final short deedID;

    // Presentation
    private final String displayName;
    private final String description;

    // Worth
    private final float experience;
    private final float morality;
    private final ValueMapStruct tradeGains;

    // Tally
    private final boolean counted;

    // Constructor \\

    public DeedData(
            String deedName,
            short deedID,
            String displayName,
            String description,
            float experience,
            float morality,
            ValueMapStruct tradeGains,
            boolean counted) {

        // Identity
        this.deedName = deedName;
        this.deedID = deedID;

        // Presentation
        this.displayName = displayName;
        this.description = description;

        // Worth
        this.experience = experience;
        this.morality = morality;
        this.tradeGains = tradeGains;

        // Tally
        this.counted = counted;
    }

    // Accessible \\

    public String getDeedName() {
        return deedName;
    }

    public short getDeedID() {
        return deedID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public float getExperience() {
        return experience;
    }

    public float getMorality() {
        return morality;
    }

    public ValueMapStruct getTradeGains() {
        return tradeGains;
    }

    public boolean isCounted() {
        return counted;
    }
}
