package application.bootstrap.progressionpipeline.stat;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.HandlePackage;

public class StatHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded stat definition. Registered and owned
     * by StatManager. Delegates all accessors through StatData.
     */

    // Internal
    private StatData statData;

    // Constructor \\

    public void constructor(StatData statData) {

        // Internal
        this.statData = statData;
    }

    // Accessible \\

    public StatData getStatData() {
        return statData;
    }

    public String getStatName() {
        return statData.getStatName();
    }

    public short getStatID() {
        return statData.getStatID();
    }

    public String getDisplayName() {
        return statData.getDisplayName();
    }

    public String getDescription() {
        return statData.getDescription();
    }

    public StatFormat getFormat() {
        return statData.getFormat();
    }

    public boolean isShown() {
        return statData.isShown();
    }

    public boolean isScaled() {
        return statData.isScaled();
    }

    public float getBase() {
        return statData.getBase();
    }

    public float getMin() {
        return statData.getMin();
    }

    public float getMax() {
        return statData.getMax();
    }

    public ValueMapStruct getAttributeFeeds() {
        return statData.getAttributeFeeds();
    }

    public float getBonusWeight() {
        return statData.getBonusWeight();
    }

    public float getBonusValue() {
        return statData.getBonusValue();
    }
}
