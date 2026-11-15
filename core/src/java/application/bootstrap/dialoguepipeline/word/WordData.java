package application.bootstrap.dialoguepipeline.word;

import application.bootstrap.dialoguepipeline.util.DialogueConditionStruct;
import engine.root.DataPackage;

public class WordData extends DataPackage {

    /*
     * Immutable dictionary word loaded from ARPG: its name is the word as it is
     * said, its forms every other spelling, inflection or synonym heard as it, its
     * tags the parts of speech and meanings it is drawn by and understood through,
     * its tone how kindly or harshly it lands from -1 to 1, and its condition who
     * may say it. The seed is the stable number of its name the dialogue models
     * weigh it by. Owned by WordHandle for the engine lifetime.
     */

    // Identity
    private final String wordName;
    private final int wordID;
    private final int seed;

    // Speech
    private final String[] forms;
    private final int[] tagIDs;
    private final float tone;
    private final DialogueConditionStruct condition;

    // Constructor \\

    public WordData(
            String wordName,
            int wordID,
            int seed,
            String[] forms,
            int[] tagIDs,
            float tone,
            DialogueConditionStruct condition) {

        // Identity
        this.wordName = wordName;
        this.wordID = wordID;
        this.seed = seed;

        // Speech
        this.forms = forms;
        this.tagIDs = tagIDs;
        this.tone = tone;
        this.condition = condition;
    }

    // Accessible \\

    public String getWordName() {
        return wordName;
    }

    public int getWordID() {
        return wordID;
    }

    public int getSeed() {
        return seed;
    }

    public String[] getForms() {
        return forms;
    }

    public int[] getTagIDs() {
        return tagIDs;
    }

    public float getTone() {
        return tone;
    }

    public DialogueConditionStruct getCondition() {
        return condition;
    }
}
