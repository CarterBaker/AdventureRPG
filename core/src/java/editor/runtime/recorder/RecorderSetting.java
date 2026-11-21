package editor.runtime.recorder;

public class RecorderSetting {

    /*
     * Constants used only by RecorderContext and the Recorder tabs beside it
     * — Timeline, Keys and Keys Graph, which share its systems — the row,
     * clip, transition, node, timeline, Keys and graph templates their panels
     * are filled from and their elements, each menu's entry points, the
     * timeline's fixed steps, its ruler's ticks, the sizes the timeline, the
     * Keys and the graph draw at, how often the clock refreshes, and every
     * word the tabs show.
     */

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/Recorder/recorder_header_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/Recorder/recorder_text_row";
    public static final String TEMPLATE_CLIP_BAR = "editor/Recorder/recorder_clip_bar";
    public static final String TEMPLATE_CLIP_BAR_SELECTED = "editor/Recorder/recorder_clip_bar_selected";
    public static final String TEMPLATE_CLIP_BAR_MUTED = "editor/Recorder/recorder_clip_bar_muted";
    public static final String TEMPLATE_CLIP_BAR_SOUND = "editor/Recorder/recorder_clip_bar_sound";
    public static final String TEMPLATE_TRANSITION_MARK = "editor/Recorder/recorder_transition_mark";
    public static final String TEMPLATE_TRANSITION_MARK_SELECTED = "editor/Recorder/recorder_transition_mark_selected";
    public static final String TEMPLATE_RULER_TICK = "editor/Recorder/recorder_ruler_tick";
    public static final String TEMPLATE_NODE_MOVEMENT = "editor/Recorder/recorder_node_movement";
    public static final String TEMPLATE_NODE_MOVEMENT_SELECTED = "editor/Recorder/recorder_node_movement_selected";
    public static final String TEMPLATE_NODE_GESTURE = "editor/Recorder/recorder_node_gesture";
    public static final String TEMPLATE_NODE_GESTURE_SELECTED = "editor/Recorder/recorder_node_gesture_selected";
    public static final String TEMPLATE_KEY_CURVE_ROW = "editor/Recorder/recorder_key_curve_row";
    public static final String TEMPLATE_KEY_CURVE_ROW_SELECTED = "editor/Recorder/recorder_key_curve_row_selected";
    public static final String TEMPLATE_KEY_NODE_ROW = "editor/Recorder/recorder_key_node_row";
    public static final String TEMPLATE_KEY_DIAMOND = "editor/Recorder/recorder_key_diamond";
    public static final String TEMPLATE_KEY_DIAMOND_SELECTED = "editor/Recorder/recorder_key_diamond_selected";
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
    public static final String TEMPLATE_LANE_STRIPE = "editor/Timeline/timeline_lane_stripe";
    public static final String TEMPLATE_LANE_STRIPE_ALTERNATE = "editor/Timeline/timeline_lane_stripe_alternate";
    public static final String TEMPLATE_MIXER_ROW = "editor/Timeline/timeline_mixer_row";
    public static final String TEMPLATE_MIXER_ROW_SELECTED = "editor/Timeline/timeline_mixer_row_selected";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "recorder_row_label";
    public static final String ELEMENT_CLIP_LABEL = "recorder_clip_label";
    public static final String ELEMENT_TRANSITION_LABEL = "recorder_transition_label";
    public static final String ELEMENT_NODE_LABEL = "recorder_node_label";
    public static final String ELEMENT_RULER_LABEL = "recorder_ruler_label";
    public static final String ELEMENT_KEY_NAME = "recorder_key_name";
    public static final String ELEMENT_KEY_STRIP = "recorder_key_strip";
    public static final String ELEMENT_GRAPH_LINES = "recorder_key_graph_lines";
    public static final String ELEMENT_GRAPH_POINTS = "recorder_key_graph_points";
    public static final String ELEMENT_MIXER_NAME = "timeline_mixer_name";
    public static final String ELEMENT_MIXER_RENAME = "timeline_mixer_rename";

    // Entry Points — the Timeline's menu; the clip lanes and the ruler are EditorSetting RECORDER_ENTRY_CLIP_LANES
    // and RECORDER_ENTRY_RULER, which the drags and the wheel share
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_TIMELINE_TITLE = 1;
    public static final int ENTRY_PLAYHEAD = 4;
    public static final int ENTRY_RANGE_START = 5;
    public static final int ENTRY_RANGE_END = 6;
    public static final int ENTRY_DRAG_GHOST = 7;
    public static final int ENTRY_LANE_PLAYHEAD = 8;
    public static final int ENTRY_VIEW = 9;
    public static final int ENTRY_LANE_STRIPES = 10;
    public static final int ENTRY_MIXER_ROWS = 11;
    public static final int ENTRY_TIMELINE_TRACK = 12;
    public static final int ENTRY_LANE_AREA = 13;

