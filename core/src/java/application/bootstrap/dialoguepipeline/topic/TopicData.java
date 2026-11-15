package application.bootstrap.dialoguepipeline.topic;

import application.bootstrap.dialoguepipeline.util.DialogueConditionStruct;
import application.bootstrap.dialoguepipeline.util.DialogueLineStruct;
import engine.root.DataPackage;

public class TopicData extends DataPackage {

    /*
     * Immutable topic loaded from ARPG: a subject a being can be asked about, the
     * dictionary words that raise it, who knows enough to speak of it, and what
     * they can say. The seed is the stable number of its name the dialogue models
     * weigh it by. Owned by TopicHandle for the engine lifetime.
     */

    // Identity
    private final String topicName;
    private final short topicID;
    private final int seed;

    // Display
    private final String displayName;

    // Speech
    private final int[] wordIDs;
    private final DialogueConditionStruct condition;
    private final DialogueLineStruct[] lines;

    // Constructor \\

    public TopicData(
            String topicName,
            short topicID,
            int seed,
            String displayName,
            int[] wordIDs,
            DialogueConditionStruct condition,
            DialogueLineStruct[] lines) {

        // Identity
        this.topicName = topicName;
        this.topicID = topicID;
        this.seed = seed;

        // Display
        this.displayName = displayName;

        // Speech
        this.wordIDs = wordIDs;
        this.condition = condition;
        this.lines = lines;
    }

    // Accessible \\

    public String getTopicName() {
        return topicName;
    }

    public short getTopicID() {
        return topicID;
    }

    public int getSeed() {
        return seed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int[] getWordIDs() {
        return wordIDs;
    }

    public DialogueConditionStruct getCondition() {
        return condition;
    }

    public DialogueLineStruct[] getLines() {
        return lines;
    }
}
