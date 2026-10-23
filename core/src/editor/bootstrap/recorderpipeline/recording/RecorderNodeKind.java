package editor.bootstrap.recorderpipeline.recording;

public enum RecorderNodeKind {

    /*
     * What a node on a clip shows of the player: a movement, the movement
     * state it is in from one change to the next, drawn along the lower row
     * of the clip; or a gesture, one action of its hands from its first
     * sample to its last, drawn along the upper row beside the clip's name.
     */

    MOVEMENT,
    GESTURE;
}
