package editor.runtime;

import application.runtime.RuntimeSetting;
import engine.input.Buttons;

public class EditorSetting {

    /*
     * Editor-only constants — chrome menus, cursors, input keys, menu entry
     * point indices, layout persistence, tool tabs, the name, export and
     * settings dialogs, console commands and the command drag between windows,
     * the Dev window's time units, editor shortcuts, the info pipeline's
     * schemas, hierarchy keys, and status text, the item editor's library and
     * status text, the image editing framework's history, files and brush
     * stamping, the brush presets and the shared tools with their canvases and
     * brush sizes, the biome planner's grid, the world map's status text and
     * overlay toggles, the world editor's palette and status text, the
     * character viewport shared by the character, animation and animation tree
     * editors, those editors' files, steps and status text, the character
     * editor's mesh and weight tools, and the recorder's files, sampling,
     * export, video containers and status text.
     */

    // Cursors
    public static final String CURSOR_RESIZE_H = "menus/CursorStretchIconHorizontal";
    public static final String CURSOR_RESIZE_V = "menus/CursorStretchIconVertical";

    // Layouts
    public static final int LAYOUT_NAME_MAX_LENGTH = 32;
    public static final String LAYOUT_SESSION_NAME = "LastSession";

    // Resize
    public static final float RESIZE_EDGE_TOLERANCE = 8f;

    // Menus
    public static final String MENU_EDITOR_BASE = "editor/EditorWindow/Base";
    public static final String MENU_EDITOR_SECONDARY = "editor/EditorWindow/Secondary";
    public static final String MENU_EDITOR_TOOLBAR = "editor/EditorWindow/Toolbar";
    public static final String MENU_EDITOR_NAME_DIALOG = "editor/EditorWindow/NameDialog";
    public static final String MENU_EDITOR_EXPORT_DIALOG = "editor/EditorWindow/ExportDialog";
    public static final String MENU_EDITOR_LAYOUT_ITEM_TEMPLATE = "editor/EditorWindow/layout_item_template";
    public static final String MENU_EDITOR_SETTINGS_DIALOG = "editor/EditorWindow/SettingsDialog";
    public static final String MENU_EDITOR_SETTINGS_TAB_GROUP = "editor/EditorWindow/settings_tab_group";
    public static final String MENU_EDITOR_SETTINGS_TAB = "editor/EditorWindow/settings_tab";
    public static final String MENU_EDITOR_SETTINGS_SECTION_HEADER = "editor/EditorWindow/settings_section_header";
    public static final String MENU_EDITOR_SETTINGS_NOTE = "editor/EditorWindow/settings_note";
    public static final String MENU_EDITOR_SETTINGS_OPTION_ROW = "editor/EditorWindow/settings_option_row";
    public static final String MENU_EDITOR_SETTINGS_SLIDER_ROW = "editor/EditorWindow/settings_slider_row";
    public static final String MENU_EDITOR_SETTINGS_CHANNEL_ROW = "editor/EditorWindow/settings_channel_row";
    public static final String MENU_EDITOR_SETTINGS_BINDING_ROW = "editor/EditorWindow/settings_binding_row";
    public static final String MENU_EDITOR_SETTINGS_SHORTCUT_ROW = "editor/EditorWindow/settings_shortcut_row";
    public static final String MENU_EDITOR_SETTINGS_COLOR_ROW = "editor/EditorWindow/settings_color_row";
    public static final String MENU_EDITOR_SETTINGS_ACTION_ROW = "editor/EditorWindow/settings_action_row";

    // Elements
    public static final String ELEMENT_LAYOUT_ITEM_LOAD = "layout_item_load";
    public static final String ELEMENT_LAYOUT_ITEM_SAVE = "layout_item_save";
    public static final String ELEMENT_LAYOUT_ITEM_DELETE = "layout_item_delete";
    public static final String ELEMENT_LAYOUT_ITEM_LABEL = "layout_item_label";
    public static final String ELEMENT_SETTINGS_TAB_GROUP_LABEL = "settings_tab_group_label";
    public static final String ELEMENT_SETTINGS_TAB_LABEL = "settings_tab_label";
    public static final String ELEMENT_SETTINGS_TAB_MARKER = "settings_tab_marker";
    public static final String ELEMENT_SETTINGS_ROW_LABEL = "settings_row_label";
    public static final String ELEMENT_SETTINGS_ROW_VALUE = "settings_row_value";
    public static final String ELEMENT_SETTINGS_ROW_PREVIOUS = "settings_row_previous";
    public static final String ELEMENT_SETTINGS_ROW_NEXT = "settings_row_next";
    public static final String ELEMENT_SETTINGS_SLIDER_TRACK = "settings_slider_track";
    public static final String ELEMENT_SETTINGS_SLIDER_KNOB = "settings_slider_knob";
    public static final String ELEMENT_SETTINGS_SLIDER_VALUE = "settings_slider_value";
    public static final String ELEMENT_SETTINGS_COLOR_SWATCH = "settings_color_swatch";

    // Keys
    public static final int KEY_BACKSPACE = 259;
    public static final int KEY_TAB = 258;
    public static final int KEY_UP = 265;
    public static final int KEY_DOWN = 264;
    public static final int KEY_ENTER = 257;
    public static final int KEY_ENTER_NUMPAD = 335;
    public static final int KEY_ESCAPE = 256;
    public static final int KEY_SPACE = 32;
    public static final int KEY_MINUS = 45;
    public static final int KEY_0 = 48;
    public static final int KEY_9 = 57;
    public static final int KEY_A = 65;
    public static final int KEY_Z = 90;
    public static final int KEY_LEFT_SHIFT = 340;
    public static final int KEY_RIGHT_SHIFT = 344;

    // Entry point indices
    public static final int ENTRY_LAYOUTS_LIST = 0;
    public static final int ENTRY_NAME_DIALOG_LABEL = 0;
    public static final int ENTRY_NAME_DIALOG_TITLE = 1;
    public static final int ENTRY_EXPORT_DIALOG_TITLE = 0;
    public static final int ENTRY_EXPORT_DIALOG_FILE = 1;
    public static final int ENTRY_EXPORT_DIALOG_FILL = 2;
    public static final int ENTRY_EXPORT_DIALOG_FRAMES = 3;
    public static final int ENTRY_EXPORT_DIALOG_TIME = 4;
    public static final int ENTRY_EXPORT_DIALOG_MESSAGE = 5;
    public static final int ENTRY_EXPORT_DIALOG_BUTTON = 6;
    public static final int ENTRY_SETTINGS_DIALOG_TABS = 0;
    public static final int ENTRY_SETTINGS_DIALOG_OPTIONS = 1;
    public static final int ENTRY_SETTINGS_DIALOG_STATUS = 2;

    // Name Dialog
    public static final int NAME_INPUT_MAX_LENGTH = 32;
    public static final int TEXT_INPUT_MAX_LENGTH = 256;
    public static final String DIALOG_TITLE_CREATE_LAYOUT = "New Layout";
    public static final String DIALOG_TITLE_NEW_ITEM = "New Item";
    public static final String DIALOG_TITLE_NEW_PART = "New Part";
    public static final String DIALOG_TITLE_RENAME_PART = "Rename Part";
    public static final String DIALOG_TITLE_NEW_ENTRY = "New Entry";
    public static final String DIALOG_TITLE_NEW_FILE = "New File";
    public static final String DIALOG_TITLE_NEW_KEY = "New Key";
    public static final String DIALOG_TITLE_EDIT_PREFIX = "Edit ";
    public static final String DIALOG_TITLE_DELETE_PREFIX = "Type ";
    public static final String DIALOG_TITLE_DELETE_SUFFIX = " to delete it";
    public static final String DIALOG_TITLE_COPY_PREFIX = "Copy ";
    public static final String DIALOG_TITLE_COPY_SUFFIX = " as";
    public static final String DIALOG_TITLE_UNIQUE_PREFIX = "Unique ";
    public static final String DIALOG_TITLE_EDIT_VALUE = "Edit Value";
    public static final String DIALOG_TITLE_NEW_CLIP = "New Clip";
    public static final String DIALOG_TITLE_NEW_TREE = "New Animation Tree";
    public static final String DIALOG_TITLE_NEW_LAYER = "New Layer";
    public static final String DIALOG_TITLE_RENAME_LAYER = "Rename Layer";
    public static final String DIALOG_TITLE_NEW_NODE = "New Node";
    public static final String DIALOG_TITLE_RENAME_NODE = "Rename Node";
    public static final String DIALOG_TITLE_NEW_RECORDING = "New Recording";
    public static final String DIALOG_TITLE_RENAME_RECORDING = "Rename Recording";
    public static final String DIALOG_TITLE_RENAME_TAKE = "Rename Take";
    public static final String DIALOG_TITLE_KEY_VALUE = "Key Value";
    public static final String DIALOG_TITLE_PLACEMENT_X = "Placement X";
    public static final String DIALOG_TITLE_PLACEMENT_Z = "Placement Z";

    // Export Dialog — the bar fills in fixed steps; the text refreshes a few times a second of real time, and the
    // time left is estimated once a few frames have been made
    public static final int EXPORT_DIALOG_FILL_STEPS = 200;
    public static final long EXPORT_DIALOG_REFRESH_NANOS = 250_000_000L;
    public static final int EXPORT_DIALOG_ESTIMATE_FRAMES = 3;
    public static final long EXPORT_DIALOG_TIME_UNSET = -1L;
    public static final int EXPORT_DIALOG_SECONDS_PER_MINUTE = 60;
    public static final int EXPORT_DIALOG_SECONDS_PER_HOUR = 3600;
    public static final String EXPORT_DIALOG_TITLE_EXPORTING = "Exporting video";
    public static final String EXPORT_DIALOG_TITLE_EXPORTED = "Video exported";
    public static final String EXPORT_DIALOG_TITLE_FAILED = "Export failed";
    public static final String EXPORT_DIALOG_TITLE_CANCELLED = "Export cancelled";
    public static final String EXPORT_DIALOG_TITLE_REFUSED = "Could not export";
    public static final String EXPORT_DIALOG_BUTTON_CANCEL = "Cancel";
    public static final String EXPORT_DIALOG_BUTTON_CLOSE = "OK";
    public static final String EXPORT_DIALOG_TEXT_PREPARING = "Loading the world around the camera";
    public static final String EXPORT_DIALOG_TEXT_ESTIMATING = "estimating";
    public static final String EXPORT_DIALOG_TEXT_EMPTY = "";
    public static final String EXPORT_DIALOG_TEXT_WAIT = "The editor is held until the video is finished";
    public static final String EXPORT_DIALOG_TEXT_SAVED_IN = "Saved in ";
    public static final String EXPORT_DIALOG_FORMAT_FILE = "%s   %d x %d   %d fps   %s";
    public static final String EXPORT_DIALOG_FORMAT_FRAMES = "Frame %d of %d   %d%%";
    public static final String EXPORT_DIALOG_FORMAT_FRAMES_DONE = "%d frames";
    public static final String EXPORT_DIALOG_FORMAT_TIME = "Elapsed %s   Remaining %s";
    public static final String EXPORT_DIALOG_FORMAT_TOOK = "Took %s";
    public static final String EXPORT_DIALOG_FORMAT_MINUTES = "%d:%02d";
    public static final String EXPORT_DIALOG_FORMAT_HOURS = "%d:%02d:%02d";

    // Settings Dialog — pages, sections and notes
    public static final String SETTINGS_GROUP_EDITOR = "Editor";
    public static final String SETTINGS_GROUP_DEV_MODE = "Dev Mode";
    public static final String SETTINGS_TAB_GENERAL = "General";
    public static final String SETTINGS_TAB_SHORTCUTS = "Shortcuts";
    public static final String SETTINGS_TAB_COLORS = "Color Scheme";
    public static final String SETTINGS_TAB_GRAPHICS = "Graphics";
    public static final String SETTINGS_TAB_EFFECTS = "Effects";
    public static final String SETTINGS_TAB_GAMEPLAY = "Gameplay";
    public static final String SETTINGS_TAB_CONTROLS = "Controls";
    public static final String SETTINGS_SECTION_PREVIEW_WORLD = "Preview World";
    public static final String SETTINGS_SECTION_SURFACES = "Surfaces";
    public static final String SETTINGS_SECTION_CONTROLS = "Controls";
    public static final String SETTINGS_SECTION_TEXT = "Text";
    public static final String SETTINGS_NOTE_WINDOW = "Sets the editor's own window";
    public static final String SETTINGS_NOTE_SAVED = "Every setting is saved to EditorSettings when this dialog closes";
    public static final String SETTINGS_NOTE_DEV_MODE = "Applies to every Dev Mode and Preview tab";
    public static final String SETTINGS_NOTE_WORLD_APPLY = "World changes apply when this dialog closes";
    public static final String SETTINGS_NOTE_CONTROLS = "Click an action, then press its new key or button";
    public static final String SETTINGS_NOTE_SHORTCUTS =
            "Click a shortcut, then press its new keys; it saves to its tool's file";
    public static final String SETTINGS_NOTE_COLORS = "Click a color to edit it; the editor follows as you change it";

    // Settings Dialog — the active tab and a row waiting for keys are tinted with the accent; a slider's knob moves
    // in fixed steps
    public static final float SETTINGS_TAB_ACTIVE_ALPHA = 0.32f;
    public static final float SETTINGS_CAPTURE_ALPHA = 0.45f;
    public static final int SETTINGS_SLIDER_STEPS = 200;

    // Settings Dialog — status text
    public static final String SETTINGS_STATUS_NONE = "";
    public static final String SETTINGS_STATUS_CAPTURE_BINDING = "Press a key or mouse button, or Escape to cancel";
    public static final String SETTINGS_STATUS_CAPTURE_PRESSED = "Press the new keys, or Escape to cancel";
    public static final String SETTINGS_STATUS_CAPTURE_HELD =
            "Press a key, or press and let go of modifiers, or Escape to cancel";
    public static final String SETTINGS_STATUS_CONTROLS_RESET = "Controls reset to their defaults";
    public static final String SETTINGS_STATUS_COLORS_RESET = "Color scheme reset to its defaults";
    public static final String SETTINGS_STATUS_COLOR_RESET_SUFFIX = " reset to its default";

    // Settings Dialog — shortcuts
    public static final String SETTINGS_SHORTCUT_CAPTURE_PROMPT = "Press keys";
    public static final String SETTINGS_SHORTCUT_HELD_SUFFIX = " (hold)";
    public static final String SETTINGS_SHORTCUT_FORMAT_SAVED = "%s is now %s";
    public static final String SETTINGS_SHORTCUT_FORMAT_CLASH = "%s is already %s in this tool";
    public static final String SETTINGS_SHORTCUT_FORMAT_UNSAVED =
            "%s has unsaved edits; save or revert it in the Info Panel first";
    public static final String SETTINGS_SHORTCUT_FORMAT_MISSING = "%s is no longer in %s; reopen the editor to load it";
    public static final String SETTINGS_SHORTCUT_REFUSED_PREFIX = "Not saved: ";

    // Settings Dialog — color scheme
    public static final String SETTINGS_COLOR_BACKGROUND = "Background";
    public static final String SETTINGS_COLOR_PANEL = "Panel";
    public static final String SETTINGS_COLOR_HEADER = "Header";
    public static final String SETTINGS_COLOR_OUTLINE = "Outline";
    public static final String SETTINGS_COLOR_SHADOW = "Shadow";
    public static final String SETTINGS_COLOR_CONTROL = "Control";
    public static final String SETTINGS_COLOR_CONTROL_HOVER = "Control Hover";
    public static final String SETTINGS_COLOR_ACCENT = "Accent";
    public static final String SETTINGS_COLOR_ACCENT_HOVER = "Accent Hover";
    public static final String SETTINGS_COLOR_DANGER = "Danger";
    public static final String SETTINGS_COLOR_TEXT = "Text";
    public static final String SETTINGS_COLOR_TEXT_MUTED = "Muted Text";
    public static final String SETTINGS_COLOR_TEXT_ON_ACCENT = "Text on Accent";
    public static final String[] SETTINGS_COLOR_CHANNEL_LABELS = { "Red", "Green", "Blue", "Alpha" };
    public static final String SETTINGS_ACTION_ENTER_HEX = "Enter Hex Value";
    public static final String SETTINGS_ACTION_RESET_COLOR = "Reset to Default";
    public static final String SETTINGS_ACTION_RESET_COLORS = "Reset Color Scheme";
    public static final String SETTINGS_DIALOG_TITLE_HEX_PREFIX = "Hex Value of ";
    public static final String SETTINGS_FORMAT_HEX = "#%02X%02X%02X%02X";
    public static final String SETTINGS_HEX_PREFIX = "#";
    public static final int SETTINGS_HEX_DIGITS_PER_CHANNEL = 2;
    public static final int SETTINGS_HEX_RADIX = 16;

