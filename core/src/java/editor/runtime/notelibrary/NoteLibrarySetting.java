package editor.runtime.notelibrary;

public class NoteLibrarySetting {

    /*
     * Constants used only by NoteLibraryContext — the row templates its
     * panels are filled from and their elements, its entry points, and every
     * word the tab shows.
     */

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/NoteLibrary/note_library_header_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/NoteLibrary/note_library_text_row";
    public static final String TEMPLATE_DETAIL_ROW = "editor/NoteLibrary/note_library_detail_row";
    public static final String TEMPLATE_NOTE_ROW = "editor/NoteLibrary/note_library_note_row";
    public static final String TEMPLATE_NOTE_ROW_SELECTED = "editor/NoteLibrary/note_library_note_row_selected";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "note_library_row_label";
    public static final String ELEMENT_ROW_VALUE = "note_library_row_value";
    public static final String ELEMENT_ROW_SELECT = "note_library_row_select";
    public static final String ELEMENT_ROW_PLAY = "note_library_row_play";

    // Entry Points
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_NOTES = 1;
    public static final int ENTRY_DETAILS = 2;
    public static final int ENTRY_LIST_TITLE = 3;

    // Notes — every octave headed by the C it starts on
    public static final int NOTES_PER_OCTAVE = 12;

    // Text
    public static final String TEXT_LIST_TITLE = "Notes";
    public static final String TEXT_INSTRUMENT = "Instrument";
    public static final String TEXT_NOTE_SET = "Note set";
    public static final String TEXT_RANGE = "Range";
    public static final String TEXT_POLYPHONY = "Notes at once";
    public static final String TEXT_PERCUSSION = "Percussion";
    public static final String TEXT_YES = "Yes";
    public static final String TEXT_NO = "No";
    public static final String TEXT_DRAG_HINT =
            "Drag a note onto the Timeline to lay it, then drag its end to hold it longer";
    public static final String TEXT_CHORD_HINT = "Notes played together go on lanes of their own";

    // Formats
    public static final String FORMAT_LIST_TITLE = "Notes of %s (%d)";
    public static final String FORMAT_OCTAVE = "Octave %d";
    public static final String FORMAT_RANGE = "%s to %s";
}
