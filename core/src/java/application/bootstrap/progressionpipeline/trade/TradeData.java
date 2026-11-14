package application.bootstrap.progressionpipeline.trade;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.DataPackage;

public class TradeData extends DataPackage {

    /*
     * Immutable trade definition loaded from ARPG: a broad craft a being grows
     * in by practice and loses by neglect. Its value lies between min and max;
     * below zero it is ineptitude. Each whole step of it adds the fraction its
     * stat bonuses name to those stats, always. The learn rate scales every
     * gain, and a trade that does not decay keeps what it reaches. The group
     * gathers trades under one header when they are listed. Owned by
     * TradeHandle for the engine lifetime.
     */

    // Identity
    private final String tradeName;
    private final short tradeID;

    // Presentation
    private final String displayName;
    private final String description;
    private final String group;

    // Range
    private final float min;
    private final float max;

    // Learning
    private final float learnRate;
    private final boolean decaying;

    // Effects
    private final ValueMapStruct statBonuses;

    // Constructor \\

    public TradeData(
            String tradeName,
            short tradeID,
            String displayName,
            String description,
            String group,
            float min,
            float max,
            float learnRate,
            boolean decaying,
            ValueMapStruct statBonuses) {

        // Identity
        this.tradeName = tradeName;
        this.tradeID = tradeID;

        // Presentation
        this.displayName = displayName;
        this.description = description;
        this.group = group;

        // Range
        this.min = min;
        this.max = max;

        // Learning
        this.learnRate = learnRate;
        this.decaying = decaying;

        // Effects
        this.statBonuses = statBonuses;
    }

    // Accessible \\

    public String getTradeName() {
        return tradeName;
    }

    public short getTradeID() {
        return tradeID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getGroup() {
        return group;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getLearnRate() {
        return learnRate;
    }

    public boolean isDecaying() {
        return decaying;
    }

    public ValueMapStruct getStatBonuses() {
        return statBonuses;
    }
}