    // Tabs
    public static final String TAB_TITLE_COMMAND_CONSOLE = "Command Console";
    public static final String TAB_TITLE_ARPG_VIEWER = "ARPG Viewer";
    public static final String TAB_TITLE_ANIMATION_EDITOR = "Animation Editor";
    public static final String TAB_TITLE_ANIMATION_TREE_EDITOR = "Animation Tree Editor";
    public static final String TAB_TITLE_BIOME_PLANNER = "Biome Planner";
    public static final String TAB_TITLE_BIOME_VIEWER = "Biome Viewer";
    public static final String TAB_TITLE_BRUSHES = "Brushes";
    public static final String TAB_TITLE_CHARACTER_EDITOR = "Character Editor";
    public static final String TAB_TITLE_CONSOLE = "Console";
    public static final String TAB_TITLE_HIERARCHY = "Hierarchy";
    public static final String TAB_TITLE_INFO_PANEL = "Info Panel";
    public static final String TAB_TITLE_ITEM_EDITOR = "Item Editor";
    public static final String TAB_TITLE_PALETTE = "Palette";
    public static final String TAB_TITLE_PROFILER = "Profiler";
    public static final String TAB_TITLE_RECORDER = "Recorder";
    public static final String TAB_TITLE_SETTLEMENT_PLANNER = "Settlement Planner";
    public static final String TAB_TITLE_SHIPWRIGHT = "Shipwright";
    public static final String TAB_TITLE_TEXTURE_VIEWER = "Texture Viewer";
    public static final String TAB_TITLE_TOOLS = "Tools";
    public static final String TAB_TITLE_WORLD_EDITOR = "World Editor";
    public static final String TAB_TITLE_WORLD_MAP = "World Map";

    // Shortcuts
    public static final String SHORTCUT_PATH = "shortcuts";
    public static final String SHORTCUT_NAME_SEPARATOR = "/";
    public static final String SHORTCUT_MODIFIER_CONTROL = "Ctrl";
    public static final String SHORTCUT_MODIFIER_SHIFT = "Shift";
    public static final String SHORTCUT_MODIFIER_ALT = "Alt";

    // Commands
    public static final String COMMAND_PATH = "commands";
    public static final String COMMAND_TOKEN_SEPARATOR_PATTERN = "\\s+";
    public static final String COMMAND_TOKEN_SEPARATOR = " ";
    public static final String COMMAND_ARGUMENT_OPEN = " <";
    public static final String COMMAND_ARGUMENT_CLOSE = ">";
    public static final String COMMAND_ECHO_PREFIX = "> ";
    public static final String COMMAND_MESSAGE_UNKNOWN = "Unknown command: ";
    public static final String COMMAND_MESSAGE_USAGE = "Usage: ";
    public static final String COMMAND_MESSAGE_NO_DEV_WINDOWS = "No Dev window is open to run the command";
    public static final String COMMAND_MESSAGE_NO_ACTION = " has no action for command: ";
    public static final String COMMAND_MESSAGE_WINDOW_CRASHED = " has crashed and cannot run command: ";
    public static final String COMMAND_MESSAGE_MATCHES = "Matching commands: ";
    public static final String COMMAND_MESSAGE_LIST_SEPARATOR = ", ";

    // Commands — Drag
    public static final int COMMAND_DRAG_BUTTON = Buttons.LEFT;
    public static final String MENU_COMMAND_DRAG_GHOST = "editor/CommandConsole/DragGhost";
    public static final int ENTRY_COMMAND_DRAG_GHOST_LABEL = 0;
    public static final float COMMAND_DRAG_GHOST_W = 200f;
    public static final float COMMAND_DRAG_GHOST_H = 26f;
    public static final float COMMAND_DRAG_GHOST_OFFSET = 14f;

    // Commands — Camera
    public static final String COMMAND_FLY = "fly";
    public static final String COMMAND_MESSAGE_FREE_CAMERA_ON = ": free flying";
    public static final String COMMAND_MESSAGE_FREE_CAMERA_OFF = ": back on foot";

    // Commands — Items
    public static final String COMMAND_GIVE = "give";
    public static final String COMMAND_ARGUMENT_ITEM = "item";
    public static final String COMMAND_MESSAGE_ITEM_UNKNOWN = ": no item is named ";
    public static final String COMMAND_MESSAGE_ITEM_GIVEN = ": given ";
    public static final String COMMAND_MESSAGE_ITEM_NO_ROOM = ": no room in the backpack or hands for ";
    public static final String COMMAND_MESSAGE_ITEM_NO_CHARACTER = ": free flying, so there is no character to give ";

    // Commands — Vehicles
    public static final String COMMAND_SPAWN_VEHICLE = "spawnvehicle";
    public static final String COMMAND_REMOVE_VEHICLE = "removevehicle";
    public static final String COMMAND_ARGUMENT_VEHICLE = "vehicle";
    public static final String COMMAND_MESSAGE_VEHICLE_UNKNOWN = ": no vehicle is named ";
    public static final String COMMAND_MESSAGE_VEHICLE_SPAWNED = ": spawned ";
    public static final String COMMAND_MESSAGE_VEHICLE_REMOVED = ": removed ";
    public static final String COMMAND_MESSAGE_VEHICLE_NONE_NEAR = ": no vehicle lies near enough to remove";
    public static final String COMMAND_MESSAGE_VEHICLE_NO_CHARACTER = ": free flying, so there is no character"
            + " to spawn beside: ";
    public static final float DEV_VEHICLE_REMOVE_REACH = 24f;

    // Commands — Teleport
    public static final String COMMAND_TELEPORT = "teleport";
    public static final String COMMAND_ARGUMENT_CHUNK_X = "chunkX";
    public static final String COMMAND_ARGUMENT_CHUNK_Y = "chunkY";
    public static final String COMMAND_MESSAGE_TELEPORT_INVALID = ": a chunk coordinate must be a whole number, not ";
    public static final String COMMAND_MESSAGE_TELEPORT_NO_PLAYER = ": no player to move";
    public static final String COMMAND_MESSAGE_TELEPORT_MOVED = ": moved to chunk ";
    public static final String COMMAND_MESSAGE_TELEPORT_SEPARATOR = ", ";

    // Commands — Time
    public static final String COMMAND_TIME = "time";
    public static final String COMMAND_DAWN = "dawn";
    public static final String COMMAND_NOON = "noon";
    public static final String COMMAND_DUSK = "dusk";
    public static final String COMMAND_MIDNIGHT = "midnight";
    public static final String COMMAND_ARGUMENT_TIME_UNIT = "unit";
    public static final String COMMAND_ARGUMENT_TIME_AMOUNT = "amount";
    public static final double COMMAND_TIME_RAW_MIDNIGHT = 0.0;
    public static final String COMMAND_MESSAGE_TIME_UNKNOWN_UNIT = ": no time unit is named ";
    public static final String COMMAND_MESSAGE_TIME_UNIT_HINT = ", use one of: ";
    public static final String COMMAND_MESSAGE_TIME_UNAVAILABLE = ": this world's calendar has no ";
    public static final String COMMAND_MESSAGE_TIME_INVALID = ": time moves by a whole number, such as 3 or -3, not ";
    public static final String COMMAND_MESSAGE_TIME_BEFORE_CALENDAR = ": cannot move the clock before the first day"
            + " of the calendar's starting age";
    public static final String COMMAND_MESSAGE_TIME_AFTER_CALENDAR = ": cannot move the clock past the last day of age ";
    public static final String COMMAND_MESSAGE_TIME_MOVED = ": the clock now reads ";

    // Dev Time — units, the groups the command console's dials sit in, and how the clock is described
    public static final String DEV_TIME_UNIT_HOUR = "hour";
    public static final String DEV_TIME_UNIT_MINUTE = "minute";
    public static final String DEV_TIME_UNIT_WEEKDAY = "weekday";
    public static final String DEV_TIME_UNIT_DAY = "day";
    public static final String DEV_TIME_UNIT_MONTH = "month";
    public static final String DEV_TIME_UNIT_SEASON = "season";
    public static final String DEV_TIME_UNIT_YEAR = "year";
    public static final String DEV_TIME_UNIT_AGE = "age";
    public static final String DEV_TIME_UNIT_MOON = "moon";
    public static final String DEV_TIME_TITLE_HOUR = "Hour";
    public static final String DEV_TIME_TITLE_MINUTE = "Minute";
    public static final String DEV_TIME_TITLE_WEEKDAY = "Weekday";
    public static final String DEV_TIME_TITLE_DAY = "Day";
    public static final String DEV_TIME_TITLE_MONTH = "Month";
    public static final String DEV_TIME_TITLE_SEASON = "Season";
    public static final String DEV_TIME_TITLE_YEAR = "Year";
    public static final String DEV_TIME_TITLE_AGE = "Age";
    public static final String DEV_TIME_TITLE_MOON = "Moon";
    public static final String DEV_TIME_GROUP_TIME_OF_DAY = "Time of Day";
    public static final String DEV_TIME_GROUP_CALENDAR = "Calendar";
    public static final String DEV_TIME_GROUP_YEARS = "Years";
    public static final String DEV_TIME_GROUP_MOON = "Moon";
    public static final int DEV_TIME_AGE_SPAN = 999;
    public static final String DEV_TIME_MINUTE_FORMAT = "%02d";
    public static final String DEV_TIME_CLOCK_SEPARATOR = ":";
    public static final String DEV_TIME_DATE_SEPARATOR = ", ";
    public static final String DEV_TIME_WORD_SEPARATOR = " ";
    public static final String DEV_TIME_YEAR_PREFIX = "year ";
    public static final String DEV_TIME_AGE_PREFIX = " of age ";
    public static final String DEV_TIME_MOON_SUFFIX = " moon";
    public static final String[] DEV_TIME_MOON_PHASES = {
            "New", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
            "Full", "Waning Gibbous", "Last Quarter", "Waning Crescent" };

    // Viewports
    public static final String FBO_EDITOR_SCENE = "EditorScene";
    public static final String FBO_EDITOR_OVERLAY = "EditorOverlay";

    // Character View — meshes, materials and draw order
    public static final String CHARACTER_VIEW_MESH_GRID = "editor/CharacterViewGrid";
    public static final String CHARACTER_VIEW_MESH_MARKER = "editor/CharacterViewMarker";
    public static final String CHARACTER_VIEW_MATERIAL_CHARACTER = "editor/CharacterViewCharacterMaterial";
    public static final String CHARACTER_VIEW_MATERIAL_GRID = "editor/CharacterViewGridMaterial";
    public static final String CHARACTER_VIEW_MATERIAL_MARKER = "editor/CharacterViewMarkerMaterial";
    public static final String CHARACTER_VIEW_MATERIAL_MARKER_SELECTED = "editor/CharacterViewMarkerSelectedMaterial";
    public static final String CHARACTER_VIEW_UNIFORM_MODEL = "u_model";
    public static final int CHARACTER_VIEW_DEPTH_GRID = 0;
    public static final int CHARACTER_VIEW_DEPTH_MARKER = 0;
    public static final int CHARACTER_VIEW_DEPTH_SELECTED = 1;
    public static final int CHARACTER_VIEW_LAYER_OVERLAY = RuntimeSetting.LAYER_WORLD + 1;

    // Character View — markers, sizes in blocks
    public static final float CHARACTER_VIEW_JOINT_SIZE = 0.035f;
    public static final float CHARACTER_VIEW_JOINT_SELECTED_SIZE = 0.06f;

    // Character View — the preview's build between thin (0) and heavy (1)
    public static final float CHARACTER_VIEW_WEIGHT_RATIO = 0.5f;

    // Character View — camera, the target a share of the model's height and the distances per block of it
    public static final float CHARACTER_VIEW_ORBIT_TARGET_HEIGHT = 0.55f;
    public static final float CHARACTER_VIEW_ORBIT_DISTANCE_DEFAULT = 1.6f;
    public static final float CHARACTER_VIEW_ORBIT_DISTANCE_MIN = 0.35f;
    public static final float CHARACTER_VIEW_ORBIT_DISTANCE_MAX = 5f;
    public static final float CHARACTER_VIEW_ORBIT_ZOOM_STEP = 0.1f;
    public static final float CHARACTER_VIEW_ORBIT_INITIAL_YAW_DEGREES = 25f;
    public static final float CHARACTER_VIEW_ORBIT_INITIAL_PITCH_DEGREES = 15f;
    public static final int CHARACTER_VIEW_BUTTON_ORBIT = Buttons.RIGHT;

    // Character View — panel keys, numbers and text
    public static final String CHARACTER_VIEW_KEY_SEPARATOR = ":";
    public static final float CHARACTER_VIEW_DECIMAL_SCALE = 10000f;
    public static final String CHARACTER_VIEW_ZERO_TEXT = "0";
    public static final String[] CHARACTER_VIEW_AXIS_NAMES = { "x", "y", "z" };
    public static final String[] CHARACTER_VIEW_AXIS_LABELS = { "X", "Y", "Z" };
    public static final String[] CHARACTER_VIEW_CHANNEL_LABELS = { "Red", "Green", "Blue" };
    public static final String CHARACTER_VIEW_LABEL_SPACE = " ";
    public static final String CHARACTER_VIEW_PAIR_SEPARATOR = "  :  ";
    public static final String CHARACTER_VIEW_COUNT_SEPARATOR = " / ";
    public static final String CHARACTER_VIEW_NONE = "none";
    public static final String CHARACTER_VIEW_YES = "Yes";
    public static final String CHARACTER_VIEW_NO = "No";
    public static final String CHARACTER_VIEW_DIRTY_MARKER = "*";
    public static final String CHARACTER_VIEW_STATUS_SEPARATOR = "  |  ";
    public static final String CHARACTER_VIEW_MESSAGE_PREVIEW_REFUSED = "Preview not rebuilt: ";

    // Character Editor — template fields
    public static final String CHARACTER_EDITOR_FIELD_MODEL = "model";
    public static final String CHARACTER_EDITOR_FIELD_APPEARANCE = "appearance";
    public static final String CHARACTER_EDITOR_FIELD_FEATURES = "features";
    public static final String CHARACTER_EDITOR_FIELD_SKIN_COLOR = "skin_color";
    public static final String CHARACTER_EDITOR_FIELD_HAIR_COLOR = "hair_color";
    public static final String CHARACTER_EDITOR_FIELD_SKIN_PALETTE = "skin_palette";
    public static final String CHARACTER_EDITOR_FIELD_HAIR_PALETTE = "hair_palette";
    public static final String CHARACTER_EDITOR_FIELD_BUILD = "build";
    public static final String CHARACTER_EDITOR_FIELD_THIN = "thin";
    public static final String CHARACTER_EDITOR_FIELD_HEAVY = "heavy";
    public static final String CHARACTER_EDITOR_FIELD_BONES = "bones";
    public static final String CHARACTER_EDITOR_FIELD_EQUIPMENT = "equipment";
    public static final String CHARACTER_EDITOR_FIELD_SLOT = "slot";
    public static final String CHARACTER_EDITOR_FIELD_BONE = "bone";
    public static final String CHARACTER_EDITOR_FIELD_POSITION = "position";
    public static final String CHARACTER_EDITOR_FIELD_ROTATION = "rotation";
    public static final String CHARACTER_EDITOR_FIELD_SIZE = "size";
    public static final String CHARACTER_EDITOR_FIELD_HOLD = "hold";

    // Character Editor — panel keys
    public static final String CHARACTER_EDITOR_KEY_POSE = "pose";
    public static final String CHARACTER_EDITOR_KEY_FEATURE = "feature";
    public static final String CHARACTER_EDITOR_KEY_SKIN = "skin";
    public static final String CHARACTER_EDITOR_KEY_HAIR = "hair";
    public static final String CHARACTER_EDITOR_KEY_COLOR_SUFFIX = "_color";
    public static final String CHARACTER_EDITOR_KEY_TONE_SUFFIX = "_tone";
    public static final String CHARACTER_EDITOR_KEY_TONE_COLOR_SUFFIX = "_tone_color";
    public static final String CHARACTER_EDITOR_KEY_ADD_TONE_SUFFIX = "_add_tone";
    public static final String CHARACTER_EDITOR_KEY_REMOVE_TONE_SUFFIX = "_remove_tone";
    public static final String CHARACTER_EDITOR_KEY_THIN = "thin";
    public static final String CHARACTER_EDITOR_KEY_HEAVY = "heavy";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT = "weight";
    public static final String CHARACTER_EDITOR_KEY_HEAD = "head";
    public static final String CHARACTER_EDITOR_KEY_GIRTH = "girth";
    public static final String CHARACTER_EDITOR_KEY_BONE = "bone";
    public static final String CHARACTER_EDITOR_KEY_ANCHOR = "anchor";
    public static final String CHARACTER_EDITOR_KEY_ANCHOR_SLOT = "anchor_slot";
    public static final String CHARACTER_EDITOR_KEY_ANCHOR_BONE = "anchor_bone";
    public static final String CHARACTER_EDITOR_KEY_ANCHOR_HOLD = "anchor_hold";
    public static final String CHARACTER_EDITOR_KEY_ANCHOR_POSITION = "anchor_position";
    public static final String CHARACTER_EDITOR_KEY_ANCHOR_ROTATION = "anchor_rotation";
    public static final String CHARACTER_EDITOR_KEY_ANCHOR_SIZE = "anchor_size";
    public static final String CHARACTER_EDITOR_KEY_ADD_ANCHOR = "add_anchor";
    public static final String CHARACTER_EDITOR_KEY_DUPLICATE_ANCHOR = "duplicate_anchor";
    public static final String CHARACTER_EDITOR_KEY_REMOVE_ANCHOR = "remove_anchor";

