package application.bootstrap.dialoguepipeline.util;

public enum DialogueValue {

    /*
     * What a line may take from the talk itself, written in angle brackets.
     * PLAYER is the name of whoever spoke, NAME the name of the one replying,
     * TITLE the title its profession gives it, and SUBJECT the dictionary
     * word that raised the topic.
     */

    PLAYER,
    NAME,
    TITLE,
    SUBJECT;

    // Values
    public static final DialogueValue[] VALUES = values();
}
