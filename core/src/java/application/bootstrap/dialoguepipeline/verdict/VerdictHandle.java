package application.bootstrap.dialoguepipeline.verdict;

import engine.root.HandlePackage;

public class VerdictHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded verdict definition. Registered and owned by
     * VerdictManager. Delegates all accessors through VerdictData.
     */

    // Internal
    private VerdictData verdictData;

    // Constructor \\

    public void constructor(VerdictData verdictData) {

        // Internal
        this.verdictData = verdictData;
    }

    // Accessible \\

    public VerdictData getVerdictData() {
        return verdictData;
    }

    public String getVerdictName() {
        return verdictData.getVerdictName();
    }

    public short getVerdictID() {
        return verdictData.getVerdictID();
    }

    public int getSeed() {
        return verdictData.getSeed();
    }

    public String getDisplayName() {
        return verdictData.getDisplayName();
    }

    public String getDescription() {
        return verdictData.getDescription();
    }

    public float getUnderstanding() {
        return verdictData.getUnderstanding();
    }

    public float getResponse() {
        return verdictData.getResponse();
    }

    public float getWording() {
        return verdictData.getWording();
    }
}