    // Character Editor — steps and ranges
    public static final float CHARACTER_EDITOR_COLOR_DEFAULT = 1f;
    public static final float CHARACTER_EDITOR_COLOR_STEP = 0.02f;
    public static final float CHARACTER_EDITOR_BUILD_FACTOR_STEP = 0.05f;
    public static final float CHARACTER_EDITOR_BUILD_FACTOR_MIN = 0.1f;
    public static final float CHARACTER_EDITOR_BUILD_FACTOR_MAX = 3f;
    public static final float CHARACTER_EDITOR_WEIGHT_STEP = 0.1f;
    public static final float CHARACTER_EDITOR_HEAD_STEP = 0.05f;
    public static final float CHARACTER_EDITOR_GIRTH_STEP = 0.05f;
    public static final float CHARACTER_EDITOR_GIRTH_MIN = 0f;
    public static final float CHARACTER_EDITOR_GIRTH_MAX = 2f;
    public static final float CHARACTER_EDITOR_POSITION_STEP = 0.005f;
    public static final float CHARACTER_EDITOR_ROTATION_STEP = 5f;
    public static final float CHARACTER_EDITOR_SIZE_STEP = 0.01f;
    public static final float CHARACTER_EDITOR_ANCHOR_SIZE_MIN = 0.01f;
    public static final float CHARACTER_EDITOR_ANCHOR_SIZE_DEFAULT = 0.2f;

    // Character Editor — preview speeds, in blocks per second
    public static final float CHARACTER_EDITOR_WALK_SPEED = 2.1f;
    public static final float CHARACTER_EDITOR_RUN_SPEED = 8f;

    // Character Editor — modes and poses
    public static final String CHARACTER_EDITOR_MODE_APPEARANCE = "Appearance";
    public static final String CHARACTER_EDITOR_MODE_BUILD = "Build";
    public static final String CHARACTER_EDITOR_MODE_EQUIPMENT = "Equipment";
    public static final String CHARACTER_EDITOR_MODE_MESH = "Mesh";
    public static final String CHARACTER_EDITOR_MODE_WEIGHTS = "Weights";
    public static final String CHARACTER_EDITOR_POSE_REST = "Rest";
    public static final String CHARACTER_EDITOR_POSE_IDLE = "Idle";
    public static final String CHARACTER_EDITOR_POSE_WALK = "Walk";
    public static final String CHARACTER_EDITOR_POSE_RUN = "Run";

    // Character Editor — panel text
    public static final String CHARACTER_EDITOR_HEADER_PREVIEW = "Preview";
    public static final String CHARACTER_EDITOR_HEADER_FEATURES = "Default Features";
    public static final String CHARACTER_EDITOR_HEADER_NO_APPEARANCE = "No appearance block";
    public static final String CHARACTER_EDITOR_HEADER_SKIN = "Skin";
    public static final String CHARACTER_EDITOR_HEADER_HAIR = "Hair";
    public static final String CHARACTER_EDITOR_HEADER_BONES = "Bones";
    public static final String CHARACTER_EDITOR_HEADER_BUILD_CURVE = "Build Curve";
    public static final String CHARACTER_EDITOR_HEADER_PREVIEW_BODY = "Preview Body";
    public static final String CHARACTER_EDITOR_HEADER_ANCHORS = "Anchors";
    public static final String CHARACTER_EDITOR_HEADER_ANCHOR = "Anchor";
    public static final String CHARACTER_EDITOR_LABEL_POSE = "Pose";
    public static final String CHARACTER_EDITOR_LABEL_TONE = "Tone Worn";
    public static final String CHARACTER_EDITOR_LABEL_TONE_PREFIX = "Tone ";
    public static final String CHARACTER_EDITOR_LABEL_ADD_TONE = "Add Tone";
    public static final String CHARACTER_EDITOR_LABEL_REMOVE_TONE = "Remove Tone";
    public static final String CHARACTER_EDITOR_LABEL_THIN = "Thin";
    public static final String CHARACTER_EDITOR_LABEL_HEAVY = "Heavy";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT = "Weight";
    public static final String CHARACTER_EDITOR_LABEL_HEAD = "Head";
    public static final String CHARACTER_EDITOR_LABEL_GIRTH = "Girth";
    public static final String CHARACTER_EDITOR_LABEL_SLOT = "Slot";
    public static final String CHARACTER_EDITOR_LABEL_BONE = "Bone";
    public static final String CHARACTER_EDITOR_LABEL_HOLD = "Held";
    public static final String CHARACTER_EDITOR_LABEL_POSITION = "Position";
    public static final String CHARACTER_EDITOR_LABEL_ROTATION = "Rotation";
    public static final String CHARACTER_EDITOR_LABEL_SIZE = "Size";
    public static final String CHARACTER_EDITOR_LABEL_ADD_ANCHOR = "Add Anchor";
    public static final String CHARACTER_EDITOR_LABEL_DUPLICATE_ANCHOR = "Duplicate Anchor";
    public static final String CHARACTER_EDITOR_LABEL_REMOVE_ANCHOR = "Remove Anchor";
    public static final String CHARACTER_EDITOR_TONE_DEFAULT = "Default";

    // Character Editor — status text
    public static final String CHARACTER_EDITOR_STATUS_NO_TEMPLATE = "No character open."
            + " Pick an entity in the Hierarchy.";
    public static final String CHARACTER_EDITOR_MESSAGE_SAVED = "Saved ";
    public static final String CHARACTER_EDITOR_MESSAGE_REVERTED = "Reverted ";
    public static final String CHARACTER_EDITOR_MESSAGE_COPIED = "Copied as ";
    public static final String CHARACTER_EDITOR_MESSAGE_DELETED = "Deleted ";
    public static final String CHARACTER_EDITOR_MESSAGE_NO_MODEL = "This entity has no character model to show";

    // Character Editor — mesh parts; a feature part is keyed by its slot's name
    public static final String CHARACTER_EDITOR_PART_BODY = "body";

    // Character Editor — mesh panel keys
    public static final String CHARACTER_EDITOR_KEY_MESH_PART = "mesh_part";
    public static final String CHARACTER_EDITOR_KEY_MESH_MIRROR = "mesh_mirror";
    public static final String CHARACTER_EDITOR_KEY_MESH_XRAY = "mesh_xray";
    public static final String CHARACTER_EDITOR_KEY_MESH_PROPORTIONAL = "mesh_proportional";
    public static final String CHARACTER_EDITOR_KEY_MESH_RADIUS = "mesh_radius";
    public static final String CHARACTER_EDITOR_KEY_MESH_SNAP = "mesh_snap";
    public static final String CHARACTER_EDITOR_KEY_MESH_CENTER = "mesh_center";
    public static final String CHARACTER_EDITOR_KEY_MESH_SIZE = "mesh_size";
    public static final String CHARACTER_EDITOR_KEY_MESH_ROTATE = "mesh_rotate";
    public static final String CHARACTER_EDITOR_KEY_SELECT_ALL = "select_all";
    public static final String CHARACTER_EDITOR_KEY_SELECT_NONE = "select_none";
    public static final String CHARACTER_EDITOR_KEY_SELECT_INVERT = "select_invert";
    public static final String CHARACTER_EDITOR_KEY_SELECT_LINKED = "select_linked";
    public static final String CHARACTER_EDITOR_KEY_SELECT_MIRROR = "select_mirror";
    public static final String CHARACTER_EDITOR_KEY_SELECT_BONE = "select_bone";
    public static final String CHARACTER_EDITOR_KEY_GRAB = "grab";
    public static final String CHARACTER_EDITOR_KEY_ROTATE = "rotate";
    public static final String CHARACTER_EDITOR_KEY_SCALE = "scale";
    public static final String CHARACTER_EDITOR_KEY_EXTRUDE = "extrude";
    public static final String CHARACTER_EDITOR_KEY_DUPLICATE = "duplicate";
    public static final String CHARACTER_EDITOR_KEY_DELETE_VERTICES = "delete_vertices";
    public static final String CHARACTER_EDITOR_KEY_DELETE_FACES = "delete_faces";
    public static final String CHARACTER_EDITOR_KEY_MAKE_FACE = "make_face";
    public static final String CHARACTER_EDITOR_KEY_MERGE = "merge";
    public static final String CHARACTER_EDITOR_KEY_SUBDIVIDE = "subdivide";
    public static final String CHARACTER_EDITOR_KEY_SYMMETRIZE_POSITIVE = "symmetrize_positive";
    public static final String CHARACTER_EDITOR_KEY_SYMMETRIZE_NEGATIVE = "symmetrize_negative";
    public static final String CHARACTER_EDITOR_KEY_FACE_TEXTURE = "face_texture";
    public static final String CHARACTER_EDITOR_KEY_PART_SHAPE = "part_shape";
    public static final String CHARACTER_EDITOR_KEY_PART_SIZE = "part_size";
    public static final String CHARACTER_EDITOR_KEY_PART_TEXTURE = "part_texture";
    public static final String CHARACTER_EDITOR_KEY_ADD_PART = "add_part";
    public static final String CHARACTER_EDITOR_KEY_UNDO = "undo";
    public static final String CHARACTER_EDITOR_KEY_REDO = "redo";
    public static final String CHARACTER_EDITOR_KEY_SAVE_MESH = "save_mesh";
    public static final String CHARACTER_EDITOR_KEY_REVERT_MESH = "revert_mesh";

    // Character Editor — weight panel keys; every weight action shares the prefix
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_PREFIX = "weight_";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_TARGET = "brush_weight";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_STRENGTH = "brush_strength";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_ASSIGN = "weight_assign";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_ADD = "weight_add";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_SUBTRACT = "weight_subtract";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_REMOVE = "weight_remove";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_NORMALIZE = "weight_normalize";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_SMOOTH = "weight_smooth";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_POSITIVE = "weight_mirror_positive";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_NEGATIVE = "weight_mirror_negative";

    // Character Editor — mesh steps and ranges, in blocks unless named otherwise
    public static final float CHARACTER_EDITOR_MESH_RADIUS_DEFAULT = 0.1f;
    public static final float CHARACTER_EDITOR_MESH_RADIUS_MIN = 0.005f;
    public static final float CHARACTER_EDITOR_MESH_RADIUS_MAX = 2f;
    public static final float CHARACTER_EDITOR_MESH_RADIUS_STEP = 0.01f;
    public static final float CHARACTER_EDITOR_MESH_SNAP_DEFAULT = 0.005f;
    public static final float CHARACTER_EDITOR_MESH_SNAP_MAX = 0.5f;
    public static final float CHARACTER_EDITOR_MESH_SNAP_STEP = 0.005f;
    public static final float CHARACTER_EDITOR_MESH_ROTATE_STEP = 15f;
    public static final float CHARACTER_EDITOR_PART_SIZE_DEFAULT = 0.1f;
    public static final float CHARACTER_EDITOR_PART_SIZE_MIN = 0.005f;
    public static final float CHARACTER_EDITOR_PART_SIZE_MAX = 2f;
    public static final float CHARACTER_EDITOR_WEIGHT_STEP_FINE = 0.05f;
    public static final float CHARACTER_EDITOR_WEIGHT_STRENGTH_DEFAULT = 0.5f;

    // Character Mesh — geometry; positions key on the decimal grid in this many signed bits per axis
    public static final int CHARACTER_MESH_KEY_BITS = 21;
    public static final long CHARACTER_MESH_KEY_MASK = (1L << CHARACTER_MESH_KEY_BITS) - 1L;
    public static final float CHARACTER_MESH_CENTER_TOLERANCE = 0.001f;
    public static final float CHARACTER_MESH_SYMMETRIZE_REACH = 0.02f;
    public static final float CHARACTER_MESH_SPAN_EPSILON = 0.0001f;
    public static final float CHARACTER_MESH_WEIGHT_EPSILON = 0.0001f;
    public static final float CHARACTER_MESH_BONE_SELECT_WEIGHT = 0.25f;
    public static final int CHARACTER_MESH_HISTORY_MAX_STEPS = 64;
    public static final int CHARACTER_MESH_SHAPE_SEGMENTS = 8;
    public static final int CHARACTER_MESH_SHAPE_RINGS = 6;
    public static final String CHARACTER_MESH_BONE_SUFFIX_LEFT = "_l";
    public static final String CHARACTER_MESH_BONE_SUFFIX_RIGHT = "_r";

    // Character Editor — mesh shapes and transforms
    public static final String CHARACTER_EDITOR_SHAPE_PLANE = "Plane";
    public static final String CHARACTER_EDITOR_SHAPE_BOX = "Box";
    public static final String CHARACTER_EDITOR_SHAPE_CYLINDER = "Cylinder";
    public static final String CHARACTER_EDITOR_SHAPE_SPHERE = "Sphere";
    public static final String CHARACTER_EDITOR_TRANSFORM_GRAB = "Grab";
    public static final String CHARACTER_EDITOR_TRANSFORM_ROTATE = "Rotate";
    public static final String CHARACTER_EDITOR_TRANSFORM_SCALE = "Scale";

    // Character Editor — mesh panel text
    public static final String CHARACTER_EDITOR_HEADER_PARTS = "Parts";
    public static final String CHARACTER_EDITOR_HEADER_NO_MESH = "This part has no mesh file to edit";
    public static final String CHARACTER_EDITOR_HEADER_OPTIONS = "Options";
    public static final String CHARACTER_EDITOR_HEADER_SELECTION = "Selection";
    public static final String CHARACTER_EDITOR_HEADER_TRANSFORM = "Transform";
    public static final String CHARACTER_EDITOR_HEADER_GEOMETRY = "Geometry";
    public static final String CHARACTER_EDITOR_HEADER_FACES = "Selected Faces";
    public static final String CHARACTER_EDITOR_HEADER_NEW_PART = "New Part";
    public static final String CHARACTER_EDITOR_HEADER_MESH_FILE = "Mesh File";
    public static final String CHARACTER_EDITOR_HEADER_ACTIVE_VERTEX = "Active Vertex";
    public static final String CHARACTER_EDITOR_LABEL_VERTICES = " vertices";
    public static final String CHARACTER_EDITOR_LABEL_FACES = " faces";
    public static final String CHARACTER_EDITOR_LABEL_SELECTED = " selected";
    public static final String CHARACTER_EDITOR_LABEL_MIRROR = "Mirror X";
    public static final String CHARACTER_EDITOR_LABEL_XRAY = "X-Ray";
    public static final String CHARACTER_EDITOR_LABEL_PROPORTIONAL = "Proportional";
    public static final String CHARACTER_EDITOR_LABEL_RADIUS = "Radius";
    public static final String CHARACTER_EDITOR_LABEL_SNAP = "Snap (Ctrl)";
    public static final String CHARACTER_EDITOR_LABEL_CENTER = "Center";
    public static final String CHARACTER_EDITOR_LABEL_TURN = "Turn";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_ALL = "Select All  (A)";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_NONE = "Select None";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_INVERT = "Invert Selection";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_LINKED = "Select Linked  (L)";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_MIRROR = "Select Mirror";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_BONE = "Select Weighted to ";
    public static final String CHARACTER_EDITOR_LABEL_GRAB = "Grab  (G)";
    public static final String CHARACTER_EDITOR_LABEL_ROTATE = "Rotate  (R)";
    public static final String CHARACTER_EDITOR_LABEL_SCALE = "Scale  (S)";
    public static final String CHARACTER_EDITOR_LABEL_EXTRUDE = "Extrude  (E)";
    public static final String CHARACTER_EDITOR_LABEL_DUPLICATE = "Duplicate  (Shift+D)";
    public static final String CHARACTER_EDITOR_LABEL_MAKE_FACE = "Make Face  (F)";
    public static final String CHARACTER_EDITOR_LABEL_MERGE = "Merge at Center  (M)";
    public static final String CHARACTER_EDITOR_LABEL_SUBDIVIDE = "Subdivide Faces";
    public static final String CHARACTER_EDITOR_LABEL_DELETE_VERTICES = "Delete Vertices  (X)";
    public static final String CHARACTER_EDITOR_LABEL_DELETE_FACES = "Delete Faces";
    public static final String CHARACTER_EDITOR_LABEL_SYMMETRIZE_POSITIVE = "Symmetrize  +X to -X";
    public static final String CHARACTER_EDITOR_LABEL_SYMMETRIZE_NEGATIVE = "Symmetrize  -X to +X";
    public static final String CHARACTER_EDITOR_LABEL_TEXTURE = "Texture";
    public static final String CHARACTER_EDITOR_LABEL_SHAPE = "Shape";
    public static final String CHARACTER_EDITOR_LABEL_ADD_PART = "Add at ";
    public static final String CHARACTER_EDITOR_LABEL_UNDO = "Undo  (Ctrl+Z)";
    public static final String CHARACTER_EDITOR_LABEL_REDO = "Redo  (Ctrl+Y)";
    public static final String CHARACTER_EDITOR_LABEL_SAVE_MESH = "Save Mesh";
    public static final String CHARACTER_EDITOR_LABEL_REVERT_MESH = "Revert Mesh";
    public static final String CHARACTER_EDITOR_LABEL_STRENGTH = "Strength";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_ASSIGN = "Assign Weight";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_ADD = "Add 0.05";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_SUBTRACT = "Subtract 0.05";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_REMOVE = "Remove Bone";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_NORMALIZE = "Normalize";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_SMOOTH = "Smooth Selected";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_POSITIVE = "Mirror Weights  +X to -X";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_NEGATIVE = "Mirror Weights  -X to +X";

