package application.bootstrap.dialoguepipeline.util;

public enum DialogueAction {

    /*
     * What a reply does in the world besides being said, named by a reply's
     * "action". NONE only speaks, LEAVE ends the talk, ATTACK turns the one
     * replying on whoever spoke, and FLEE sends it running from them.
     */

    NONE,
    LEAVE,
    ATTACK,
    FLEE;

    // Values
    public static final DialogueAction[] VALUES = values();
}
