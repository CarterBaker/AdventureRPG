package application.bootstrap.dialoguepipeline.intent;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.HandlePackage;

public class IntentHandle extends HandlePackage {

    /*
     * Persistent reference to an loaded intent definition. Registered and owned by
     * IntentManager. Delegates all accessors through IntentData.
     */

    // Internal
    private IntentData intentData;

    // Constructor \\

    public void constructor(IntentData intentData) {

        // Internal
        this.intentData = intentData;
    }

    // Accessible \\

    public IntentData getIntentData() {
        return intentData;
    }

    public String getIntentName() {
        return intentData.getIntentName();
    }

    public short getIntentID() {
        return intentData.getIntentID();
    }

    public int getSeed() {
        return intentData.getSeed();
    }

    public String getDescription() {
        return intentData.getDescription();
    }

    public String[] getExamples() {
        return intentData.getExamples();
    }

    public float getAffinity() {
        return intentData.getAffinity();
    }

    public ValueMapStruct getMoodShifts() {
        return intentData.getMoodShifts();
    }

    public short[] getReplyIDs() {
        return intentData.getReplyIDs();
    }
}
