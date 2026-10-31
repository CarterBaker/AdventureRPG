package editor.runtime.recorder;

public class RecorderSetting {

    /*
     * Constants used only by RecorderContext — the row, clip, node, timeline,
     * Keys and Assets templates its panels are filled from and their elements,
     * its entry points, the timeline's fixed steps, its ruler's ticks, the
     * sizes the timeline and the Keys tab draw at, how often the clock and
     * export progress refresh, and every word the tab shows.
     */

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/Recorder/recorder_header_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/Recorder/recorder_text_row";
    public static final String TEMPLATE_RECORDING_ROW = "editor/Recorder/recorder_recording_row";
    public static final String TEMPLATE_RECORDING_ROW_SELECTED = "editor/Recorder/recorder_recording_row_selected";
    public static final String TEMPLATE_SETTING_ROW = "editor/Recorder/recorder_setting_row";
    public static final String TEMPLATE_ACTION_ROW = "editor/Recorder/recorder_action_row";
    public static final String TEMPLATE_TAKE_ROW = "editor/Recorder/recorder_take_row";
    public static final String TEMPLATE_TAKE_ROW_SELECTED = "editor/Recorder/recorder_take_row_selected";
    public static final String TEMPLATE_CLIP_BAR = "editor/Recorder/recorder_clip_bar";
    public static final String TEMPLATE_CLIP_BAR_SELECTED = "editor/Recorder/recorder_clip_bar_selected";
    public static final String TEMPLATE_CLIP_BAR_MUTED = "editor/Recorder/recorder_clip_bar_muted";
    public static final String TEMPLATE_CLIP_BAR_SOUND = "editor/Recorder/recorder_clip_bar_sound";
    public static final String TEMPLATE_RULER_TICK = "editor/Recorder/recorder_ruler_tick";
    public static final String TEMPLATE_NODE_MOVEMENT = "editor/Recorder/recorder_node_movement";
    public static final String TEMPLATE_NODE_MOVEMENT_SELECTED = "editor/Recorder/recorder_node_movement_selected";
    public static final String TEMPLATE_NODE_GESTURE = "editor/Recorder/recorder_node_gesture";
    public static final String TEMPLATE_NODE_GESTURE_SELECTED = "editor/Recorder/recorder_node_gesture_selected";
    public static final String TEMPLATE_KEY_TRACK_ROW = "editor/Recorder/recorder_key_track_row";
    public static final String TEMPLATE_KEY_CURVE_ROW = "editor/Recorder/recorder_key_curve_row";
    public static final String TEMPLATE_KEY_CURVE_ROW_SELECTED = "editor/Recorder/recorder_key_curve_row_selected";
    public static final String TEMPLATE_KEY_NODE_ROW = "editor/Recorder/recorder_key_node_row";
    public static final String TEMPLATE_KEY_DIAMOND = "editor/Recorder/recorder_key_diamond";
    public static final String TEMPLATE_KEY_DIAMOND_SELECTED = "editor/Recorder/recorder_key_diamond_selected";
    public static final String TEMPLATE_KEY_SUMMARY = "editor/Recorder/recorder_key_summary";
    public static final String TEMPLATE_KEY_MOVEMENT = "editor/Recorder/recorder_key_movement";
    public static final String TEMPLATE_KEY_MOVEMENT_SELECTED = "editor/Recorder/recorder_key_movement_selected";
    public static final String TEMPLATE_KEY_GESTURE = "editor/Recorder/recorder_key_gesture";
    public static final String TEMPLATE_KEY_GESTURE_SELECTED = "editor/Recorder/recorder_key_gesture_selected";
    public static final String TEMPLATE_GRAPH_SEGMENT = "editor/Recorder/recorder_graph_segment";
    public static final String TEMPLATE_GRAPH_HANDLE_LINE = "editor/Recorder/recorder_graph_handle_line";
    public static final String TEMPLATE_GRAPH_KEY = "editor/Recorder/recorder_graph_key";
    public static final String TEMPLATE_GRAPH_KEY_SELECTED = "editor/Recorder/recorder_graph_key_selected";
    public static final String TEMPLATE_GRAPH_HANDLE = "editor/Recorder/recorder_graph_handle";
    public static final String TEMPLATE_GRAPH_BOX = "editor/Recorder/recorder_graph_box";
    public static final String TEMPLATE_ASSET_SECTION_ROW = "editor/Recorder/recorder_asset_section_row";
    public static final String TEMPLATE_ASSET_ROW = "editor/Recorder/recorder_asset_row";
    public static final String TEMPLATE_ASSET_ROW_MISSING = "editor/Recorder/recorder_asset_row_missing";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "recorder_row_label";
    public static final String ELEMENT_ROW_VALUE = "recorder_row_value";
    public static final String ELEMENT_ROW_SELECT = "recorder_row_select";
    public static final String ELEMENT_ROW_PLACE = "recorder_row_place";
    public static final String ELEMENT_ROW_RENAME = "recorder_row_rename";
    public static final String ELEMENT_ROW_DELETE = "recorder_row_delete";
    public static final String ELEMENT_CLIP_LABEL = "recorder_clip_label";
    public static final String ELEMENT_NODE_LABEL = "recorder_node_label";
    public static final String ELEMENT_RULER_LABEL = "recorder_ruler_label";
    public static final String ELEMENT_KEY_NAME = "recorder_key_name";
    public static final String ELEMENT_KEY_STRIP = "recorder_key_strip";
    public static final String ELEMENT_GRAPH_LINES = "recorder_key_graph_lines";
    public static final String ELEMENT_GRAPH_POINTS = "recorder_key_graph_points";

