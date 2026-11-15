package application.bootstrap.dialoguepipeline.topic;

import application.bootstrap.dialoguepipeline.util.DialogueConditionStruct;
import application.bootstrap.dialoguepipeline.util.DialogueLineStruct;
import engine.root.HandlePackage;

public class TopicHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded topic definition. Registered and owned by
     * TopicManager. Delegates all accessors through TopicData.
     */

    // Internal
    private TopicData topicData;

    // Constructor \\

    public void constructor(TopicData topicData) {

        // Internal
        this.topicData = topicData;
    }

    // Accessible \\

    public TopicData getTopicData() {
        return topicData;
    }

    public String getTopicName() {
        return topicData.getTopicName();
    }

    public short getTopicID() {
        return topicData.getTopicID();
    }

    public int getSeed() {
        return topicData.getSeed();
    }

    public String getDisplayName() {
        return topicData.getDisplayName();
    }

    public int[] getWordIDs() {
        return topicData.getWordIDs();
    }

    public DialogueConditionStruct getCondition() {
        return topicData.getCondition();
    }

    public DialogueLineStruct[] getLines() {
        return topicData.getLines();
    }
}
