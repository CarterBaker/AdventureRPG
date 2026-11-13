package editor.runtime.recordings;

public class RecordingsSetting {

    /*
     * Constants used only by RecordingsContext — the row templates its panels
     * are filled from and their elements, its entry points, and every word
     * the tab shows.
     */

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/Recordings/recordings_header_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/Recordings/recordings_text_row";
    public static final String TEMPLATE_DETAIL_ROW = "editor/Recordings/recordings_detail_row";
    public static final String TEMPLATE_RECORDING_ROW = "editor/Recordings/recordings_recording_row";
    public static final String TEMPLATE_RECORDING_ROW_SELECTED = "editor/Recordings/recordings_recording_row_selected";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "recordings_row_label";
    public static final String ELEMENT_ROW_VALUE = "recordings_row_value";

    // Entry Points
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_RECORDINGS = 1;
    public static final int ENTRY_DETAILS = 2;
    public static final int ENTRY_LIST_TITLE = 3;

    // Text
    public static final String TEXT_NO_RECORDINGS = "No recordings yet: press New";
    public static final String TEXT_NO_RECORDING = "No recording open: click one to open it";
    public static final String TEXT_OPEN = "Open";
    public static final String TEXT_EMPTY_VALUE = "";
    public static final String TEXT_UNSAVED = "Unsaved changes";
    public static final String TEXT_SAVED = "Saved";
    public static final String TEXT_WORLD = "World";
    public static final String TEXT_TAKES = "Takes";
    public static final String TEXT_CLIPS = "Clips";
    public static final String TEXT_TRANSITIONS = "Transitions";
    public static final String TEXT_LENGTH = "Length";
    public static final String TEXT_STATE = "State";
    public static final String TEXT_OPEN_HINT = "Click a recording to open it in every Recorder tab";
    public static final String TEXT_EDIT_HINT = "Drag a recording onto the Timeline to nest it in the open one";

    // Formats
    public static final String FORMAT_LIST_TITLE = "Recordings (%d)";
    public static final String FORMAT_TIME = "%d:%05.2f";
    public static final int SECONDS_PER_MINUTE = 60;
}