    // Entry Points — the clip lanes, the ruler, the Keys rows and the Keys graph are EditorSetting
    // RECORDER_ENTRY_CLIP_LANES, RECORDER_ENTRY_RULER, RECORDER_ENTRY_KEY_ROWS and RECORDER_ENTRY_KEY_GRAPH, which the
    // drags and the wheel share
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_CLOCK = 1;
    public static final int ENTRY_RECORDINGS = 2;
    public static final int ENTRY_SETTINGS = 3;
    public static final int ENTRY_TAKES = 4;
    public static final int ENTRY_PLAYHEAD = 7;
    public static final int ENTRY_RANGE_START = 8;
    public static final int ENTRY_RANGE_END = 9;
    public static final int ENTRY_CLIP = 10;
    public static final int ENTRY_DRAG_GHOST = 11;
    public static final int ENTRY_LANE_PLAYHEAD = 12;
    public static final int ENTRY_VIEW = 13;
    public static final int ENTRY_KEYS_PANEL = 14;
    public static final int ENTRY_KEYS_TITLE = 15;
    public static final int ENTRY_GRAPH_MARKS = 18;
    public static final int ENTRY_KEYS_PLAYHEAD = 19;
    public static final int ENTRY_GRAPH_TOP = 20;
    public static final int ENTRY_GRAPH_BOTTOM = 21;
    public static final int ENTRY_KEY_INSPECTOR = 22;
    public static final int ENTRY_TAKES_PANEL = 23;
    public static final int ENTRY_CLIP_PANEL = 24;
    public static final int ENTRY_ASSETS_PANEL = 25;
    public static final int ENTRY_ASSETS_TITLE = 26;
    public static final int ENTRY_ASSET_ROWS = 27;

    // Timeline — positions across the view as a percentage of it; a drag's ghost may reach a view's width past
    // either side, where the lanes' mask hides it
    public static final int TIMELINE_STEPS = 1000;
    public static final int GHOST_OVERHANG_STEPS = TIMELINE_STEPS;
    public static final float TIMELINE_PERCENT = 100f;
    public static final float CLIP_BAR_PIXELS = 30f;
    public static final float EDGE_GHOST_WIDTH_PIXELS = 2f;
    public static final float NODE_MIN_PIXELS = 2f;
    public static final float PLAYHEAD_WIDTH_PIXELS = 2f;
    public static final float PLAYHEAD_HEIGHT_PIXELS = 30f;
    public static final float RANGE_MARKER_WIDTH_PIXELS = 2f;
    public static final float RANGE_MARKER_HEIGHT_PIXELS = 30f;

    // Keys — a panel set aside is moved this far off the window; a key's diamond is a square turned on its corner,
    // the graph's line is sampled every few pixels, its lines are drawn this thick, and keys closer than this on
    // it are drawn once
    public static final float HIDDEN_PANEL_PIXELS = -100000f;
    public static final float KEY_DIAMOND_DEGREES = 45f;
    public static final float KEY_NODE_PIXELS = 16f;
    public static final float GRAPH_SEGMENT_PIXELS = 4f;
    public static final float GRAPH_LINE_PIXELS = 2f;
    public static final float GRAPH_HANDLE_LINE_PIXELS = 1f;
    public static final float GRAPH_BOX_LINE_PIXELS = 1f;
    public static final float GRAPH_KEY_SPACING_PIXELS = 3f;

