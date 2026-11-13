package editor.bootstrap.recorderpipeline.pianoroll;

public enum PianoRollDragKind {

    /*
     * What a press on the Piano Roll's grid has taken hold of: nothing, the
     * notes selected being moved in time and pitch, their ends being dragged
     * to lengthen or shorten them, a box drawn across empty grid to select
     * the notes within it, a new note being drawn out from where it was
     * pressed, or the Erase tool sweeping away every note it passes over.
     */

    NONE,
    MOVE,
    RESIZE,
    BOX,
    DRAW,
    ERASE;

    // Values
    public static final PianoRollDragKind[] VALUES = values();
}
