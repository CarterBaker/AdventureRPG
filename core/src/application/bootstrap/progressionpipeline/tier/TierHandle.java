package application.bootstrap.progressionpipeline.tier;

import engine.graphics.color.Color;
import engine.root.HandlePackage;

public class TierHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded gear tier definition. Registered and
     * owned by TierManager. Delegates all accessors through TierData.
     */

    // Internal
    private TierData tierData;

    // Constructor \\

    public void constructor(TierData tierData) {

        // Internal
        this.tierData = tierData;
    }

    // Accessible \\

    public TierData getTierData() {
        return tierData;
    }

    public String getTierName() {
        return tierData.getTierName();
    }

    public short getTierID() {
        return tierData.getTierID();
    }

    public int getRank() {
        return tierData.getRank();
    }

    public String getDisplayName() {
        return tierData.getDisplayName();
    }

    public Color getColor() {
        return tierData.getColor();
    }

    public float getMultiplier() {
        return tierData.getMultiplier();
    }

    public int getBonusCount() {
        return tierData.getBonusCount();
    }

    public float getWeight() {
        return tierData.getWeight();
    }

    public float getGlow() {
        return tierData.getGlow();
    }

    public float getPulse() {
        return tierData.getPulse();
    }

    public float getSweep() {
        return tierData.getSweep();
    }

    public float getMotes() {
        return tierData.getMotes();
    }

    public float getSheen() {
        return tierData.getSheen();
    }
}
