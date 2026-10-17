package editor.characterview.util;

public enum CharacterViewRowKind {

    /*
     * The kinds of row a character tool's side panel lists. A header names
     * the section below it; an item is a selectable entry; a number steps
     * down and up and its value opens for typing; a choice steps back and
     * forth through its options; an action is a single button.
     */

    HEADER,
    ITEM,
    NUMBER,
    CHOICE,
    ACTION
}
