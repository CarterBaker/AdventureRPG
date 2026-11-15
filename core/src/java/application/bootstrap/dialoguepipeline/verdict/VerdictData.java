package application.bootstrap.dialoguepipeline.verdict;

import engine.root.DataPackage;

public class VerdictData extends DataPackage {

    /*
     * Immutable verdict loaded from ARPG: one judgement a trainer can pass on a
     * turn of talk, the name it is shown by, and what it is worth, from -1 to 1,
     * to the understanding of what was said, to the choice of reply and to its
     * wording. The seed is the stable number of its name. Owned by VerdictHandle
     * for the engine lifetime.
     */

    // Identity
    private final String verdictName;
    private final short verdictID;
    private final int seed;

    // Display
    private final String displayName;
    private final String description;

    // Worth
    private final float understanding;
    private final float response;
    private final float wording;

    // Constructor \\

    public VerdictData(
            String verdictName,
            short verdictID,
            int seed,
            String displayName,
            String description,
            float understanding,
            float response,
            float wording) {

        // Identity
        this.verdictName = verdictName;
        this.verdictID = verdictID;
        this.seed = seed;

        // Display
        this.displayName = displayName;
        this.description = description;

        // Worth
        this.understanding = understanding;
        this.response = response;
        this.wording = wording;
    }

    // Accessible \\

    public String getVerdictName() {
        return verdictName;
    }

    public short getVerdictID() {
        return verdictID;
    }

    public int getSeed() {
        return seed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public float getUnderstanding() {
        return understanding;
    }

    public float getResponse() {
        return response;
    }

    public float getWording() {
        return wording;
    }
}
