package editor.runtime.recordings;

public class RecordingsSetting {

    /*
     * Constants used only by RecordingsContext, the Tracks tab — the row
     * templates its panels are filled from and their elements, its entry
     * points, and every word the tab shows for media files and for the
     * animations of the character chosen.
     */

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/Recordings/recordings_header_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/Recordings/recordings_text_row";
    public static final String TEMPLATE_DETAIL_ROW = "editor/Recordings/recordings_detail_row";
    public static final String TEMPLATE_RECORDING_ROW = "editor/Recordings/recordings_recording_row";
    public static final String TEMPLATE_RECORDING_ROW_SELECTED = "editor/Recordings/recordings_recording_row_selected";
    public static final String TEMPLATE_ANIMATION_ROW = "editor/Recordings/recordings_animation_row";
    public static final String TEMPLATE_ANIMATION_ROW_SELECTED = "editor/Recordings/recordings_animation_row_selected";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "recordings_row_label";
    public static final String ELEMENT_ROW_VALUE = "recordings_row_value";

    // Entry Points
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_RECORDINGS = 1;
    public static final int ENTRY_DETAILS = 2;
    public static final int ENTRY_LIST_TITLE = 3;

    // Text
    public static final String TEXT_NO_RECORDINGS = "No media yet: press New, or Record in the Recorder";
    public static final String TEXT_KIND = "Exports as";
    public static final String TEXT_KIND_VIDEO = "Video (it shows a picture)";
    public static final String TEXT_KIND_MUSIC = "Music (song and sound)";
    public static final String TEXT_NO_RECORDING = "No media file open: click one to open it";
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
    public static final String TEXT_OPEN_HINT = "Click a media file to open it in every Recorder tab";
    public static final String TEXT_EDIT_HINT = "Drag a media file onto the Timeline to lay it in the open one";
    public static final String TEXT_NO_ANIMATIONS =
            "No animations for this character yet: File, New in the Animation Editor";
    public static final String TEXT_NO_CHARACTER = "No character chosen: pick one in the Animation Editor's rows";
    public static final String TEXT_NO_ANIMATION = "No animation open: click one to open it on the Timeline";
    public static final String TEXT_CHARACTER = "Character";
    public static final String TEXT_RIG = "Rig";
    public static final String TEXT_BONES = "Bones keyed";
    public static final String TEXT_ANIMATION_OPEN_HINT =
            "Click an animation to open it on the Timeline and in the Animation Editor";
    public static final String TEXT_ANIMATION_EDIT_HINT =
            "Its bones are the Layers tab's; key them on the Keys and Keys Graph tabs";

    // Formats
    public static final String FORMAT_LIST_TITLE = "Media (%d)";
    public static final String FORMAT_ANIMATION_LIST_TITLE = "Animations (%d)";
    public static final String FORMAT_TIME = "%d:%05.2f";
    public static final int SECONDS_PER_MINUTE = 60;
}
