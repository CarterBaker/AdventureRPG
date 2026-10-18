package editor.bootstrap.charactereditorpipeline.util;

public enum CharacterEditorSelection {

    /*
     * How picked vertices combine with the selection: Replace makes them the
     * whole selection, Add and Subtract grow or shrink it by them, and Toggle
     * flips each one.
     */

    REPLACE,
    ADD,
    SUBTRACT,
    TOGGLE
}
