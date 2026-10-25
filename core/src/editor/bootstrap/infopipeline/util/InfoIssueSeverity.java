package editor.bootstrap.infopipeline.util;

public enum InfoIssueSeverity {

    /*
     * How much an issue matters to the game. An ERROR is something the game
     * would refuse to load, so it is drawn in red and stops the file being
     * saved; IGNORED is data the game never reads, so it is drawn muted, kept
     * exactly as it is, and never stops a save.
     */

    ERROR,
    IGNORED
}