    // Character Editor — mesh status text
    public static final String CHARACTER_EDITOR_STATUS_MESH_HINT = "Click or drag to select, Shift adds, Ctrl removes";
    public static final String CHARACTER_EDITOR_STATUS_TRANSFORM_HINT = "X Y Z hold to an axis, Ctrl snaps, click or "
            + "Enter confirms, right click or Esc cancels";
    public static final String CHARACTER_EDITOR_MESSAGE_MESH_EMPTY = "A mesh needs at least one face";
    public static final String CHARACTER_EDITOR_MESSAGE_MESH_LIMIT = "That edit would pass the mesh vertex limit";
    public static final String CHARACTER_EDITOR_MESSAGE_MESH_NOT_SAVED = "A mesh file could not be written";
    public static final String CHARACTER_EDITOR_MESSAGE_MESH_NOT_REVERTED = "The mesh file could not be read";
    public static final String CHARACTER_EDITOR_MESSAGE_NOTHING_TO_EXTRUDE = "Select faces or edges to extrude";
    public static final String CHARACTER_EDITOR_MESSAGE_NO_FACES = "Select whole faces first";
    public static final String CHARACTER_EDITOR_MESSAGE_FACE_VERTICES = "Select 3 or 4 vertices to make a face";
    public static final String CHARACTER_EDITOR_MESSAGE_FACE_FLAT = "Those vertices lie on one line";
    public static final String CHARACTER_EDITOR_MESSAGE_MERGE_VERTICES = "Select 2 or more vertices to merge";
    public static final String CHARACTER_EDITOR_MESSAGE_FLAT_SELECTION = "The selection has no size on that axis";
    public static final String CHARACTER_EDITOR_MESSAGE_NO_TEXTURE = "No texture to make the part in";
    public static final String CHARACTER_EDITOR_MESSAGE_UNDONE = "Undone";
    public static final String CHARACTER_EDITOR_MESSAGE_REDONE = "Redone";
    public static final String CHARACTER_EDITOR_MESSAGE_NOTHING_TO_UNDO = "Nothing to undo";
    public static final String CHARACTER_EDITOR_MESSAGE_NOTHING_TO_REDO = "Nothing to redo";

    // Animation Editor — clip fields
    public static final String ANIMATION_EDITOR_FIELD_RIG = "rig";
    public static final String ANIMATION_EDITOR_FIELD_LOOP = "loop";
    public static final String ANIMATION_EDITOR_FIELD_TRACKS = "tracks";
    public static final String ANIMATION_EDITOR_FIELD_TIME = "time";
    public static final String ANIMATION_EDITOR_FIELD_ROTATION = "rotation";
    public static final String ANIMATION_EDITOR_FIELD_POSITION = "position";
    public static final String ANIMATION_EDITOR_FIELD_SCALE = "scale";
    public static final String ANIMATION_EDITOR_DEFAULT_RIG = "HumanoidRig";

    // Animation Editor — panel keys; the channel rows are keyed by their fields
    public static final String ANIMATION_EDITOR_KEY_BONE = "bone";
    public static final String ANIMATION_EDITOR_KEY_CHARACTER = "character";
    public static final String ANIMATION_EDITOR_KEY_LOOP = "loop";
    public static final String ANIMATION_EDITOR_KEY_ADD_KEY = "add_key";
    public static final String ANIMATION_EDITOR_KEY_DELETE_KEY = "delete_key";
    public static final String ANIMATION_EDITOR_KEY_PREVIOUS_KEY = "previous_key";
    public static final String ANIMATION_EDITOR_KEY_NEXT_KEY = "next_key";
    public static final String ANIMATION_EDITOR_KEY_KEY_EARLIER = "key_earlier";
    public static final String ANIMATION_EDITOR_KEY_KEY_LATER = "key_later";
    public static final String ANIMATION_EDITOR_KEY_KEY_POSE = "key_pose";
    public static final String ANIMATION_EDITOR_KEY_COPY_POSE = "copy_pose";
    public static final String ANIMATION_EDITOR_KEY_PASTE_POSE = "paste_pose";
    public static final String ANIMATION_EDITOR_KEY_MIRROR_POSE = "mirror_pose";

    // Animation Editor — timing, steps and mirroring
    public static final float ANIMATION_EDITOR_FRAME_RATE = 30f;
    public static final float ANIMATION_EDITOR_TIMELINE_MIN_SPAN = 1f;
    public static final float ANIMATION_EDITOR_KEY_EPSILON = 0.0005f;
    public static final float ANIMATION_EDITOR_ROTATION_STEP = 5f;
    public static final float ANIMATION_EDITOR_POSITION_STEP = 0.005f;
    public static final float ANIMATION_EDITOR_SCALE_STEP = 0.05f;
    public static final float ANIMATION_EDITOR_SCALE_MIN = 0.01f;
    public static final String ANIMATION_EDITOR_LEFT_SUFFIX = "_l";
    public static final String ANIMATION_EDITOR_RIGHT_SUFFIX = "_r";

    // Animation Editor — panel text
    public static final String ANIMATION_EDITOR_HEADER_CLIP = "Clip";
    public static final String ANIMATION_EDITOR_HEADER_BONES = "Bones";
    public static final String ANIMATION_EDITOR_HEADER_POSE = "Whole Pose";
    public static final String ANIMATION_EDITOR_HEADER_KEY = "Key ";
    public static final String ANIMATION_EDITOR_HEADER_NO_KEY = "No key at ";
    public static final String ANIMATION_EDITOR_KEY_AT = " at ";
    public static final String ANIMATION_EDITOR_KEY_COUNT_OPEN = "  (";
    public static final String ANIMATION_EDITOR_KEY_COUNT_CLOSE = ")";
    public static final String ANIMATION_EDITOR_LABEL_CHARACTER = "Character";
    public static final String ANIMATION_EDITOR_LABEL_LOOP = "Loops";
    public static final String ANIMATION_EDITOR_LABEL_ROTATION = "Rotation";
    public static final String ANIMATION_EDITOR_LABEL_POSITION = "Position";
    public static final String ANIMATION_EDITOR_LABEL_SCALE = "Scale";
    public static final String ANIMATION_EDITOR_LABEL_ADD_KEY = "Add Key";
    public static final String ANIMATION_EDITOR_LABEL_DELETE_KEY = "Delete Key";
    public static final String ANIMATION_EDITOR_LABEL_PREVIOUS_KEY = "Previous Key";
    public static final String ANIMATION_EDITOR_LABEL_NEXT_KEY = "Next Key";
    public static final String ANIMATION_EDITOR_LABEL_KEY_EARLIER = "Key a Frame Earlier";
    public static final String ANIMATION_EDITOR_LABEL_KEY_LATER = "Key a Frame Later";
    public static final String ANIMATION_EDITOR_LABEL_KEY_POSE = "Key Pose";
    public static final String ANIMATION_EDITOR_LABEL_COPY_POSE = "Copy Pose";
    public static final String ANIMATION_EDITOR_LABEL_PASTE_POSE = "Paste Pose";
    public static final String ANIMATION_EDITOR_LABEL_MIRROR_POSE = "Mirror Pose";

    // Animation Editor — status text
    public static final String ANIMATION_EDITOR_STATUS_NO_CLIP = "No clip open."
            + " Pick an animation in the Hierarchy or press New.";
    public static final String ANIMATION_EDITOR_STATUS_NO_RIG = "This clip names no rig the engine knows";
    public static final String ANIMATION_EDITOR_STATUS_NO_CHARACTER = "No character is built on this clip's rig";
    public static final String ANIMATION_EDITOR_STATUS_PLAYING = "Playing";
    public static final String ANIMATION_EDITOR_STATUS_PAUSED = "Paused";
    public static final String ANIMATION_EDITOR_TIME_SEPARATOR = " / ";
    public static final String ANIMATION_EDITOR_TIME_SUFFIX = "s";
    public static final String ANIMATION_EDITOR_MESSAGE_CREATED = "Created ";
    public static final String ANIMATION_EDITOR_MESSAGE_SAVED = "Saved ";
    public static final String ANIMATION_EDITOR_MESSAGE_REVERTED = "Reverted ";
    public static final String ANIMATION_EDITOR_MESSAGE_DELETED = "Deleted ";
    public static final String ANIMATION_EDITOR_MESSAGE_POSE_COPIED = "Pose copied";

    // Animation Tree Editor — tree fields
    public static final String ANIMATION_TREE_EDITOR_FIELD_RIG = "rig";
    public static final String ANIMATION_TREE_EDITOR_FIELD_LAYERS = "layers";
    public static final String ANIMATION_TREE_EDITOR_FIELD_NAME = "name";
    public static final String ANIMATION_TREE_EDITOR_FIELD_MODE = "mode";
    public static final String ANIMATION_TREE_EDITOR_FIELD_WEIGHT = "weight";
    public static final String ANIMATION_TREE_EDITOR_FIELD_BLEND = "blend";
    public static final String ANIMATION_TREE_EDITOR_FIELD_DEFAULT = "default";
    public static final String ANIMATION_TREE_EDITOR_FIELD_MASK = "mask";
    public static final String ANIMATION_TREE_EDITOR_FIELD_NODES = "nodes";
    public static final String ANIMATION_TREE_EDITOR_FIELD_CLIP = "clip";
    public static final String ANIMATION_TREE_EDITOR_FIELD_CLIPS = "clips";
    public static final String ANIMATION_TREE_EDITOR_FIELD_VALUE = "value";
    public static final String ANIMATION_TREE_EDITOR_FIELD_PARAMETER = "parameter";
    public static final String ANIMATION_TREE_EDITOR_FIELD_RATE = "rate";
    public static final String ANIMATION_TREE_EDITOR_FIELD_RATE_SCALE = "rate_scale";
    public static final String ANIMATION_TREE_EDITOR_FIELD_DELAY = "delay";
    public static final String ANIMATION_TREE_EDITOR_FIELD_STATES = "states";
    public static final String ANIMATION_TREE_EDITOR_FIELD_ACTIONS = "actions";
    public static final String ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS = "transitions";
    public static final String ANIMATION_TREE_EDITOR_FIELD_FROM = "from";
    public static final String ANIMATION_TREE_EDITOR_FIELD_TO = "to";
    public static final String ANIMATION_TREE_EDITOR_BASE_LAYER = "base";

    // Animation Tree Editor — panel keys
    public static final String ANIMATION_TREE_EDITOR_KEY_CHARACTER = "character";
    public static final String ANIMATION_TREE_EDITOR_KEY_SIMULATED_STATE = "simulated_state";
    public static final String ANIMATION_TREE_EDITOR_KEY_SIMULATED_ACTION = "simulated_action";
    public static final String ANIMATION_TREE_EDITOR_KEY_PARAMETER = "parameter";
    public static final String ANIMATION_TREE_EDITOR_KEY_PLAY_ACTION = "play_action";
    public static final String ANIMATION_TREE_EDITOR_KEY_LAYER = "layer";
    public static final String ANIMATION_TREE_EDITOR_KEY_NODE = "node";
    public static final String ANIMATION_TREE_EDITOR_KEY_ADD_LAYER = "add_layer";
    public static final String ANIMATION_TREE_EDITOR_KEY_RENAME_LAYER = "rename_layer";
    public static final String ANIMATION_TREE_EDITOR_KEY_REMOVE_LAYER = "remove_layer";
    public static final String ANIMATION_TREE_EDITOR_KEY_LAYER_UP = "layer_up";
    public static final String ANIMATION_TREE_EDITOR_KEY_LAYER_DOWN = "layer_down";
    public static final String ANIMATION_TREE_EDITOR_KEY_LAYER_MODE = "layer_mode";
    public static final String ANIMATION_TREE_EDITOR_KEY_LAYER_WEIGHT = "layer_weight";
    public static final String ANIMATION_TREE_EDITOR_KEY_LAYER_BLEND = "layer_blend";
    public static final String ANIMATION_TREE_EDITOR_KEY_LAYER_DEFAULT = "layer_default";
    public static final String ANIMATION_TREE_EDITOR_KEY_ADD_NODE = "add_node";
    public static final String ANIMATION_TREE_EDITOR_KEY_RENAME_NODE = "rename_node";
    public static final String ANIMATION_TREE_EDITOR_KEY_REMOVE_NODE = "remove_node";
    public static final String ANIMATION_TREE_EDITOR_KEY_NODE_KIND = "node_kind";
    public static final String ANIMATION_TREE_EDITOR_KEY_NODE_CLIP = "node_clip";
    public static final String ANIMATION_TREE_EDITOR_KEY_NODE_PARAMETER = "node_parameter";
    public static final String ANIMATION_TREE_EDITOR_KEY_NODE_RATE = "node_rate";
    public static final String ANIMATION_TREE_EDITOR_KEY_NODE_RATE_SCALE = "node_rate_scale";
    public static final String ANIMATION_TREE_EDITOR_KEY_NODE_BLEND = "node_blend";
    public static final String ANIMATION_TREE_EDITOR_KEY_NODE_DELAY = "node_delay";
    public static final String ANIMATION_TREE_EDITOR_KEY_SPACE_CLIP = "space_clip";
    public static final String ANIMATION_TREE_EDITOR_KEY_SPACE_VALUE = "space_value";
    public static final String ANIMATION_TREE_EDITOR_KEY_ADD_SPACE_CLIP = "add_space_clip";
    public static final String ANIMATION_TREE_EDITOR_KEY_REMOVE_SPACE_CLIP = "remove_space_clip";
    public static final String ANIMATION_TREE_EDITOR_KEY_STATE = "state";
    public static final String ANIMATION_TREE_EDITOR_KEY_ACTION = "action";
    public static final String ANIMATION_TREE_EDITOR_KEY_MASK = "mask";
    public static final String ANIMATION_TREE_EDITOR_KEY_CLEAR_MASK = "clear_mask";
    public static final String ANIMATION_TREE_EDITOR_KEY_ADD_TRANSITION = "add_transition";
    public static final String ANIMATION_TREE_EDITOR_KEY_REMOVE_TRANSITION = "remove_transition";
    public static final String ANIMATION_TREE_EDITOR_KEY_TRANSITION_FROM = "transition_from";
    public static final String ANIMATION_TREE_EDITOR_KEY_TRANSITION_TO = "transition_to";
    public static final String ANIMATION_TREE_EDITOR_KEY_TRANSITION_BLEND = "transition_blend";

    // Animation Tree Editor — steps and ranges; the parameter steps follow AnimationParameter's order
    public static final float ANIMATION_TREE_EDITOR_WEIGHT_FULL = 1f;
    public static final float ANIMATION_TREE_EDITOR_WEIGHT_STEP = 0.05f;
    public static final float ANIMATION_TREE_EDITOR_BLEND_STEP = 0.02f;
    public static final float ANIMATION_TREE_EDITOR_RATE_STEP = 0.05f;
    public static final float ANIMATION_TREE_EDITOR_RATE_MIN = 0.05f;
    public static final float ANIMATION_TREE_EDITOR_LOOK_PITCH_LIMIT = 90f;
    public static final float ANIMATION_TREE_EDITOR_LOOK_YAW_LIMIT = 180f;
    public static final float[] ANIMATION_TREE_EDITOR_PARAMETER_STEPS = { 0.5f, 0.5f, 30f, 10f, 15f };