    // Ruler — the spacing of its ticks in seconds, the closest at least this many pixels apart that fits
    public static final float[] RULER_INTERVAL_SECONDS = {
            0.1f, 0.25f, 0.5f, 1f, 2f, 5f, 10f, 15f, 30f, 60f, 120f, 300f, 600f, 1200f, 3600f };
    public static final float RULER_MIN_TICK_PIXELS = 72f;
    public static final int RULER_FALLBACK_TICKS = 8;

    // Refresh
    public static final float REFRESH_SECONDS = 0.1f;
    public static final int SECONDS_PER_MINUTE = 60;

    // Text — rows
    public static final String TEXT_EMPTY = "";
    public static final String TEXT_NO_RECORDINGS = "No recordings yet: press Record or New";
    public static final String TEXT_NO_TAKES = "No takes yet: arm channels and press Record";
    public static final String TEXT_NO_RECORDING = "No recording open";
    public static final String TEXT_NO_CLIP = "Click a clip on the timeline to edit it";
    public static final String TEXT_UNSAVED_SUFFIX = " *";
    public static final String TEXT_CHANNEL_SEPARATOR = " ";

    // Text — settings and clip
    public static final String TEXT_HEADER_RECORD = "Record";
    public static final String TEXT_HEADER_EXPORT = "Export";
    public static final String TEXT_HEADER_PLAYS = "Plays";
    public static final String TEXT_HEADER_EDIT = "Edit at the playhead";
    public static final String TEXT_ARMED = "Armed";
    public static final String TEXT_OFF = "Off";
    public static final String TEXT_ON = "On";
    public static final String TEXT_MUTE = "Muted";
    public static final String TEXT_TIME_RAMP = "Time ramp";
    public static final String TEXT_RAMP_CUT = "Cut";
    public static final String TEXT_TRIM_START = "Trim start to playhead";
    public static final String TEXT_TRIM_END = "Trim end to playhead";
    public static final String TEXT_SPLIT = "Cut at playhead";
    public static final String TEXT_STAMP = "Stamp a copy at playhead";
    public static final String TEXT_REMOVE_CLIP = "Remove from timeline";
    public static final String TEXT_HEADER_NODE = "Node";
    public static final String TEXT_NODE_DOING = "Doing";
    public static final String TEXT_NODE_PREVIOUS = "Previous movement";
    public static final String TEXT_REMOVE_MOVEMENT = "Remove: carry on as before";
    public static final String TEXT_REMOVE_GESTURE = "Remove: hands free";
    public static final String TEXT_NODE_MOVE_HINT = "Drag its notch on the clip to retime it";
    public static final String TEXT_GESTURE_MOVE_HINT = "Drag it along the clip to move it";
    public static final String TEXT_NODE_SHARED = "Node edits change the take, and every clip of it";
    public static final String TEXT_VIEW_FIT = "Whole timeline";

