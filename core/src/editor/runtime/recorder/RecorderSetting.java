package editor.runtime.recorder;

public class RecorderSetting {

    /*
     * Constants used only by RecorderContext — the row, clip and timeline
     * templates its panels are filled from and their elements, its entry
     * points, the timeline's fixed steps and lanes, how often the clock and
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
    public static final String TEMPLATE_MARKER_ROW = "editor/Recorder/recorder_marker_row";
    public static final String TEMPLATE_MARKER_ROW_SELECTED = "editor/Recorder/recorder_marker_row_selected";
    public static final String TEMPLATE_CLIP_BAR = "editor/Recorder/recorder_clip_bar";
    public static final String TEMPLATE_CLIP_BAR_SELECTED = "editor/Recorder/recorder_clip_bar_selected";
    public static final String TEMPLATE_CLIP_BAR_MUTED = "editor/Recorder/recorder_clip_bar_muted";
    public static final String TEMPLATE_MARKER_TICK = "editor/Recorder/recorder_marker_tick";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "recorder_row_label";
    public static final String ELEMENT_ROW_VALUE = "recorder_row_value";
    public static final String ELEMENT_ROW_SELECT = "recorder_row_select";
    public static final String ELEMENT_ROW_PLACE = "recorder_row_place";
    public static final String ELEMENT_ROW_RENAME = "recorder_row_rename";
    public static final String ELEMENT_ROW_DELETE = "recorder_row_delete";
    public static final String ELEMENT_CLIP_LABEL = "recorder_clip_label";

    // Entry Points — the clip lanes are EditorSetting.RECORDER_ENTRY_CLIP_LANES, which the drags share
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_CLOCK = 1;
    public static final int ENTRY_RECORDINGS = 2;
    public static final int ENTRY_SETTINGS = 3;
    public static final int ENTRY_TAKES = 4;
    public static final int ENTRY_MARKERS = 5;
    public static final int ENTRY_MARKER_TICKS = 7;
    public static final int ENTRY_PLAYHEAD = 8;
    public static final int ENTRY_RANGE_START = 9;
    public static final int ENTRY_RANGE_END = 10;
    public static final int ENTRY_CLIP = 11;
    public static final int ENTRY_DRAG_GHOST = 12;
    public static final int ENTRY_LANE_PLAYHEAD = 13;

    // Timeline — positions along the track as a percentage of it, and the lanes clips are laid on, highest on top
    public static final int TIMELINE_STEPS = 1000;
    public static final float TIMELINE_PERCENT = 100f;
    public static final float LANE_PIXELS = 22f;
    public static final float CLIP_BAR_PIXELS = 20f;
    public static final float CLIP_BAR_INSET_PIXELS = 1f;
    public static final float RANGE_MARKER_WIDTH_PIXELS = 2f;
    public static final float RANGE_MARKER_HEIGHT_PIXELS = 30f;

    // Refresh
    public static final float REFRESH_SECONDS = 0.1f;
    public static final int SECONDS_PER_MINUTE = 60;

    // Text — rows
    public static final String TEXT_EMPTY = "";
    public static final String TEXT_NO_RECORDINGS = "No recordings yet: press Record or New";
    public static final String TEXT_NO_TAKES = "No takes yet: arm channels and press Record";
    public static final String TEXT_NO_MARKERS = "No markers: press Mark to bookmark the playhead";
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
    public static final String TEXT_SPLIT = "Split at playhead";
    public static final String TEXT_STAMP = "Stamp a copy at playhead";
    public static final String TEXT_REMOVE_CLIP = "Remove from timeline";
    public static final String TEXT_RESOLUTION = "Resolution";
    public static final String TEXT_FRAME_RATE = "Frame rate";
    public static final String TEXT_FORMAT = "Format";
    public static final String TEXT_MOTION_BLUR = "Motion blur";
    public static final String TEXT_SUPERSAMPLE = "Supersample";
    public static final String TEXT_HUD = "Interface";
    public static final String TEXT_RANGE_START = "Range start";
    public static final String TEXT_RANGE_END = "Range end";
    public static final String TEXT_RANGE_FROM_START = "Recording start";
    public static final String TEXT_RANGE_TO_END = "Recording end";
    public static final String TEXT_CLEAR_RANGE = "Clear range";
    public static final String TEXT_EXPORT = "Export video";
    public static final String TEXT_CANCEL_EXPORT = "Cancel export";
    public static final String TEXT_FFMPEG_FOUND = "ffmpeg found: every format is open";
    public static final String TEXT_FFMPEG_MISSING = "No ffmpeg: AVI only, MP4 and MOV need it";

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
    public static final String FORMAT_MARKER = "%s   %s";
    public static final String FORMAT_PROGRESS = "Frame %d of %d";
}