    // Animation Tree Editor — views and panel text
    public static final String ANIMATION_TREE_EDITOR_VIEW_LAYER = "Layer";
    public static final String ANIMATION_TREE_EDITOR_VIEW_NODE = "Node";
    public static final String ANIMATION_TREE_EDITOR_VIEW_STATES = "States";
    public static final String ANIMATION_TREE_EDITOR_VIEW_MASK = "Mask";
    public static final String ANIMATION_TREE_EDITOR_VIEW_TRANSITIONS = "Transitions";
    public static final String ANIMATION_TREE_EDITOR_HEADER_SIMULATE = "Simulate";
    public static final String ANIMATION_TREE_EDITOR_HEADER_LAYERS = "Layers";
    public static final String ANIMATION_TREE_EDITOR_HEADER_NODES = "Nodes";
    public static final String ANIMATION_TREE_EDITOR_HEADER_BLEND = "Blend";
    public static final String ANIMATION_TREE_EDITOR_HEADER_PLAYBACK = "Playback";
    public static final String ANIMATION_TREE_EDITOR_HEADER_SPACE_CLIP = "Blend Clip ";
    public static final String ANIMATION_TREE_EDITOR_HEADER_STATES = "Movement States";
    public static final String ANIMATION_TREE_EDITOR_HEADER_ACTIONS = "Actions";
    public static final String ANIMATION_TREE_EDITOR_HEADER_MASK = "Bone Weights";
    public static final String ANIMATION_TREE_EDITOR_HEADER_NO_MASK = "Bone Weights (no mask: whole body)";
    public static final String ANIMATION_TREE_EDITOR_HEADER_BASE_MASK = "The base layer always moves the whole body";
    public static final String ANIMATION_TREE_EDITOR_HEADER_TRANSITION = "Transition ";
    public static final String ANIMATION_TREE_EDITOR_KIND_SINGLE = "Single Clip";
    public static final String ANIMATION_TREE_EDITOR_KIND_BLEND_SPACE = "Blend Space";
    public static final String ANIMATION_TREE_EDITOR_LABEL_BASE = "base";
    public static final String ANIMATION_TREE_EDITOR_LABEL_OVERRIDE = "override";
    public static final String ANIMATION_TREE_EDITOR_LABEL_STATE = "State";
    public static final String ANIMATION_TREE_EDITOR_LABEL_ACTION = "Action";
    public static final String ANIMATION_TREE_EDITOR_LABEL_PLAY_ACTION = "Play Action";
    public static final String ANIMATION_TREE_EDITOR_LABEL_ADD_LAYER = "Add Layer";
    public static final String ANIMATION_TREE_EDITOR_LABEL_RENAME_LAYER = "Rename Layer";
    public static final String ANIMATION_TREE_EDITOR_LABEL_REMOVE_LAYER = "Remove Layer";
    public static final String ANIMATION_TREE_EDITOR_LABEL_LAYER_UP = "Move Layer Up";
    public static final String ANIMATION_TREE_EDITOR_LABEL_LAYER_DOWN = "Move Layer Down";
    public static final String ANIMATION_TREE_EDITOR_LABEL_MODE = "Mode";
    public static final String ANIMATION_TREE_EDITOR_LABEL_WEIGHT = "Weight";
    public static final String ANIMATION_TREE_EDITOR_LABEL_BLEND = "Blend";
    public static final String ANIMATION_TREE_EDITOR_LABEL_DEFAULT = "Default Node";
    public static final String ANIMATION_TREE_EDITOR_LABEL_ADD_NODE = "Add Node";
    public static final String ANIMATION_TREE_EDITOR_LABEL_RENAME_NODE = "Rename Node";
    public static final String ANIMATION_TREE_EDITOR_LABEL_REMOVE_NODE = "Remove Node";
    public static final String ANIMATION_TREE_EDITOR_LABEL_KIND = "Kind";
    public static final String ANIMATION_TREE_EDITOR_LABEL_CLIP = "Clip";
    public static final String ANIMATION_TREE_EDITOR_LABEL_VALUE = "Value";
    public static final String ANIMATION_TREE_EDITOR_LABEL_PARAMETER = "Parameter";
    public static final String ANIMATION_TREE_EDITOR_LABEL_RATE = "Rate";
    public static final String ANIMATION_TREE_EDITOR_LABEL_RATE_SCALE = "Rate Scale";
    public static final String ANIMATION_TREE_EDITOR_LABEL_DELAY = "Delay";
    public static final String ANIMATION_TREE_EDITOR_LABEL_ADD_SPACE_CLIP = "Add Blend Clip";
    public static final String ANIMATION_TREE_EDITOR_LABEL_REMOVE_SPACE_CLIP = "Remove Blend Clip";
    public static final String ANIMATION_TREE_EDITOR_LABEL_CLEAR_MASK = "Clear Mask";
    public static final String ANIMATION_TREE_EDITOR_LABEL_ADD_TRANSITION = "Add Transition";
    public static final String ANIMATION_TREE_EDITOR_LABEL_REMOVE_TRANSITION = "Remove Transition";
    public static final String ANIMATION_TREE_EDITOR_LABEL_FROM = "From";
    public static final String ANIMATION_TREE_EDITOR_LABEL_TO = "To";

    // Animation Tree Editor — status text
    public static final String ANIMATION_TREE_EDITOR_STATUS_NO_TREE = "No tree open."
            + " Pick an animation tree in the Hierarchy or press New.";
    public static final String ANIMATION_TREE_EDITOR_STATUS_NO_RIG = "This tree names no rig the engine knows";
    public static final String ANIMATION_TREE_EDITOR_STATUS_NO_CHARACTER = "No character is built on this tree's rig";
    public static final String ANIMATION_TREE_EDITOR_MESSAGE_CREATED = "Created ";
    public static final String ANIMATION_TREE_EDITOR_MESSAGE_SAVED = "Saved ";
    public static final String ANIMATION_TREE_EDITOR_MESSAGE_REVERTED = "Reverted ";
    public static final String ANIMATION_TREE_EDITOR_MESSAGE_DELETED = "Deleted ";
    public static final String ANIMATION_TREE_EDITOR_MESSAGE_LAST_LAYER = "A tree needs at least its base layer";
    public static final String ANIMATION_TREE_EDITOR_MESSAGE_NO_ACTION = "No layer plays the action ";

    // Texture Viewer
    public static final String TEXTURE_VIEWER_STATUS_HOVERED = "Texture: ";
    public static final String TEXTURE_VIEWER_STATUS_NO_BRUSH = "No brush. Click a texture to paint items with it.";

    // World Map
    public static final String WORLD_MAP_STATUS_X = "X ";
    public static final String WORLD_MAP_STATUS_Z = "   Z ";
    public static final String WORLD_MAP_STATUS_SEPARATOR = "   |   ";
    public static final String WORLD_MAP_STATUS_SCALE = "Blocks per pixel ";
    public static final String WORLD_MAP_STATUS_SCALE_FORMAT = "%.3g";
    public static final String WORLD_MAP_FOLLOW_ON = "Following";
    public static final String WORLD_MAP_FOLLOW_OFF = "Follow";
    public static final String WORLD_MAP_DAY_NIGHT_ON = "Day/Night On";
    public static final String WORLD_MAP_DAY_NIGHT_OFF = "Day/Night Off";
    public static final String WORLD_MAP_WEATHER_ON = "Weather On";
    public static final String WORLD_MAP_WEATHER_OFF = "Weather Off";

    // Settlement Planner
    public static final int SETTLEMENT_PLANNER_LAYOUT_DEFAULT = 1;
    public static final String SETTLEMENT_PLANNER_ARCHITECTURE_BIOME = "Biome's choice";
    public static final String SETTLEMENT_PLANNER_MESSAGE_TYPE_LIVE = "Settlement live: ";
    public static final String SETTLEMENT_PLANNER_MESSAGE_TYPE_REFUSED = "Settlement not applied: ";
    public static final String SETTLEMENT_PLANNER_MESSAGE_RELEASED = "No longer editing a placement";
    public static final String SETTLEMENT_PLANNER_MESSAGE_NOTHING_EDITED =
            "Select a placement in the World Editor first";
    public static final String SETTLEMENT_PLANNER_MESSAGE_MOVED = "Moved";
    public static final String SETTLEMENT_PLANNER_MESSAGE_REMOVED = "Removed ";
    public static final String SETTLEMENT_PLANNER_MESSAGE_UNBUILDABLE = "No architecture builds ";
    public static final String SETTLEMENT_PLANNER_MESSAGE_NO_ARCHITECTURE = "A structure has no architecture to choose";
    public static final String SETTLEMENT_PLANNER_MESSAGE_NO_LAYOUT = "Only a settlement has layouts";
    public static final String SETTLEMENT_PLANNER_MESSAGE_NO_TURN = "A settlement is not turned; choose another layout";
    public static final String SETTLEMENT_PLANNER_MESSAGE_UNKNOWN = "The planner has nothing named ";
    public static final String SETTLEMENT_PLANNER_MESSAGE_EDITING = "Editing ";
    public static final String SETTLEMENT_PLANNER_MESSAGE_NOT_APPLIED = "Nothing buildable is chosen";
    public static final String SETTLEMENT_PLANNER_STATUS_SEPARATOR = "  |  ";
    public static final String SETTLEMENT_PLANNER_STATUS_COLUMN_X = "  X ";
    public static final String SETTLEMENT_PLANNER_STATUS_Z = "  Z ";
    public static final String SETTLEMENT_PLANNER_STATUS_NOTHING = "Nothing to design";
    public static final String SETTLEMENT_PLANNER_STATUS_NOTHING_EDITED =
            "Nothing selected: drag a catalogue row onto the World Editor, or select there";
    public static final String SETTLEMENT_PLANNER_STATUS_TURNS = "Turns ";
    public static final String SETTLEMENT_PLANNER_STATUS_NO_ARCHITECTURE = "No architecture";
    public static final String SETTLEMENT_PLANNER_STATUS_CELL_LAYOUT = "Cell layout";
    public static final String SETTLEMENT_PLANNER_STATUS_LAYOUT = "Layout ";
    public static final String SETTLEMENT_PLANNER_STATUS_PLANNING = "Planning...";
    public static final String SETTLEMENT_PLANNER_STATUS_BUILDINGS = "Buildings ";
    public static final String SETTLEMENT_PLANNER_STATUS_ROADS = ", roads ";
    public static final String SETTLEMENT_PLANNER_STATUS_WALLED = ", walled";

    // Placement Icons — one icon per hand placement on every map, a house for a settlement and a banner turned the
    // way a structure faces, the placement the Settlement Planner edits in its highlight
    public static final String PLACEMENT_ICON_MESH = "editor/EditorViewportQuad";
    public static final String PLACEMENT_ICON_MATERIAL = "editor/PlacementIconMaterial";
    public static final String PLACEMENT_ICON_UNIFORM_SHAPE = "u_iconShape";
    public static final String PLACEMENT_ICON_UNIFORM_FILL_COLOR = "u_fillColor";
    public static final String PLACEMENT_ICON_UNIFORM_CENTER = "u_markerCenter";
    public static final String PLACEMENT_ICON_UNIFORM_DIRECTION = "u_markerDirection";
    public static final String PLACEMENT_ICON_UNIFORM_SCALE = "u_markerScale";
    public static final float PLACEMENT_ICON_SHAPE_SETTLEMENT = 0f;
    public static final float PLACEMENT_ICON_SHAPE_STRUCTURE = 1f;
    public static final float PLACEMENT_ICON_SIZE_PIXELS = 12f;
    public static final float PLACEMENT_ICON_EDITED_SIZE_PIXELS = 16f;
    public static final float[] PLACEMENT_ICON_COLOR_SETTLEMENT = { 0.95f, 0.78f, 0.32f, 1f };
    public static final float[] PLACEMENT_ICON_COLOR_STRUCTURE = { 0.45f, 0.72f, 0.95f, 1f };
    public static final float[] PLACEMENT_ICON_COLOR_EDITED = { 1f, 0.42f, 0.32f, 1f };

    // Image Editing
    public static final String IMAGE_FORMAT = "png";
    public static final String IMAGE_TEMP_SUFFIX = ".tmp";
    public static final int IMAGE_HISTORY_MAX_STEPS = 64;
    public static final long IMAGE_HISTORY_MAX_PIXELS = 16777216L;
    public static final int IMAGE_SMOOTH_REACH_PIXELS = 1;
    public static final float IMAGE_BRUSH_EDGE_PIXELS = 0.5f;
    public static final int IMAGE_BRUSH_RANDOM_SEED = 0x2545F491;
    public static final float IMAGE_BRUSH_RANDOM_SCALE = 1f / (1 << 24);

    // Brushes — presets load from this directory, the default one standing in for any deleted
    public static final String BRUSH_PATH = "brushes";
    public static final String BRUSH_DEFAULT = "Basic";
    public static final String BRUSH_MESSAGE_REFUSED = "Brush not applied: ";
    public static final String BRUSH_SHAPE_ROUND = "Round";
    public static final String BRUSH_SHAPE_SQUARE = "Square";
    public static final String BRUSH_SHAPE_DIAMOND = "Diamond";
    public static final String BRUSH_MODE_PAINT = "Paint";
    public static final String BRUSH_MODE_SMOOTH = "Smooth";
    public static final String BRUSH_MODE_SWIRL = "Swirl";
    public static final String BRUSH_MODE_SCATTER = "Scatter";
    public static final String BRUSH_KEY_HARDNESS = "hardness";
    public static final String BRUSH_KEY_OPACITY = "opacity";
    public static final String BRUSH_KEY_DENSITY = "density";
    public static final String BRUSH_KEY_SPACING = "spacing";
    public static final String BRUSH_KEY_TWIST = "twist";
    public static final String BRUSH_LABEL_HARDNESS = "Hardness";
    public static final String BRUSH_LABEL_OPACITY = "Opacity";
    public static final String BRUSH_LABEL_DENSITY = "Density";
    public static final String BRUSH_LABEL_SPACING = "Spacing";
    public static final String BRUSH_LABEL_TWIST = "Twist";
    public static final float BRUSH_UNIT_STEP = 0.05f;
    public static final float BRUSH_TWIST_STEP = 15f;
    public static final float BRUSH_TWIST_MAX = 360f;
    public static final float BRUSH_SPACING_MIN = 0.05f;
    public static final float BRUSH_HARDNESS_DEFAULT = 1f;
    public static final float BRUSH_OPACITY_DEFAULT = 1f;
    public static final float BRUSH_DENSITY_DEFAULT = 1f;
    public static final float BRUSH_SPACING_DEFAULT = 0.1f;
    public static final float BRUSH_TWIST_DEFAULT = 90f;
    public static final double BRUSH_VALUE_PRECISION = 100.0;

    // Tools — sizes are brush radii, in image pixels on the world's layers and window pixels on a character
    public static final String TOOL_SELECT = "Select";
    public static final String TOOL_BRUSH = "Brush";
    public static final String TOOL_ERASE = "Erase";
    public static final String TOOL_FILL = "Fill";
    public static final String TOOL_PICK = "Pick";
    public static final String TOOL_PLACE = "Place";
    public static final String TOOL_WALL = "Wall";
    public static final String TOOL_CANVAS_BIOMES = "World Biomes";
    public static final String TOOL_CANVAS_HEIGHTS = "World Heights";
    public static final String TOOL_CANVAS_SETTLEMENTS = "World Placements";
    public static final String TOOL_CANVAS_CHARACTER_MESH = "Character Mesh";
    public static final String TOOL_CANVAS_CHARACTER_WEIGHTS = "Character Weights";
    public static final String TOOL_CANVAS_ITEM = "Item";
    public static final float TOOL_IMAGE_SIZE_MIN = 0f;
    public static final float TOOL_IMAGE_SIZE_MAX = 32f;
    public static final float TOOL_IMAGE_SIZE_STEP = 1f;
    public static final float TOOL_IMAGE_SIZE_DEFAULT = 1f;
    public static final float TOOL_SCREEN_SIZE_MIN = 4f;
    public static final float TOOL_SCREEN_SIZE_MAX = 400f;
    public static final float TOOL_SCREEN_SIZE_STEP = 5f;
    public static final float TOOL_SCREEN_SIZE_DEFAULT = 40f;
    public static final String TOOL_STATUS_SIZE = "Size ";
    public static final String TOOL_MESSAGE_MODE_REFUSED = " brushes cannot stroke here";

