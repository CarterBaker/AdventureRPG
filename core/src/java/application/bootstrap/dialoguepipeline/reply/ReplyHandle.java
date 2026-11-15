package application.bootstrap.dialoguepipeline.reply;

import application.bootstrap.dialoguepipeline.util.DialogueAction;
import application.bootstrap.dialoguepipeline.util.DialogueConditionStruct;
import application.bootstrap.dialoguepipeline.util.DialogueLeanStruct;
import application.bootstrap.dialoguepipeline.util.DialogueLineStruct;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.HandlePackage;

public class ReplyHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded reply definition. Registered and owned by
     * ReplyManager. Delegates all accessors through ReplyData.
     */

    // Internal
    private ReplyData replyData;

    // Constructor \\

    public void constructor(ReplyData replyData) {

        // Internal
        this.replyData = replyData;
    }

    // Accessible \\

    public ReplyData getReplyData() {
        return replyData;
    }

    public String getReplyName() {
        return replyData.getReplyName();
    }

    public short getReplyID() {
        return replyData.getReplyID();
    }

    public int getSeed() {
        return replyData.getSeed();
    }

    public String getDescription() {
        return replyData.getDescription();
    }

    public DialogueConditionStruct getCondition() {
        return replyData.getCondition();
    }

    public DialogueLeanStruct getLean() {
        return replyData.getLean();
    }

    public DialogueLineStruct[] getLines() {
        return replyData.getLines();
    }

    public float getAffinity() {
        return replyData.getAffinity();
    }

    public ValueMapStruct getMoodShifts() {
        return replyData.getMoodShifts();
    }

    public DialogueAction getAction() {
        return replyData.getAction();
    }
}
