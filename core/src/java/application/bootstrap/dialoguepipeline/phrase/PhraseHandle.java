package application.bootstrap.dialoguepipeline.phrase;

import application.bootstrap.dialoguepipeline.util.DialogueLineStruct;
import engine.root.HandlePackage;

public class PhraseHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded phrase definition. Registered and owned by
     * PhraseManager. Delegates all accessors through PhraseData.
     */

    // Internal
    private PhraseData phraseData;

    // Constructor \\

    public void constructor(PhraseData phraseData) {

        // Internal
        this.phraseData = phraseData;
    }

    // Accessible \\

    public PhraseData getPhraseData() {
        return phraseData;
    }

    public String getPhraseName() {
        return phraseData.getPhraseName();
    }

    public short getPhraseID() {
        return phraseData.getPhraseID();
    }

    public int getSeed() {
        return phraseData.getSeed();
    }

    public DialogueLineStruct[] getLines() {
        return phraseData.getLines();
    }
}
