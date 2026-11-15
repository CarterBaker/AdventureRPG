package application.bootstrap.dialoguepipeline.word;

import application.bootstrap.dialoguepipeline.util.DialogueConditionStruct;
import engine.root.HandlePackage;

public class WordHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded word definition. Registered and owned by
     * WordManager. Delegates all accessors through WordData.
     */

    // Internal
    private WordData wordData;

    // Constructor \\

    public void constructor(WordData wordData) {

        // Internal
        this.wordData = wordData;
    }

    // Accessible \\

    public WordData getWordData() {
        return wordData;
    }

    public String getWordName() {
        return wordData.getWordName();
    }

    public int getWordID() {
        return wordData.getWordID();
    }

    public int getSeed() {
        return wordData.getSeed();
    }

    public String[] getForms() {
        return wordData.getForms();
    }

    public int[] getTagIDs() {
        return wordData.getTagIDs();
    }

    public float getTone() {
        return wordData.getTone();
    }

    public DialogueConditionStruct getCondition() {
        return wordData.getCondition();
    }
}