    // Text — Keys
    public static final String TEXT_KEYS_TITLE = "Keys";
    public static final String TEXT_KEYS_NO_CLIP = "Click a clip on the timeline to see its keys";
    public static final String TEXT_KEYS_NO_TRACKS = "This take recorded nothing a curve can carry";
    public static final String TEXT_TRACK_OPENED = "-  ";
    public static final String TEXT_TRACK_CLOSED = "+  ";
    public static final String TEXT_ROW_MOVEMENT = "Movement";
    public static final String TEXT_ROW_GESTURES = "Gestures";
    public static final String TEXT_GRAPH_EMPTY = "Click a value in the list to see its curve";
    public static final String TEXT_HEADER_KEYS = "Keys";
    public static final String TEXT_HEADER_CURVE = "Curve";
    public static final String TEXT_KEYS_HINT_OPEN = "Click a name to open or close it";
    public static final String TEXT_KEYS_HINT_SELECT = "Click a value to see its curve on the graph below";
    public static final String TEXT_KEYS_HINT_VIEW = "Wheel zooms, Shift+wheel or dragging empty space scrolls";
    public static final String TEXT_KEYS_HINT_VALUES = "Ctrl+wheel on the graph zooms its values";
    public static final String TEXT_KEYS_HINT_KEY =
            "Click a key to edit it: Shift adds, Ctrl removes, and dragging on nothing with either boxes keys";
    public static final String TEXT_CURVE_UNLAID = "Its keys are laid once nothing records or exports";
    public static final String TEXT_KEY_ADD = "Add a key at the playhead";
    public static final String TEXT_KEY_VALUE = "Value";
    public static final String TEXT_KEY_MODE = "Handles";
    public static final String TEXT_KEY_DELETE = "Delete key";
    public static final String TEXT_CURVE_REFIT = "Lay again from the recording";
    public static final String TEXT_TAKE_REFIT = "Lay every value of the take again";
    public static final String TEXT_CURVE_SHARED = "Curves belong to the take, and every clip of it";
    public static final String TEXT_HEADER_SOUNDS = "Sounds";
    public static final String TEXT_SOUND_VOLUME = "Volume";
    public static final String TEXT_SOUND_PITCH = "Pitch";
    public static final String TEXT_SOUND_LENGTH = "Length";
    public static final String TEXT_SOUND_PLACED = "Placed in the world, heard from where it was";
    public static final String TEXT_SOUND_UNPLACED = "Heard as it is, wherever the camera stands";
    public static final String TEXT_SOUND_LOOPS = "Loops for as long as it runs";
    public static final String TEXT_SOUND_MISSING = "The game no longer has this sound: it plays silent";
    public static final String TEXT_SOUND_SEEK = "Move the playhead to it";
    public static final String TEXT_SOUND_COPY = "Copy it to the playhead";
    public static final String TEXT_SOUND_REPLACE = "Play the Sound Library's selection instead";
    public static final String TEXT_SOUND_REMOVE = "Remove sound";
    public static final String TEXT_SOUND_REMOVE_ALL = "Remove every one of this sound";
    public static final String TEXT_SOUND_MOVE_HINT = "Drag it along its row to retime it";
    public static final String TEXT_SOUND_SHARED = "Sound edits change the take, and every clip of it";
    public static final String TEXT_SOUNDS_ADD = "Add the Sound Library's selection at the playhead";
    public static final String TEXT_SOUNDS_HINT_SELECT = "Click a sound on the Sounds layer to change or remove it";
    public static final String TEXT_SOUNDS_HINT_DROP = "Drop a sound from the Sound Library onto the rows to add it";
    public static final String TEXT_RESOLUTION = "Resolution";
    public static final String TEXT_FRAME_RATE = "Frame rate";
    public static final String TEXT_FORMAT = "Format";
    public static final String TEXT_MOTION_BLUR = "Motion blur";
    public static final String TEXT_SUPERSAMPLE = "Supersample";
    public static final String TEXT_RANGE_START = "Range start";
    public static final String TEXT_RANGE_END = "Range end";
    public static final String TEXT_RANGE_FROM_START = "Recording start";
    public static final String TEXT_RANGE_TO_END = "Recording end";
    public static final String TEXT_CLEAR_RANGE = "Clear range";
    public static final String TEXT_EXPORT = "Export video";

    // Text — Assets
    public static final String TEXT_ASSETS_TITLE = "Assets";
    public static final String TEXT_ASSET_MISSING = "Missing";
    public static final String TEXT_CANCEL_EXPORT = "Cancel export";

    // Formats
    public static final String FORMAT_TIME = "%d:%05.2f";
    public static final String FORMAT_CLOCK = "%s   %s / %s";
    public static final String FORMAT_RESOLUTION = "%d x %d";
    public static final String FORMAT_FRAME_RATE = "%d fps";
    public static final String FORMAT_MOTION_BLUR = "%d samples";
    public static final String FORMAT_RAMP = "%.0fs";
    public static final String FORMAT_TAKE = "%s   %s   %s";
    public static final String FORMAT_TAKE_PLACED = "%s   %s   %s   x%d";
    public static final String FORMAT_CLIP_PLACE = "Lane %d   %s to %s";
    public static final String FORMAT_CLIP_SOURCE = "Plays %s to %s of the take";
    public static final String FORMAT_NODE = "%s   %s to %s";
    public static final String FORMAT_VIEW = "%s to %s";
    public static final String FORMAT_NODE_HEADER = "%s: %s";
    public static final String FORMAT_KEYS_TITLE = "Keys: %s";
    public static final String FORMAT_CURVE_HEADER = "%s: %s";
    public static final String FORMAT_CURVE_NOW = "Now %s";
    public static final String FORMAT_CURVE_KEYS = "%d keys";
    public static final String FORMAT_KEYS_SELECTED = "%d keys selected";
    public static final String FORMAT_KEY_HEADER = "Key at %s";
    public static final String FORMAT_SOUND_LAYER = "Sounds (%d)";
    public static final String FORMAT_SOUND_ROW = "%s (%d)";
    public static final String FORMAT_SOUND_HEADER = "Sound: %s";
    public static final String FORMAT_SOUND_SPAN = "%s to %s";
    public static final String FORMAT_SOUND_PERCENT = "%.0f%%";
    public static final String FORMAT_SOUND_SECONDS = "%.2fs";
    public static final String FORMAT_ASSETS_TITLE = "Assets: %s";
    public static final String FORMAT_ASSET_OWNERS = "%s and %d more";
    public static final String FORMAT_PROGRESS = "Frame %d of %d";
}