    // Entry Points — the transport's menu
    public static final int ENTRY_TRANSPORT_STATUS = 0;
    public static final int ENTRY_TRANSPORT_CLOCK = 1;
    public static final int ENTRY_TRANSPORT_LOOP_ICON = 2;
    public static final int ENTRY_TRANSPORT_RECORD_ICON = 3;

    // Entry Points — the Keys' menu; its rows and the span their strips share are EditorSetting
    // RECORDER_ENTRY_KEY_ROWS and RECORDER_ENTRY_KEY_STRIPS, which the drags and the wheel share
    public static final int ENTRY_KEYS_TITLE = 0;
    public static final int ENTRY_KEYS_PLAYHEAD = 3;

    // Entry Points — the Keys Graph's menu; its graph is EditorSetting RECORDER_ENTRY_KEY_GRAPH, which the drags and
    // the wheel share
    public static final int ENTRY_GRAPH_TITLE = 0;
    public static final int ENTRY_GRAPH_MARKS = 2;
    public static final int ENTRY_GRAPH_PLAYHEAD = 3;
    public static final int ENTRY_GRAPH_TOP = 4;
    public static final int ENTRY_GRAPH_BOTTOM = 5;
    public static final int ENTRY_GRAPH_DETAIL = 6;

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

    // Timeline — where the ruler and the lanes stand in the tab, and the lane strips beside them: how wide, the gap
    // before the lanes, and how each reads
    public static final float TIMELINE_INSET_PIXELS = 12f;
    public static final float TRACK_TOP_PIXELS = -30f;
    public static final float TRACK_HEIGHT_PIXELS = 20f;
    public static final float LANES_TOP_PIXELS = -56f;
    public static final float MIXER_PIXELS = 110f;
    public static final float MIXER_GAP_PIXELS = 6f;
    public static final String TEXT_MIXER_SEPARATOR = "  ";
    public static final String FORMAT_BAR_BEAT = "%d.%d";

    // Keys — a mark set aside is moved this far off the window; a key's diamond is a square turned on its corner,
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
    public static final String TEXT_NO_RECORDING = "No recording open";
    public static final String TEXT_UNSAVED_SUFFIX = " *";
    public static final String TEXT_BREADCRUMB_SEPARATOR = " > ";
    public static final String TEXT_TIMELINE_TITLE = "Timeline";

    // Text — the view
    public static final String TEXT_VIEW_FIT = "Whole timeline";

    // Text — Keys
    public static final String TEXT_KEYS_TITLE = "Keys";
    public static final String TEXT_KEYS_NO_CLIP = "Click a clip on the timeline to see its keys";
    public static final String TEXT_KEYS_NO_TRACKS = "This take recorded nothing a curve can carry";
    public static final String TEXT_KEYS_PICK_LAYER = "Pick a layer in the Layers tab to see its keys";
    public static final String TEXT_LAYER_SOUNDS = "Sounds";
    public static final String TEXT_LAYER_NONE = "no layer picked";
    public static final String TEXT_GRAPH_TITLE = "Keys Graph";
    public static final String TEXT_ROW_MOVEMENT = "Movement";
    public static final String TEXT_ROW_GESTURES = "Gestures";
    public static final String TEXT_GRAPH_EMPTY = "Click a value on the Keys tab to see its curve";
    public static final String TEXT_CURVE_UNLAID = "Its keys are laid once nothing records or exports";

    // Formats
    public static final String FORMAT_TIME = "%d:%05.2f";
    public static final String FORMAT_TIMELINE_TITLE = "Timeline: %s";
    public static final String FORMAT_CLOCK = "%s / %s";

    // Formats — a clip's name on its bar, by the kind of its source in RecordingSourceKind order: a take's, a nested
    // recording's, a song's and a notes clip's instrument
    public static final String[] FORMAT_CLIP_LABELS = { "%s", "[%s]", "%s", "%s" };
    public static final String FORMAT_VIEW = "%s to %s";
    public static final String FORMAT_KEYS_TITLE = "Keys: %s";
    public static final String FORMAT_GRAPH_NOW = "Now %s  |  %d keys";
    public static final String FORMAT_GRAPH_KEY = "Now %s  |  %d keys  |  key at %s: %s, %s";
    public static final String FORMAT_CURVE_HEADER = "%s: %s";
    public static final String FORMAT_SOUND_ROW = "%s (%d)";
    public static final String FORMAT_PROGRESS = "Frame %d of %d";
}
