package application.bootstrap.dialoguepipeline.phrase;

import application.bootstrap.dialoguepipeline.util.DialogueLineStruct;
import engine.root.DataPackage;

public class PhraseData extends DataPackage {

    /*
     * Immutable phrase loaded from ARPG: a named piece of speech any line may call
     * for in braces, and every way it can be said. The seed is the stable number
     * of its name. Owned by PhraseHandle for the engine lifetime.
     */

    // Identity
    private final String phraseName;
    private final short phraseID;
    private final int seed;

    // Speech
    private final DialogueLineStruct[] lines;

    // Constructor \\

    public PhraseData(
            String phraseName,
            short phraseID,
            int seed,
            DialogueLineStruct[] lines) {

        // Identity
        this.phraseName = phraseName;
        this.phraseID = phraseID;
        this.seed = seed;

        // Speech
        this.lines = lines;
    }

    // Accessible \\

    public String getPhraseName() {
        return phraseName;
    }

    public short getPhraseID() {
        return phraseID;
    }

    public int getSeed() {
        return seed;
    }

    public DialogueLineStruct[] getLines() {
        return lines;
    }
}
