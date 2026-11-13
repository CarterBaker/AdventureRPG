package editor.runtime.pianoroll;

public class PianoRollSetting {

    /*
     * Constants used only by PianoRollContext — the templates its grid,
     * keyboard and ruler are filled from and their elements, its menu's entry
     * points, the height of a pitch's row, the fixed steps positions across
     * the grid are drawn at, how dense the grid's lines may grow, and how
     * often a resized tab is measured again.
     */

    // Templates
    public static final String TEMPLATE_KEY_WHITE = "editor/PianoRoll/piano_roll_key_white";
    public static final String TEMPLATE_KEY_BLACK = "editor/PianoRoll/piano_roll_key_black";
    public static final String TEMPLATE_ROW_WHITE = "editor/PianoRoll/piano_roll_row_white";
    public static final String TEMPLATE_ROW_BLACK = "editor/PianoRoll/piano_roll_row_black";
    public static final String TEMPLATE_LINE_BAR = "editor/PianoRoll/piano_roll_line_bar";
    public static final String TEMPLATE_LINE_BEAT = "editor/PianoRoll/piano_roll_line_beat";
    public static final String TEMPLATE_NOTE = "editor/PianoRoll/piano_roll_note";
    public static final String TEMPLATE_NOTE_SELECTED = "editor/PianoRoll/piano_roll_note_selected";
    public static final String TEMPLATE_NOTE_GHOST = "editor/PianoRoll/piano_roll_note_ghost";
    public static final String TEMPLATE_CLIP_SPAN = "editor/PianoRoll/piano_roll_clip_span";
    public static final String TEMPLATE_RULER_TICK = "editor/Recorder/recorder_ruler_tick";

    // Elements
    public static final String ELEMENT_KEY_LABEL = "piano_roll_key_label";
    public static final String ELEMENT_RULER_LABEL = "recorder_ruler_label";

    // Entry Points
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_TITLE = 1;
    public static final int ENTRY_KEYS = 2;
    public static final int ENTRY_GRID = 3;
    public static final int ENTRY_RULER = 4;
    public static final int ENTRY_PLAYHEAD = 5;
    public static final int ENTRY_BOX = 6;
    public static final int ENTRY_TYPING = 7;
    public static final int ENTRY_GRID_ROWS = 8;
    public static final int ENTRY_GRID_LINES = 9;
    public static final int ENTRY_GRID_NOTES = 10;
    public static final int ENTRY_RULER_TRACK = 11;
    public static final int ENTRY_RULER_PLAYHEAD = 12;

    // Grid — a pitch's row, the steps across it, the fewest pixels between beat lines before only bars are drawn, the
    // fewest between ruler labels, and the most rows a tab may hold
    public static final float ROW_PIXELS = 13f;
    public static final float NOTE_PIXELS = 11f;
    public static final int GRID_STEPS = 1000;
    public static final float GRID_PERCENT = 100f;
    public static final float BEAT_LINE_MIN_PIXELS = 8f;
    public static final float RULER_MIN_PIXELS = 48f;
    public static final int ROWS_MAX = 128;
    public static final float LINE_PIXELS = 1f;
    public static final float PLAYHEAD_PIXELS = 2f;
    public static final float RULER_PLAYHEAD_PIXELS = 30f;

    // Refresh
    public static final float REFRESH_SECONDS = 0.25f;

    // Text
    public static final String FORMAT_TITLE = "Piano Roll: %s on %s";
    public static final String TEXT_TITLE = "Piano Roll";
    public static final String TEXT_EMPTY = "";
    public static final String FORMAT_BAR = "%d";
}
