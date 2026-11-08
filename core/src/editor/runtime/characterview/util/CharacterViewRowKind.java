package editor.runtime.characterview.util;

public enum CharacterViewRowKind {

    /*
     * The kinds of row a character tool's side panel lists. A header names
     * the section below it; an item is a selectable entry; a number steps
     * down and up and its value opens for typing; a text is a value that
     * only opens for typing, such as a name; a choice steps back and forth
     * through its options; an action is a single button; a pair is two
     * values side by side, each only opening for typing, such as the two
     * ends of a range.
     */

    HEADER,
    ITEM,
    NUMBER,
    TEXT,
    CHOICE,
    ACTION,
    PAIR
}