    // World Editor
    public static final String WORLD_EDITOR_MODE_PAINT = "Paint";
    public static final String WORLD_EDITOR_MODE_SETTLEMENTS = "Settlements";
    public static final String WORLD_EDITOR_LAYER_BIOMES = "Biomes";
    public static final String WORLD_EDITOR_LAYER_HEIGHTS = "Heights";
    public static final int WORLD_EDITOR_HEIGHT_LEVEL_STEP = 16;
    public static final float WORLD_EDITOR_HEIGHT_PERCENT_SCALE = 100f;
    public static final String WORLD_EDITOR_HEIGHT_RAISE = "Raise ";
    public static final String WORLD_EDITOR_HEIGHT_LOWER = "Lower ";
    public static final String WORLD_EDITOR_HEIGHT_PERCENT = "%";
    public static final String WORLD_EDITOR_HEIGHT_NEUTRAL = "Neutral";
    public static final double WORLD_EDITOR_CLEAR_RADIUS_BLOCKS = 384.0;
    public static final int WORLD_EDITOR_PLACEMENT_REACH_PIXELS = 5;
    public static final String WORLD_EDITOR_STATUS_NO_PLACEABLE = "Nothing to place";
    public static final String WORLD_EDITOR_MESSAGE_PLACED = "Placed ";
    public static final String WORLD_EDITOR_MESSAGE_CLEARED = "Cleared ";
    public static final String WORLD_EDITOR_MESSAGE_NOTHING_NEAR = "No placement near the pointer";
    public static final String WORLD_EDITOR_MESSAGE_MOVED = "Moved the selection";
    public static final String WORLD_EDITOR_MESSAGE_SELECTED = " selected";
    public static final String WORLD_EDITOR_MESSAGE_DELETED = "Deleted ";
    public static final String WORLD_EDITOR_MESSAGE_NOTHING_SELECTED = "Nothing selected";
    public static final String WORLD_EDITOR_MESSAGE_CELL_HELD = "This settlement cell already holds ";
    public static final String WORLD_EDITOR_MESSAGE_PLACEMENT_GONE = "The edited placement no longer stands";
    public static final String WORLD_EDITOR_MESSAGE_PLACEMENTS_RELOAD_FAILED = "Could not reload placements of ";
    public static final String WORLD_EDITOR_PLAYERS_ON = "Players On";
    public static final String WORLD_EDITOR_PLAYERS_OFF = "Players Off";
    public static final String WORLD_EDITOR_DIRTY_MARKER = "*";
    public static final String WORLD_EDITOR_STATUS_SEPARATOR = "  |  ";
    public static final String WORLD_EDITOR_STATUS_NO_BIOME = "No biome selected";
    public static final String WORLD_EDITOR_STATUS_X = "X ";
    public static final String WORLD_EDITOR_STATUS_Y = "  Y ";
    public static final String WORLD_EDITOR_STATUS_UNPAINTED = "Unpainted color";
    public static final String WORLD_EDITOR_MESSAGE_SAVED = "Saved ";
    public static final String WORLD_EDITOR_MESSAGE_SAVE_FAILED = "Could not save ";
    public static final String WORLD_EDITOR_MESSAGE_RELOADED = "Reloaded ";
    public static final String WORLD_EDITOR_MESSAGE_RELOAD_FAILED = "Could not reload ";
    public static final String WORLD_EDITOR_MESSAGE_NOTHING_TO_UNDO = "Nothing to undo";
    public static final String WORLD_EDITOR_MESSAGE_NOTHING_TO_REDO = "Nothing to redo";
    public static final String WORLD_EDITOR_MESSAGE_PICKED = "Picked ";
    public static final String WORLD_EDITOR_MESSAGE_UNKNOWN_COLOR = "No biome paints this color";
    public static final String WORLD_EDITOR_MESSAGE_BIOME_LIVE = "Biome live: ";
    public static final String WORLD_EDITOR_MESSAGE_BIOME_REFUSED = "Biome not applied: ";
    public static final String WORLD_EDITOR_MESSAGE_BIOME_RETIRED = "Biome retired: ";
    public static final String WORLD_EDITOR_MESSAGE_TREE_LIVE = "Tree live: ";
    public static final String WORLD_EDITOR_MESSAGE_TREE_REFUSED = "Tree not applied: ";

    // Biome Planner — a grid this many cells a side, each cell this many world image pixels a side
    public static final String BIOME_PLANNER_WORLD_NAME = "EditorBiomePlanner";
    public static final int BIOME_PLANNER_GRID_SIDE = 3;
    public static final int BIOME_PLANNER_CELL_PIXELS = 4;
    public static final int BIOME_PLANNER_EMPTY_COLOR = 0x000000FF;
    public static final String BIOME_PLANNER_STATUS_CENTRE = "Centre: ";
    public static final String BIOME_PLANNER_MESSAGE_CENTRE = "The centre is always the selected biome";
    public static final String BIOME_PLANNER_MESSAGE_PAINTED = "Painted ";
    public static final String BIOME_PLANNER_MESSAGE_RESET = "Every cell follows the centre";

    // Item Library
    public static final String ITEM_EDITOR_MESH_DIRECTORY = "items";
    public static final String ITEM_EDITOR_DEFINITION_FILE = "EditorItems";
    public static final String ITEM_EDITOR_TEXTURE_ARRAY = "items/standard";
    public static final String ITEM_EDITOR_DEFAULT_PART_NAME = "Body";

    // Info Schemas
    public static final String INFO_SCHEMA_PATH = "schemas";
    public static final String INFO_SCHEMA_BIOMES = "Biomes";
    public static final String INFO_SCHEMA_ITEMS = "Items";
    public static final String INFO_SCHEMA_TREES = "Trees";
    public static final String INFO_SCHEMA_ENTITIES = "Entities";
    public static final String INFO_SCHEMA_ANIMATIONS = "Animations";
    public static final String INFO_SCHEMA_ANIMATION_TREES = "AnimationTrees";
    public static final String INFO_SCHEMA_WORLDS = "Worlds";
    public static final String INFO_SCHEMA_SHIPS = "Ships";
    public static final String INFO_SCHEMA_SHORTCUTS = "Shortcuts";
    public static final String INFO_SCHEMA_SETTLEMENTS = "Settlements";
    public static final String INFO_SCHEMA_STRUCTURES = "Structures";
    public static final String INFO_SCHEMA_BRUSHES = "Brushes";
    public static final String INFO_ITEM_MESH_FIELD = "mesh";
    public static final String INFO_FOLDER_SEPARATOR = "/";
    public static final String INFO_PATH_SEPARATOR = "|";
    public static final String[] INFO_SUMMARY_FIELDS = { "name", "block", "clip", "time" };

    // Info Hierarchy
    public static final String HIERARCHY_FOLDER_KEY_PREFIX = "folder:";
    public static final String HIERARCHY_FILE_KEY_PREFIX = "file:";
    public static final String HIERARCHY_ENTRY_KEY_PREFIX = "entry:";
    public static final String HIERARCHY_KEY_SEPARATOR = "|";

    // Info Text
    public static final String INFO_DIRTY_MARKER = "*";
    public static final String INFO_TITLE_SEPARATOR = "  >  ";
    public static final String INFO_INDEX_OPEN = "[";
    public static final String INFO_INDEX_CLOSE = "]";
    public static final String INFO_SUMMARY_SEPARATOR = "  ";
    public static final String INFO_GROUP_OBJECT_SUFFIX = "  { }";
    public static final String INFO_GROUP_ARRAY_OPEN = "  [";
    public static final String INFO_GROUP_ARRAY_CLOSE = "]";
    public static final String INFO_GROUP_MAP_OPEN = "  {";
    public static final String INFO_GROUP_MAP_CLOSE = "}";
    public static final int INFO_VALUE_PREVIEW_LENGTH = 48;
    public static final String INFO_VALUE_ELLIPSIS = "...";
    public static final String INFO_STATUS_NO_SELECTION = "Select an entry in the Hierarchy, or pick a tab and New.";
    public static final String INFO_STATUS_FILE_SELECTED = "Select an entry in this file, or press New to add one.";
    public static final String INFO_STATUS_FOLDER_SELECTED = "New creates its file inside this folder.";
    public static final String INFO_MESSAGE_SAVED = "Saved ";
    public static final String INFO_MESSAGE_SAVED_ALL = "Saved every changed file";
    public static final String INFO_MESSAGE_NOTHING_TO_SAVE = "Nothing to save";
    public static final String INFO_MESSAGE_REVERTED = "Reverted ";
    public static final String INFO_MESSAGE_CREATED = "Created ";
    public static final String INFO_MESSAGE_DELETED = "Deleted ";
    public static final String INFO_MESSAGE_COPIED = "Copied ";
    public static final String INFO_MESSAGE_COPY_ABORTED = "Copy aborted: the source or a unique value changed";
    public static final String INFO_MESSAGE_REQUIRED = "Required fields cannot be removed";
    public static final String INFO_MESSAGE_FIXED_LENGTH = "This list has a fixed length";
    public static final String INFO_MESSAGE_SAVED_OF = " of ";
    public static final String INFO_MESSAGE_SAVED_SUFFIX = " changed files  |  ";
    public static final String INFO_STATUS_PROBLEM = " problem  |  ";
    public static final String INFO_STATUS_PROBLEMS = " problems  |  ";
    public static final String INFO_STATUS_IGNORED = " key the game ignores  |  ";
    public static final String INFO_STATUS_IGNORED_PLURAL = " keys the game ignores  |  ";
    public static final String INFO_ISSUE_SEPARATOR = ": ";
    public static final String INFO_ISSUE_PATH_SEPARATOR = ".";

    // Info Rules
    public static final String INFO_REFERENCE_FORM_PATH = "path";
    public static final String INFO_REFERENCE_FORM_NAME = "name";
    public static final String INFO_REFERENCE_SEPARATOR = "/";
    public static final String INFO_LOCAL_SEPARATOR = ".";
    public static final String INFO_LOCAL_ANY_ELEMENT = "[]";
    public static final String INFO_LOCAL_SAME_ELEMENT = "[.]";
    public static final String INFO_LOCAL_MAP_KEYS = "{}";
    public static final String INFO_TILE_ALIAS_SEPARATOR = "_";
    public static final String INFO_VALUE_KEY_STRING = "s:";
    public static final String INFO_VALUE_KEY_OTHER = "v:";

    // Info Problems
    public static final String INFO_PROBLEM_MISSING = "Required: the game cannot load this entry without it";
    public static final String INFO_PROBLEM_UNKNOWN_FIELD = "Ignored by the game; kept as it is";
    public static final String INFO_PROBLEM_UNKNOWN_KEY = "Ignored by the game; kept as it is: ";
    public static final String INFO_PROBLEM_NO_ENTRIES = "Holds no list of entries under ";
    public static final String INFO_PROBLEM_ENTRY_NOT_OBJECT = "Holds a value that is not an entry at index ";
    public static final String INFO_PROBLEM_ENTRY_UNNAMED = "Holds an entry without a name at index ";
    public static final String INFO_PROBLEM_NAME_IN_FILE = "Another entry in this file has this name";
    public static final String INFO_PROBLEM_NAME_IN_SCHEMA = "Another entry has this name in ";
    public static final String INFO_PROBLEM_UNIQUE = "Another entry already uses this value";
    public static final String INFO_PROBLEM_TYPE = "Must be of type ";
    public static final String INFO_PROBLEM_WHOLE_NUMBER = "Must be a whole number";
    public static final String INFO_PROBLEM_ENUM = "Must be one of: ";
    public static final String INFO_PROBLEM_LIST_SEPARATOR = ", ";
    public static final String INFO_PROBLEM_LENGTH_PREFIX = "Must hold exactly ";
    public static final String INFO_PROBLEM_LENGTH_SUFFIX = " values";
    public static final String INFO_PROBLEM_DISTINCT = "Repeats a value an earlier element already holds";
    public static final String INFO_PROBLEM_MUST_BE = "Must be ";
    public static final String INFO_PROBLEM_BOUND_FROM = "from ";
    public static final String INFO_PROBLEM_BOUND_TO = " to ";
    public static final String INFO_PROBLEM_BOUND_AT_LEAST = "at least ";
    public static final String INFO_PROBLEM_BOUND_AT_MOST = "at most ";
    public static final String INFO_PROBLEM_BOUND_ABOVE = "above ";
    public static final String INFO_PROBLEM_BOUND_BELOW = "below ";
    public static final String INFO_PROBLEM_BOUND_AND = " and ";
    public static final String INFO_PROBLEM_BELOW_FIELD = "Must not be below ";
    public static final String INFO_PROBLEM_CONFLICT = "Cannot be set together with ";
    public static final String INFO_PROBLEM_MIN_COUNT_PREFIX = "Must hold at least ";
    public static final String INFO_PROBLEM_MAX_COUNT_PREFIX = "Must hold at most ";
    public static final String INFO_PROBLEM_REQUIRED_NAME = "The game cannot start without an entry named ";
    public static final String INFO_PROBLEM_NAMES_NOTHING = "Names nothing: choose one of ";
    public static final String INFO_PROBLEM_QUOTE = "'";
    public static final String INFO_PROBLEM_NOT_IN = "' is not in ";
    public static final String INFO_PROBLEM_NO_FILE = "' is not a file in ";
    public static final String INFO_PROBLEM_NOT_LOCAL = "' is not one of this entry's ";

    // Info Refusals
    public static final String INFO_REFUSAL_NONE = "";
    public static final String INFO_REFUSAL_PREFIX = "Not saved: ";
    public static final String INFO_REFUSAL_HAS = " has ";
    public static final String INFO_REFUSAL_NAMES = " names '";
    public static final String INFO_REFUSAL_IN = "' in ";
    public static final String INFO_REFUSAL_NOT_SAVED = ", which is not saved yet; save it first or use Save All";
    public static final String INFO_REFUSAL_TAKES = " would take away '";
    public static final String INFO_REFUSAL_STILL_NAMED = "', which is still named by ";
    public static final String INFO_REFUSAL_AT = " at ";
    public static final String INFO_REFUSAL_DELETE_PREFIX = "Not deleted: '";
    public static final String INFO_REFUSAL_REQUIRED = "', which the game cannot start without";

    // Item Status
    public static final String ITEM_EDITOR_DIRTY_MARKER = "*";
    public static final String ITEM_EDITOR_STATUS_SEPARATOR = "  |  ";
    public static final String ITEM_EDITOR_STATUS_BRUSH = "Brush: ";
    public static final String ITEM_EDITOR_STATUS_NO_ITEM = "No item open. Pick one in the Hierarchy or press New.";
    public static final String ITEM_EDITOR_TEXTURE_SEPARATOR = "  :  ";
    public static final String ITEM_EDITOR_MESSAGE_SAVED = "Saved ";
    public static final String ITEM_EDITOR_MESSAGE_RELOADED = "Reloaded ";
    public static final String ITEM_EDITOR_MESSAGE_CREATED = "Created ";
    public static final String ITEM_EDITOR_MESSAGE_DELETED = "Deleted ";
    public static final String ITEM_EDITOR_MESSAGE_CONVERTED = "Converted from a quad mesh: save to keep it";
    public static final String ITEM_EDITOR_MESSAGE_EMPTY = "Nothing to save: the item has no cubes or walls";
    public static final String ITEM_EDITOR_MESSAGE_NOT_SAVED = "Not saved yet: nothing to reload";
    public static final String ITEM_EDITOR_MESSAGE_NEW_MESH = "No mesh file yet: save to create it";
    public static final String ITEM_EDITOR_MESSAGE_NO_MESH = "This item names no mesh: set its mesh in the Info Panel";
    public static final String ITEM_EDITOR_MESSAGE_RENAMED = "Renamed part to ";
    public static final String ITEM_EDITOR_MESSAGE_MESH_LIMIT = "Edit refused: the item would pass the vertex limit";
    public static final String ITEM_EDITOR_MESSAGE_LAST_PART = "An item needs at least one part";
    public static final String ITEM_EDITOR_MESSAGE_PART_LIMIT = "An item cannot hold any more parts";

    // Recorder — files, in the game's own directory; videos go in their own folder beside the recordings
    public static final String RECORDER_DIRECTORY = "Recordings";
    public static final String RECORDER_VIDEO_DIRECTORY = "Videos";
    public static final String RECORDER_DEFAULT_NAME_PREFIX = "Recording";
    public static final String RECORDER_TAKE_NAME_PREFIX = "Take ";
    public static final String RECORDER_PLAYER_DISPLAY_NAME = "Player";
    public static final int RECORDER_FIRST_NAME_NUMBER = 1;
    public static final int RECORDER_NAME_MAX_LENGTH = 32;
    public static final int RECORDER_LABEL_MAX_LENGTH = 48;
    public static final String RECORDER_VIDEO_NAME_SEPARATOR = "_";
    public static final String RECORDER_EXTENSION_SEPARATOR = ".";
    public static final int RECORDER_FILE_VERSION = 2;
    public static final int RECORDER_FILE_VERSION_LAYERS = 1;
    public static final String RECORDER_FILE_CHANNEL_WORLD = "world";

    // Recorder — the tab's menu, and the lanes its drags land on and the ruler and graph its wheel zooms, which
    // every Recorder tab shares
    public static final String RECORDER_MENU = "editor/Recorder/Recorder";
    public static final int RECORDER_ENTRY_CLIP_LANES = 5;
    public static final int RECORDER_ENTRY_RULER = 6;
    public static final int RECORDER_ENTRY_KEY_ROWS = 16;
    public static final int RECORDER_ENTRY_KEY_GRAPH = 17;

    // Recorder — channels
    public static final String RECORDER_CHANNEL_LABEL_CAMERA = "Camera";
    public static final String RECORDER_CHANNEL_LABEL_PLAYER = "Player";
    public static final String RECORDER_CHANNEL_LABEL_CHARACTERS = "Characters";
    public static final String RECORDER_CHANNEL_LABEL_ITEMS = "Items";
    public static final String RECORDER_CHANNEL_LABEL_VEHICLES = "Vehicles";
    public static final String RECORDER_CHANNEL_LABEL_WEATHER = "Weather";
    public static final String RECORDER_CHANNEL_LABEL_TIME = "Time and light";
    public static final String RECORDER_CHANNEL_LABEL_INTERFACE = "Interface";

