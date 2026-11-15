package application.bootstrap.dialoguepipeline.reply;

import application.bootstrap.dialoguepipeline.util.DialogueAction;
import application.bootstrap.dialoguepipeline.util.DialogueConditionStruct;
import application.bootstrap.dialoguepipeline.util.DialogueLeanStruct;
import application.bootstrap.dialoguepipeline.util.DialogueLineStruct;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.DataPackage;

public class ReplyData extends DataPackage {

    /*
     * Immutable reply loaded from ARPG: one thing a being can answer with. It
     * carries who may give it, who it comes naturally to before training has a
     * say, every way it can be worded, how giving it moves the replier's regard
     * for the speaker and its moods, and what it does in the world. The seed is
     * the stable number of its name the dialogue models weigh it by. Owned by
     * ReplyHandle for the engine lifetime.
     */

    // Identity
    private final String replyName;
    private final short replyID;
    private final int seed;

    // Display
    private final String description;

    // Choice
    private final DialogueConditionStruct condition;
    private final DialogueLeanStruct lean;

    // Speech
    private final DialogueLineStruct[] lines;

    // Effect
    private final float affinity;
    private final ValueMapStruct moodShifts;
    private final DialogueAction action;

    // Constructor \\

    public ReplyData(
            String replyName,
            short replyID,
            int seed,
            String description,
            DialogueConditionStruct condition,
            DialogueLeanStruct lean,
            DialogueLineStruct[] lines,
            float affinity,
            ValueMapStruct moodShifts,
            DialogueAction action) {

        // Identity
        this.replyName = replyName;
        this.replyID = replyID;
        this.seed = seed;

        // Display
        this.description = description;

        // Choice
        this.condition = condition;
        this.lean = lean;

        // Speech
        this.lines = lines;

        // Effect
        this.affinity = affinity;
        this.moodShifts = moodShifts;
        this.action = action;
    }

    // Accessible \\

    public String getReplyName() {
        return replyName;
    }

    public short getReplyID() {
        return replyID;
    }

    public int getSeed() {
        return seed;
    }

    public String getDescription() {
        return description;
    }

    public DialogueConditionStruct getCondition() {
        return condition;
    }

    public DialogueLeanStruct getLean() {
        return lean;
    }

    public DialogueLineStruct[] getLines() {
        return lines;
    }

    public float getAffinity() {
        return affinity;
    }

    public ValueMapStruct getMoodShifts() {
        return moodShifts;
    }

    public DialogueAction getAction() {
        return action;
    }
}
