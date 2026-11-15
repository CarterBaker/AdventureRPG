package application.bootstrap.dialoguepipeline.util;

import engine.root.StructPackage;

public class DialogueLineStruct extends StructPackage {

    /*
     * One immutable way of saying something, cut into its segments at load:
     * text said as written, phrases expanded in place, dictionary draws by
     * tag and values taken from the talk. Each segment's kind is paired by
     * index with its text, its reference — a phrase ID or a value's ordinal —
     * and its tag IDs, whichever its kind reads. The key is the stable number
     * training knows the line by, drawn from its owner's name and its text,
     * the weight how often it is chosen before training has a say, and the
     * condition who may say it.
     */

    // Identity
    private final int key;
    private final float weight;
    private final DialogueConditionStruct condition;
    private final boolean subjectNamed;
    private final boolean topicExpanded;

    // Segments
    private final DialogueSegment[] kinds;
    private final String[] texts;
    private final int[] references;
    private final int[][] tagIDs;

    // Constructor \\

    public DialogueLineStruct(
            int key,
            float weight,
            DialogueConditionStruct condition,
            boolean subjectNamed,
            boolean topicExpanded,
            DialogueSegment[] kinds,
            String[] texts,
            int[] references,
            int[][] tagIDs) {

        // Identity
        this.key = key;
        this.weight = weight;
        this.condition = condition;
        this.subjectNamed = subjectNamed;
        this.topicExpanded = topicExpanded;

        // Segments
        this.kinds = kinds;
        this.texts = texts;
        this.references = references;
        this.tagIDs = tagIDs;
    }

    // Match \\

    public boolean matches(DialogueProfileStruct profile, DialogueTopicState talkState) {

        if (subjectNamed && talkState == DialogueTopicState.NONE)
            return false;

        if (topicExpanded && talkState != DialogueTopicState.KNOWN)
            return false;

        return condition.matches(profile, talkState);
    }

    // Accessible \\

    public int getKey() {
        return key;
    }

    public float getWeight() {
        return weight;
    }

    public int getSegmentCount() {
        return kinds.length;
    }

    public DialogueSegment getKind(int index) {
        return kinds[index];
    }

    public String getText(int index) {
        return texts[index];
    }

    public int getReference(int index) {
        return references[index];
    }

    public int[] getTagIDs(int index) {
        return tagIDs[index];
    }
}
