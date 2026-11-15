package application.bootstrap.dialoguepipeline.util;

public enum DialogueSegment {

    /*
     * The kinds of piece a line is cut into. LITERAL is text said as written,
     * PHRASE the name of a phrase in braces, expanded in its place, TOPIC the
     * reserved phrase that expands the lines of the subject raised, WORD a
     * list of tags in square brackets, filled by a dictionary word carrying
     * all of them, and VALUE a name in angle brackets, filled from the talk.
     */

    LITERAL,
    PHRASE,
    TOPIC,
    WORD,
    VALUE;
}
