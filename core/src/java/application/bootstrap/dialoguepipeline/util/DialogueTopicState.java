package application.bootstrap.dialoguepipeline.util;

public enum DialogueTopicState {

    /*
     * Where what was said stands on a subject, which a condition may ask for
     * under "topic". ANY asks nothing, NONE holds when no topic was raised,
     * KNOWN when one was raised that the listener may speak of, and UNKNOWN
     * when one was raised that it may not.
     */

    ANY,
    NONE,
    KNOWN,
    UNKNOWN;

    // Values
    public static final DialogueTopicState[] VALUES = values();
}