    // Recorder — the timeline; clips stack on a fixed number of lanes, the highest playing over the rest, and the
    // timeline runs on past the recording's end so there is room to lay a clip after it
    public static final int RECORDER_LANE_COUNT = 6;
    public static final float RECORDER_MIN_TIMELINE_SECONDS = 1f;
    public static final float RECORDER_TIMELINE_TAIL_SECONDS = 2f;
    public static final float RECORDER_TIMELINE_TAIL_SHARE = 0.1f;
    public static final float RECORDER_DRAG_THRESHOLD_PIXELS = 4f;
    public static final int RECORDER_RESUME_FRAMES = 2;

    // Recorder — a lane, as drawn and as pressed: a clip's name and gestures along its upper row, its movement below
    public static final float RECORDER_LANE_PIXELS = 32f;
    public static final float RECORDER_CLIP_BAR_INSET_PIXELS = 1f;
    public static final float RECORDER_CLIP_NAME_ROW_PIXELS = 13f;

    // Recorder — taking hold on the lanes; how near a press must be to a clip's edge or a node's notch, how near a
    // drag snaps, and at most how much of a short clip's length its edges take up
    public static final float RECORDER_EDGE_GRAB_PIXELS = 6f;
    public static final float RECORDER_NOTCH_GRAB_PIXELS = 5f;
    public static final float RECORDER_SNAP_PIXELS = 8f;
    public static final float RECORDER_EDGE_GRAB_SHARE = 1f / 3f;

    // Recorder — the view; each notch of the wheel zooms about the pointer, Shift with the wheel or a sideways scroll
    // moves along by a share of the view, and a playhead leaving the view brings it back a share in from the left
    public static final float RECORDER_MIN_VIEW_SECONDS = 0.25f;
    public static final float RECORDER_ZOOM_STEP = 1.25f;
    public static final float RECORDER_ZOOM_BUTTON_STEP = 2f;
    public static final float RECORDER_SCROLL_SHARE = 0.1f;
    public static final float RECORDER_REVEAL_SHARE = 0.1f;

    // Recorder — nodes, named after the movement state or action they show, its words parted by spaces
    public static final char RECORDER_NODE_NAME_SEPARATOR = '_';
    public static final char RECORDER_NODE_WORD_SEPARATOR = ' ';

    // Recorder — curves; a curve fitted to a take keeps this near what was recorded, read every so often, with no
    // more keys than this or so many a second of the take, each key's slope pulled this slightly toward the slope
    // through its neighbours, and a graph of one shows at least this span of its values
    public static final double RECORDER_CURVE_DISTANCE_TOLERANCE = 0.05;
    public static final double RECORDER_CURVE_ANGLE_TOLERANCE = 1.0;
    public static final double RECORDER_CURVE_SCALAR_TOLERANCE = 0.03;
    public static final double RECORDER_CURVE_CLOCK_TOLERANCE = 250.0;
    public static final double RECORDER_CURVE_DISTANCE_SPAN = 0.5;
    public static final double RECORDER_CURVE_ANGLE_SPAN = 5.0;
    public static final double RECORDER_CURVE_SCALAR_SPAN = 0.1;
    public static final double RECORDER_CURVE_CLOCK_SPAN = 60_000.0;
    public static final float RECORDER_CURVE_FIT_INTERVAL = 1f / 20f;
    public static final int RECORDER_CURVE_MAX_KEYS = 512;
    public static final float RECORDER_CURVE_MAX_KEYS_PER_SECOND = 4f;
    public static final double RECORDER_CURVE_FIT_STEADINESS = 1e-3;
    public static final float RECORDER_KEY_MIN_GAP_SECONDS = 0.01f;
    public static final int RECORDER_CURVE_SOLVE_STEPS = 16;
    public static final double RECORDER_CURVE_SOLVE_EPSILON = 1e-5;
    public static final int RECORDER_GRAPH_FRAME_SAMPLES = 64;

    // Recorder — curves, as the Keys tab names them; a mast, sail or door is named with its number
    public static final String RECORDER_TRACK_LABEL_CAMERA = "Camera";
    public static final String RECORDER_TRACK_LABEL_WEATHER = "World weather";
    public static final String RECORDER_TRACK_LABEL_CLOCK = "World time";
    public static final String RECORDER_CURVE_PART_FORMAT = "%s %d";
    public static final String RECORDER_CURVE_LABEL_POSITION_X = "Position X";
    public static final String RECORDER_CURVE_LABEL_POSITION_Y = "Height";
    public static final String RECORDER_CURVE_LABEL_POSITION_Z = "Position Z";
    public static final String RECORDER_CURVE_LABEL_YAW = "Yaw";
    public static final String RECORDER_CURVE_LABEL_PITCH = "Pitch";
    public static final String RECORDER_CURVE_LABEL_ROLL = "Roll";
    public static final String RECORDER_CURVE_LABEL_SHAKE_YAW = "Shake yaw";
    public static final String RECORDER_CURVE_LABEL_SHAKE_PITCH = "Shake pitch";
    public static final String RECORDER_CURVE_LABEL_SHAKE_ROLL = "Shake roll";
    public static final String RECORDER_CURVE_LABEL_FIELD_OF_VIEW = "Field of view";
    public static final String RECORDER_CURVE_LABEL_FACING = "Facing";
    public static final String RECORDER_CURVE_LABEL_TURN_RATE = "Turn rate";
    public static final String RECORDER_CURVE_LABEL_LOOK_YAW = "Look yaw";
    public static final String RECORDER_CURVE_LABEL_LOOK_PITCH = "Look pitch";
    public static final String RECORDER_CURVE_LABEL_GROUND_SPEED = "Ground speed";
    public static final String RECORDER_CURVE_LABEL_RISE_SPEED = "Rise speed";
    public static final String RECORDER_CURVE_LABEL_GROUND_OFFSET = "Ground offset";
    public static final String RECORDER_CURVE_LABEL_RUDDER = "Rudder";
    public static final String RECORDER_CURVE_LABEL_BRACE = "Mast brace";
    public static final String RECORDER_CURVE_LABEL_HOIST = "Sail hoist";
    public static final String RECORDER_CURVE_LABEL_DOOR = "Door opening";
    public static final String RECORDER_CURVE_LABEL_DRIFT_X = "Drift X";
    public static final String RECORDER_CURVE_LABEL_DRIFT_Z = "Drift Z";
    public static final String RECORDER_CURVE_LABEL_GUST = "Gusts";
    public static final String RECORDER_CURVE_LABEL_TIME = "Time and date";

    // Recorder — values as the Keys tab writes them and the value dialog reads them back; the clock is a day counted
    // from the calendar's first, and a time of day
    public static final String RECORDER_VALUE_FORMAT_DISTANCE = "%.2f";
    public static final String RECORDER_VALUE_FORMAT_DEGREES = "%.1f";
    public static final String RECORDER_VALUE_FORMAT_SCALAR = "%.3f";
    public static final String RECORDER_VALUE_FORMAT_CLOCK = "Day %d  %02d:%02d";
    public static final String RECORDER_VALUE_FORMAT_CLOCK_INPUT = "%d %02d:%02d";
    public static final String RECORDER_VALUE_WORD_PATTERN = "\\s+";
    public static final String RECORDER_VALUE_CLOCK_SEPARATOR = ":";

    // Recorder — the Keys tab; a row hands back its track's place and what it shows, a curve's slot or one of the
    // rows below, and a press takes hold of a key or a handle this near it
    public static final String RECORDER_KEY_ROW_SEPARATOR = ":";
    public static final int RECORDER_KEY_ROW_TRACK = -1;
    public static final int RECORDER_KEY_ROW_MOVEMENT = -2;
    public static final int RECORDER_KEY_ROW_GESTURES = -3;
    public static final float RECORDER_KEY_GRAB_PIXELS = 6f;
    public static final float RECORDER_GRAPH_GRAB_PIXELS = 9f;
    public static final double RECORDER_GRAPH_MARGIN_SHARE = 0.1;
    public static final String RECORDER_KEY_STATUS_SEPARATOR = ": ";

    // Recorder — asset sections, as the Assets pane names them
    public static final String RECORDER_ASSET_LABEL_WORLD = "World";
    public static final String RECORDER_ASSET_LABEL_CALENDAR = "Calendar";
    public static final String RECORDER_ASSET_LABEL_CHARACTERS = "Characters";
    public static final String RECORDER_ASSET_LABEL_GENDERS = "Genders";
    public static final String RECORDER_ASSET_LABEL_FEATURES = "Features";
    public static final String RECORDER_ASSET_LABEL_ITEMS = "Items";
    public static final String RECORDER_ASSET_LABEL_VEHICLES = "Vehicles";
    public static final String RECORDER_ASSET_LABEL_MODELS = "Models";
    public static final String RECORDER_ASSET_LABEL_TEXTURES = "Textures";
    public static final String RECORDER_ASSET_LABEL_MATERIALS = "Materials";
    public static final String RECORDER_ASSET_LABEL_ANIMATION_TREES = "Animation trees";
    public static final String RECORDER_ASSET_LABEL_ANIMATIONS = "Animations";
    public static final String RECORDER_ASSET_LABEL_INTERFACE = "Interface";

    // Recorder — key modes, as the Keys pane names them
    public static final String RECORDER_KEY_MODE_LABEL_AUTO = "Auto";
    public static final String RECORDER_KEY_MODE_LABEL_ALIGNED = "Aligned";
    public static final String RECORDER_KEY_MODE_LABEL_FREE = "Free";
    public static final String RECORDER_KEY_MODE_LABEL_LINEAR = "Linear";
    public static final String RECORDER_KEY_MODE_LABEL_CONSTANT = "Hold";

    // Recorder — sampling, in seconds; the camera, the player and the clock are sampled finer than the world
    public static final float RECORDER_SAMPLE_INTERVAL = 1f / 60f;
    public static final float RECORDER_WORLD_SAMPLE_INTERVAL = 1f / 30f;
    public static final float RECORDER_WEATHER_SAMPLE_INTERVAL = 1f / 10f;
    public static final float RECORDER_OVERLAY_SAMPLE_INTERVAL = 1f / 30f;
    public static final int RECORDER_OVERLAY_KEYFRAME_INTERVAL = 30;
    public static final float RECORDER_SAMPLE_TOLERANCE = 0.001f;
    public static final float RECORDER_GAP_SECONDS = 0.25f;
    public static final float RECORDER_MIN_TAKE_SECONDS = 0.1f;
    public static final float RECORDER_TIME_UNSET = -1f;
    public static final int RECORDER_NO_CLIP = -1;
    public static final float RECORDER_FULL_TURN_DEGREES = 360f;
    public static final int RECORDER_LOOK_HASH_PRIME = 31;

    // Recorder — the clock; a clock moving this far past real time between samples was moved by a command
    public static final long RECORDER_CLOCK_JUMP_MILLIS = 1000L;
    public static final float[] RECORDER_TIME_RAMP_SECONDS = { 0f, 2f, 5f, 10f };

    // Recorder — the interface; while it plays, the Dev window's own is withheld from this layer up
    public static final int RECORDER_INTERFACE_LIVE_LAYER = RuntimeSetting.LAYER_VITALITY;

    // Recorder — sample blocks: a count, then whole columns of 4-byte floats and ints and 8-byte chunk coordinates
    public static final int RECORDER_BLOCK_HEADER_BYTES = Integer.BYTES;
    public static final int RECORDER_PLACE_SAMPLE_BYTES = Float.BYTES + Long.BYTES + Float.BYTES * 3;
    public static final int RECORDER_CAMERA_SAMPLE_BYTES = RECORDER_PLACE_SAMPLE_BYTES + Float.BYTES * 7;
    public static final int RECORDER_BODY_SAMPLE_BYTES = RECORDER_PLACE_SAMPLE_BYTES + Float.BYTES * 8
            + Integer.BYTES * 4;
    public static final int RECORDER_PROJECTILE_SAMPLE_BYTES = RECORDER_PLACE_SAMPLE_BYTES + Float.BYTES * 4;
    public static final int RECORDER_CLOCK_SAMPLE_BYTES = Float.BYTES + Long.BYTES;
    public static final int RECORDER_CURVE_KEY_BYTES = Float.BYTES * 3 + Double.BYTES * 3 + Integer.BYTES;
    public static final int RECORDER_WEATHER_SAMPLE_BYTES = Float.BYTES * 2 + Double.BYTES * 2;
    public static final int RECORDER_VEHICLE_SAMPLE_BYTES = RECORDER_PLACE_SAMPLE_BYTES + Float.BYTES * 5
            + Integer.BYTES;
    public static final int RECORDER_VEHICLE_PART_BYTES = Float.BYTES;
    public static final int RECORDER_OVERLAY_FRAME_BYTES = Float.BYTES + Integer.BYTES * 4;

    // Recorder — export choices, cycled in order; the first motion blur choice is none
    public static final int[] RECORDER_EXPORT_WIDTHS = { 1280, 1920, 2560, 3840 };
    public static final int[] RECORDER_EXPORT_HEIGHTS = { 720, 1080, 1440, 2160 };
    public static final int[] RECORDER_EXPORT_FRAME_RATES = { 24, 30, 60 };
    public static final int[] RECORDER_EXPORT_MOTION_BLUR_SAMPLES = { 1, 2, 4, 8 };
    public static final int RECORDER_EXPORT_DEFAULT_WIDTH = 1920;
    public static final int RECORDER_EXPORT_DEFAULT_HEIGHT = 1080;
    public static final int RECORDER_EXPORT_DEFAULT_FRAME_RATE = 60;
    public static final int RECORDER_EXPORT_DEFAULT_MOTION_BLUR_SAMPLES = 1;
    public static final int RECORDER_EXPORT_SUPERSAMPLE_SCALE = 2;

    // Recorder — export rendering; a 180 degree shutter, and the frames streaming is given to settle
    public static final float RECORDER_EXPORT_SHUTTER = 0.5f;
    public static final int RECORDER_EXPORT_WARMUP_IDLE_FRAMES = 8;
    public static final int RECORDER_EXPORT_WARMUP_LIMIT = 1800;
    public static final int RECORDER_EXPORT_SETTLE_LIMIT = 120;
    public static final int RECORDER_EXPORT_FRAME_SLOTS = 2;
    public static final String RECORDER_CAPTURE_FBO = "RecorderCaptureScene";
    public static final String RECORDER_ENCODING_THREAD_NAME = "VideoEncoding";
    public static final int RECORDER_FRAME_BYTES_PER_PIXEL = 3;
    public static final int RECORDER_BYTE_MASK = 0xFF;

    // Recorder — video formats
    public static final String RECORDER_VIDEO_EXTENSION_MP4 = "mp4";
    public static final String RECORDER_VIDEO_EXTENSION_MOV = "mov";
    public static final String RECORDER_VIDEO_EXTENSION_AVI = "avi";
    public static final String RECORDER_VIDEO_LABEL_MP4 = "MP4  H.264";
    public static final String RECORDER_VIDEO_LABEL_MOV = "MOV  ProRes 422 HQ";
    public static final String RECORDER_VIDEO_LABEL_AVI = "AVI  Motion JPEG";

    // Recorder — MP4 and MOV, encoded in the engine; pictures are whole macroblocks, the frame cropped from them,
    // and each frame goes to the file as its own chunk, so one packet buffer serves every frame
    public static final int RECORDER_MACROBLOCK_PIXELS = 16;
    public static final int RECORDER_PACKET_BYTES_PER_PIXEL = 2;
    public static final int RECORDER_FRAMES_PER_CHUNK = 1;

    // Recorder — colour, studio-range BT.709 as HD video expects, as fixed point RGB-to-YCbCr weights that leave a
    // grey with no colour; planes hold each value less 128, and the streams and files name BT.709 throughout
    public static final int RECORDER_YUV_SHIFT = 16;
    public static final int RECORDER_YUV_ROUNDING = 1 << (RECORDER_YUV_SHIFT - 1);
    public static final int RECORDER_YUV_LUMA_BLACK = 16;
    public static final int RECORDER_YUV_PLANE_OFFSET = 128;
    public static final int RECORDER_YUV_LUMA_RED = 11966;
    public static final int RECORDER_YUV_LUMA_GREEN = 40254;
    public static final int RECORDER_YUV_LUMA_BLUE = 4064;
    public static final int RECORDER_YUV_BLUE_RED = -6596;
    public static final int RECORDER_YUV_BLUE_GREEN = -22188;
    public static final int RECORDER_YUV_BLUE_BLUE = 28784;
    public static final int RECORDER_YUV_RED_RED = 28784;
    public static final int RECORDER_YUV_RED_GREEN = -26145;
    public static final int RECORDER_YUV_RED_BLUE = -2639;
    public static final int RECORDER_COLOR_BT709 = 1;

