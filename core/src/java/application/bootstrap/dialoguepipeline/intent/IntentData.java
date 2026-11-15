package application.bootstrap.dialoguepipeline.intent;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.DataPackage;

public class IntentData extends DataPackage {

    /*
     * Immutable intent loaded from ARPG: one thing a speaker can mean. It carries
     * the example sentences the understanding model first learns it from, how
     * being told it moves the listener's regard for the speaker and its moods, and
     * the replies it can be answered with. The seed is the stable number of its
     * name the dialogue models weigh it by. Owned by IntentHandle for the engine
     * lifetime.
     */

    // Identity
    private final String intentName;
    private final short intentID;
    private final int seed;

    // Display
    private final String description;

    // Training
    private final String[] examples;

    // Effect
    private final float affinity;
    private final ValueMapStruct moodShifts;

    // Replies
    private final short[] replyIDs;

    // Constructor \\

    public IntentData(
            String intentName,
            short intentID,
            int seed,
            String description,
            String[] examples,
            float affinity,
            ValueMapStruct moodShifts,
            short[] replyIDs) {

        // Identity
        this.intentName = intentName;
        this.intentID = intentID;
        this.seed = seed;

        // Display
        this.description = description;

        // Training
        this.examples = examples;

        // Effect
        this.affinity = affinity;
        this.moodShifts = moodShifts;

        // Replies
        this.replyIDs = replyIDs;
    }

    // Accessible \\

    public String getIntentName() {
        return intentName;
    }

    public short getIntentID() {
        return intentID;
    }

    public int getSeed() {
        return seed;
    }

    public String getDescription() {
        return description;
    }

    public String[] getExamples() {
        return examples;
    }

    public float getAffinity() {
        return affinity;
    }

    public ValueMapStruct getMoodShifts() {
        return moodShifts;
    }

    public short[] getReplyIDs() {
        return replyIDs;
    }
}
