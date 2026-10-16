package application.bootstrap.progressionpipeline.deed;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.HandlePackage;

public class DeedHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded deed definition. Registered and owned
     * by DeedManager. Delegates all accessors through DeedData.
     */

    // Internal
    private DeedData deedData;

    // Constructor \\

    public void constructor(DeedData deedData) {

        // Internal
        this.deedData = deedData;
    }

    // Accessible \\

    public DeedData getDeedData() {
        return deedData;
    }

    public String getDeedName() {
        return deedData.getDeedName();
    }

    public short getDeedID() {
        return deedData.getDeedID();
    }

    public String getDisplayName() {
        return deedData.getDisplayName();
    }

    public String getDescription() {
        return deedData.getDescription();
    }

    public float getExperience() {
        return deedData.getExperience();
    }

    public float getMorality() {
        return deedData.getMorality();
    }

    public ValueMapStruct getTradeGains() {
        return deedData.getTradeGains();
    }

    public boolean isCounted() {
        return deedData.isCounted();
    }
}