    // Recorder — H.264, at a fixed high quality below the quantiser where deblocking begins, so the encoder predicts
    // from exactly the pixels a decoder does; a macroblock is raised only as far as it must be to fit the packet,
    // never past the last quantiser whose chroma the encoder rebuilds as a decoder does, and eased back once it fits
    // with room to spare; a keyframe every second, and the lowest level whose frame size and macroblock rate hold
    // the export
    public static final int RECORDER_H264_INTRA_QP = 15;
    public static final int RECORDER_H264_INTER_QP = 15;
    public static final int RECORDER_H264_MAX_QP = 29;
    public static final int RECORDER_H264_EASE_SHIFT = 3;
    public static final int RECORDER_H264_KEYFRAME_SECONDS = 1;
    public static final int RECORDER_H264_MOTION_SEARCH_RANGE = 32;
    public static final int RECORDER_H264_VIDEO_FORMAT_UNSPECIFIED = 5;
    public static final int RECORDER_H264_TICKS_PER_FRAME = 2;
    public static final int[] RECORDER_H264_LEVELS = { 31, 32, 40, 42, 50, 51, 52 };
    public static final int[] RECORDER_H264_LEVEL_FRAME_MACROBLOCKS = { 3600, 5120, 8192, 8704, 22080, 36864, 36864 };
    public static final int[] RECORDER_H264_LEVEL_MACROBLOCK_RATES = {
            108000, 216000, 245760, 522240, 589824, 983040, 2073600 };

    // Recorder — ProRes 422 HQ; its frame header names the colour primaries, transfer and matrix at these bytes
    public static final int RECORDER_PRORES_PRIMARIES_BYTE = 22;
    public static final int RECORDER_PRORES_TRANSFER_BYTE = 23;
    public static final int RECORDER_PRORES_MATRIX_BYTE = 24;

    // Recorder — Motion JPEG
    public static final String RECORDER_JPEG_FORMAT = "jpeg";
    public static final float RECORDER_JPEG_QUALITY = 0.95f;

    // Recorder — the OpenDML AVI container, its chunk names, sizes in bytes, flags and segment limit
    public static final String RECORDER_AVI_RIFF = "RIFF";
    public static final String RECORDER_AVI_LIST = "LIST";
    public static final String RECORDER_AVI_FORM_FIRST = "AVI ";
    public static final String RECORDER_AVI_FORM_EXTENDED = "AVIX";
    public static final String RECORDER_AVI_HEADER_LIST = "hdrl";
    public static final String RECORDER_AVI_MAIN_HEADER = "avih";
    public static final String RECORDER_AVI_STREAM_LIST = "strl";
    public static final String RECORDER_AVI_STREAM_HEADER = "strh";
    public static final String RECORDER_AVI_STREAM_FORMAT = "strf";
    public static final String RECORDER_AVI_SUPER_INDEX = "indx";
    public static final String RECORDER_AVI_EXTENDED_LIST = "odml";
    public static final String RECORDER_AVI_EXTENDED_HEADER = "dmlh";
    public static final String RECORDER_AVI_FRAME_LIST = "movi";
    public static final String RECORDER_AVI_FRAME_CHUNK = "00dc";
    public static final String RECORDER_AVI_STANDARD_INDEX = "ix00";
    public static final String RECORDER_AVI_OLD_INDEX = "idx1";
    public static final String RECORDER_AVI_VIDEO_STREAM = "vids";
    public static final String RECORDER_AVI_CODEC = "MJPG";
    public static final int RECORDER_AVI_CHUNK_HEADER_BYTES = 8;
    public static final int RECORDER_AVI_LIST_HEADER_BYTES = 12;
    public static final int RECORDER_AVI_RIFF_HEADER_BYTES = 12;
    public static final int RECORDER_AVI_MAIN_HEADER_BYTES = 56;
    public static final int RECORDER_AVI_MAIN_HEADER_RESERVED_BYTES = 16;
    public static final int RECORDER_AVI_STREAM_HEADER_BYTES = 56;
    public static final int RECORDER_AVI_BITMAP_HEADER_BYTES = 40;
    public static final int RECORDER_AVI_BITMAP_TAIL_BYTES = 16;
    public static final int RECORDER_AVI_SUPER_INDEX_ENTRIES = 256;
    public static final int RECORDER_AVI_SUPER_INDEX_RESERVED_BYTES = 12;
    public static final int RECORDER_AVI_SUPER_INDEX_BYTES = 24 + RECORDER_AVI_SUPER_INDEX_ENTRIES * 16;
    public static final int RECORDER_AVI_EXTENDED_HEADER_BYTES = 248;
    public static final int RECORDER_AVI_STREAM_LIST_BYTES = 4
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_STREAM_HEADER_BYTES
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_BITMAP_HEADER_BYTES
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_SUPER_INDEX_BYTES;
    public static final int RECORDER_AVI_EXTENDED_LIST_BYTES = 4
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_EXTENDED_HEADER_BYTES;
    public static final int RECORDER_AVI_HEADER_LIST_BYTES = RECORDER_AVI_LIST_HEADER_BYTES
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_MAIN_HEADER_BYTES
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_STREAM_LIST_BYTES
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_EXTENDED_LIST_BYTES;
    public static final int RECORDER_AVI_STANDARD_INDEX_HEADER_BYTES = 24;
    public static final int RECORDER_AVI_STANDARD_INDEX_ENTRY_BYTES = 8;
    public static final int RECORDER_AVI_OLD_INDEX_ENTRY_BYTES = 16;
    public static final short RECORDER_AVI_STANDARD_INDEX_LONGS_PER_ENTRY = 2;
    public static final short RECORDER_AVI_SUPER_INDEX_LONGS_PER_ENTRY = 4;
    public static final byte RECORDER_AVI_INDEX_OF_INDEXES = 0;
    public static final byte RECORDER_AVI_INDEX_OF_CHUNKS = 1;
    public static final int RECORDER_AVI_KEYFRAME_FLAG = 0x10;
    public static final int RECORDER_AVI_HAS_INDEX_FLAG = 0x10;
    public static final int RECORDER_AVI_STREAM_COUNT = 1;
    public static final int RECORDER_AVI_DEFAULT_QUALITY = -1;
    public static final short RECORDER_AVI_PLANES = 1;
    public static final short RECORDER_AVI_BITS_PER_PIXEL = 24;
    public static final int RECORDER_AVI_MICROS_PER_SECOND = 1_000_000;
    public static final long RECORDER_AVI_SEGMENT_LIMIT_BYTES = 1L << 30;

    // Recorder — messages
    public static final String RECORDER_MESSAGE_UNREADABLE = "Recording is unreadable and was skipped: ";
    public static final String RECORDER_MESSAGE_MALFORMED = "Recording is malformed or no longer fits the game: ";
    public static final String RECORDER_MESSAGE_INTERRUPTED = "The export was interrupted";
    public static final String RECORDER_MESSAGE_NO_JPEG = "This Java has no JPEG writer for AVI exports";
    public static final String RECORDER_MESSAGE_WRITE_FAILED = "The video could not be written: ";
    public static final String RECORDER_MESSAGE_AVI_TOO_LONG = "The video is too long for one AVI file";
    public static final String RECORDER_MESSAGE_NO_TEMPLATE = "A recorded character's template no longer exists: ";
    public static final String RECORDER_MESSAGE_NO_ITEM = "A recorded item no longer exists: ";
    public static final String RECORDER_MESSAGE_LOOK_REFUSED = "A recorded look no longer fits its character: ";
    public static final String RECORDER_MESSAGE_NO_VEHICLE = "A recorded vehicle type no longer exists: ";
    public static final String RECORDER_MESSAGE_OVERLAY_UNREADABLE = "A recorded interface frame could not be read: ";
    public static final String RECORDER_MESSAGE_FLUSH_FAILED = "An interface frame was lost finishing a take: ";

    // Recorder — status text
    public static final String RECORDER_STATUS_READY = "Ready";
    public static final String RECORDER_STATUS_RECORDING = "Recording ";
    public static final String RECORDER_STATUS_PLAYING = "Playing";
    public static final String RECORDER_STATUS_PAUSED = "Paused";
    public static final String RECORDER_STATUS_TAKE_SAVED = "Saved take ";
    public static final String RECORDER_STATUS_SUSPENDED = "Paused with the game: close the pause menu to carry on";
    public static final String RECORDER_STATUS_PLACED = "Placed ";
    public static final String RECORDER_STATUS_MOVED = "Moved ";
    public static final String RECORDER_STATUS_CUT = "Cut ";
    public static final String RECORDER_STATUS_CUT_TOOL_ON = "Cut tool on: click a clip to cut it there";
    public static final String RECORDER_STATUS_CUT_TOOL_OFF = "Cut tool off";
    public static final String RECORDER_STATUS_CUT_TOO_NEAR = "Too near the clip's end to cut there";
    public static final String RECORDER_STATUS_STAMPED = "Stamped ";
    public static final String RECORDER_STATUS_TRIMMED = "Trimmed ";
    public static final String RECORDER_STATUS_RETIMED = "Retimed ";
    public static final String RECORDER_STATUS_NODE_CHANGED = "Now ";
    public static final String RECORDER_STATUS_NODE_REMOVED = "Removed ";
    public static final String RECORDER_STATUS_NO_NODE = "Click a node on a clip first";
    public static final String RECORDER_STATUS_ONLY_MOVEMENT = "The take moves only one way: change it instead";
    public static final String RECORDER_STATUS_KEY_MOVED = "Moved a key of ";
    public static final String RECORDER_STATUS_KEYS_MOVED = "Moved keys of ";
    public static final String RECORDER_STATUS_KEY_ADDED = "Keyed ";
    public static final String RECORDER_STATUS_KEY_DELETED = "Deleted a key of ";
    public static final String RECORDER_STATUS_KEYS_DELETED = "Deleted keys of ";
    public static final String RECORDER_STATUS_KEY_CHANGED = "Changed a key of ";
    public static final String RECORDER_STATUS_CURVE_REFIT = "Laid again from the recording: ";
    public static final String RECORDER_STATUS_TAKE_REFIT = "Laying every value again from the recording: ";
    public static final String RECORDER_STATUS_LAST_KEY = "A value keeps at least one key: change its value instead";
    public static final String RECORDER_STATUS_NO_CURVE = "Click a value on the Keys pane first";
    public static final String RECORDER_STATUS_NO_KEY = "Click a key first";
    public static final String RECORDER_STATUS_CLIP_REMOVED = "Removed a clip of ";
    public static final String RECORDER_STATUS_TAKE_DELETED = "Deleted take ";
    public static final String RECORDER_STATUS_NO_CLIP = "Select a clip on the timeline first";
    public static final String RECORDER_STATUS_OUTSIDE_CLIP = "Move the playhead inside the clip first";
    public static final String RECORDER_STATUS_UPGRADED = "Opened and upgraded to takes and clips: ";
    public static final String RECORDER_STATUS_TAKE_TOO_SHORT = "Take too short to keep";
    public static final String RECORDER_STATUS_NOTHING_ARMED = "Arm at least one channel to record";
    public static final String RECORDER_STATUS_NOTHING_RECORDED = "Nothing recorded yet";
    public static final String RECORDER_STATUS_NO_DEV_WINDOW = "Open a Dev Mode tab to record or play";
    public static final String RECORDER_STATUS_WRONG_WORLD = "This recording was made in another world: ";
    public static final String RECORDER_STATUS_NAME_TAKEN = "That name is taken or not a valid file name: ";
    public static final String RECORDER_STATUS_MISSING = "Recording no longer exists: ";
    public static final String RECORDER_STATUS_UNREADABLE = "Recording could not be read: ";
    public static final String RECORDER_STATUS_OPENED = "Opened ";
    public static final String RECORDER_STATUS_SAVE_FAILED = "Could not save ";
    public static final String RECORDER_STATUS_RENAMED = "Renamed to ";
    public static final String RECORDER_STATUS_RENAME_FAILED = "Could not rename ";
    public static final String RECORDER_STATUS_DELETED = "Deleted ";
    public static final String RECORDER_STATUS_DELETE_FAILED = "Could not delete ";
    public static final String RECORDER_STATUS_EXPORT_WHILE_RECORDING = "Stop the take before exporting";
    public static final String RECORDER_STATUS_EXPORT_WHILE_PAUSED =
            "Close the pause menu in the Dev Mode tab before exporting";
    public static final String RECORDER_STATUS_EXPORTING = "Exporting ";
    public static final String RECORDER_STATUS_EXPORTED = "Exported ";
    public static final String RECORDER_STATUS_EXPORT_FAILED = "Export failed: ";
    public static final String RECORDER_STATUS_EXPORT_CANCELLED = "Export cancelled";

    // Recorder — the held shortcuts its drags and wheel read, named in shortcuts/Recorder
    public static final String RECORDER_SHORTCUT_FREE_DRAG = "Recorder/FreeDrag";
    public static final String RECORDER_SHORTCUT_SCROLL_VIEW = "Recorder/ScrollView";
    public static final String RECORDER_SHORTCUT_ZOOM_VALUES = "Recorder/ZoomValues";
    public static final String RECORDER_SHORTCUT_EXTEND_SELECTION = "Recorder/ExtendSelection";
    public static final String RECORDER_SHORTCUT_SUBTRACT_SELECTION = "Recorder/SubtractSelection";

    // Recorder — the keys the Recorder tab's setting rows hand back; channel rows add the channel's key to a prefix
    public static final String RECORDER_KEY_ARM_PREFIX = "arm_";
    public static final String RECORDER_KEY_CLIP_CHANNEL_PREFIX = "clip_channel_";
    public static final String RECORDER_KEY_CLIP_MUTE = "clip_mute";
    public static final String RECORDER_KEY_CLIP_RAMP = "clip_ramp";
    public static final String RECORDER_KEY_CLIP_TRIM_START = "clip_trim_start";
    public static final String RECORDER_KEY_CLIP_TRIM_END = "clip_trim_end";
    public static final String RECORDER_KEY_CLIP_SPLIT = "clip_split";
    public static final String RECORDER_KEY_CLIP_STAMP = "clip_stamp";
    public static final String RECORDER_KEY_CLIP_REMOVE = "clip_remove";
    public static final String RECORDER_KEY_NODE_NEXT = "node_next";
    public static final String RECORDER_KEY_NODE_PREVIOUS = "node_previous";
    public static final String RECORDER_KEY_NODE_REMOVE = "node_remove";
    public static final String RECORDER_KEY_KEY_ADD = "key_add";
    public static final String RECORDER_KEY_KEY_DELETE = "key_delete";
    public static final String RECORDER_KEY_KEY_MODE = "key_mode";
    public static final String RECORDER_KEY_KEY_VALUE = "key_value";
    public static final String RECORDER_KEY_CURVE_REFIT = "curve_refit";
    public static final String RECORDER_KEY_TAKE_REFIT = "take_refit";
    public static final String RECORDER_KEY_RESOLUTION = "resolution";
    public static final String RECORDER_KEY_FRAME_RATE = "frame_rate";
    public static final String RECORDER_KEY_FORMAT = "format";
    public static final String RECORDER_KEY_MOTION_BLUR = "motion_blur";
    public static final String RECORDER_KEY_SUPERSAMPLE = "supersample";
    public static final String RECORDER_KEY_RANGE_START = "range_start";
    public static final String RECORDER_KEY_RANGE_END = "range_end";
    public static final String RECORDER_KEY_RANGE_CLEAR = "range_clear";
    public static final String RECORDER_KEY_EXPORT = "export";
    public static final String RECORDER_KEY_CANCEL_EXPORT = "cancel_export";

    // Commands — Recorder
    public static final String COMMAND_RECORD = "record";
    public static final String COMMAND_PLAY = "play";
    public static final String COMMAND_STOP = "stop";

    // Tabs
    public static final float DIVIDER_HIT_TOLERANCE = 6f;
    public static final String ELEMENT_TAB_STRIP_ACCENT = "tab_strip_accent";
    public static final String ELEMENT_TAB_STRIP_CLOSE = "tab_strip_close";
    public static final String ELEMENT_TAB_STRIP_LABEL = "tab_strip_label";
    public static final String MENU_TAB_GHOST = "editor/TabFrame/TabGhost";
    public static final String MENU_TAB_SHELL = "editor/TabFrame/TabFrame";
    public static final String MENU_TAB_STRIP_ENTRY = "editor/TabFrame/tab_strip_entry";
    public static final float RATIO_DEFAULT = 0.5f;
    public static final float RATIO_MAX = 0.9f;
    public static final float RATIO_MIN = 0.1f;
    public static final float TAB_DRAG_EDGE_FRACTION = 0.25f;
    public static final int TAB_DRAG_PREVIEW_H = 144;
    public static final int TAB_DRAG_PREVIEW_W = 256;
    public static final float TAB_DRAG_THRESHOLD = 6f;
    public static final int TAB_ENTRY_STRIP = 0;
    public static final float TAB_STRIP_ACTIVE_ALPHA = 0.32f;
    public static final String TAB_TITLE_DEV = "Dev Mode";
    public static final String TAB_TITLE_PREVIEW = "Preview";
    public static final String WINDOW_TITLE_EDITOR_SECONDARY = "Secondary";
}
