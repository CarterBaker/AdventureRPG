package editor.runtime;

import application.runtime.RuntimeSetting;
import engine.input.Buttons;

public class EditorSetting {

    /*
     * Editor-only constants — chrome menus, cursors, input keys, menu entry
     * point indices, layout persistence, tool tabs, the name dialog, console
     * commands and the command drag between windows, the Dev window's time
     * units, the info pipeline's
     * schemas, hierarchy keys, and status text, the item editor's library and
     * status text, the image editing framework's history and files, the world
     * map's status text and overlay toggles, the world editor's brush and
     * status text, the character viewport shared by the character, animation
     * and animation tree editors, and those editors' files, steps and status
     * text.
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
    public static final String MENU_EDITOR_LAYOUT_ITEM_TEMPLATE = "editor/EditorWindow/layout_item_template";

    // Elements
    public static final String ELEMENT_LAYOUT_ITEM_LABEL = "layout_item_label";

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

    // Tabs
    public static final String TAB_TITLE_COMMAND_CONSOLE = "Command Console";
    public static final String TAB_TITLE_ANIMATION_EDITOR = "Animation Editor";
    public static final String TAB_TITLE_ANIMATION_TREE_EDITOR = "Animation Tree Editor";
    public static final String TAB_TITLE_CHARACTER_EDITOR = "Character Editor";
    public static final String TAB_TITLE_CONSOLE = "Console";
    public static final String TAB_TITLE_HIERARCHY = "Hierarchy";
    public static final String TAB_TITLE_INFO_PANEL = "Info Panel";
    public static final String TAB_TITLE_ITEM_EDITOR = "Item Editor";
    public static final String TAB_TITLE_PROFILER = "Profiler";
    public static final String TAB_TITLE_SHIPWRIGHT = "Shipwright";
    public static final String TAB_TITLE_TEXTURE_VIEWER = "Texture Viewer";
    public static final String TAB_TITLE_WORLD_EDITOR = "World Editor";
    public static final String TAB_TITLE_WORLD_MAP = "World Map";

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

    // Image Editing
    public static final String IMAGE_FORMAT = "png";
    public static final String IMAGE_TEMP_SUFFIX = ".tmp";
    public static final int IMAGE_HISTORY_MAX_STEPS = 64;
    public static final long IMAGE_HISTORY_MAX_PIXELS = 16777216L;
    public static final int IMAGE_SMOOTH_REACH_PIXELS = 1;

    // World Editor
    public static final int WORLD_EDITOR_BRUSH_RADIUS_DEFAULT = 1;
    public static final int WORLD_EDITOR_BRUSH_RADIUS_MAX = 32;
    public static final int WORLD_EDITOR_BRUSH_RADIUS_MIN = 0;
    public static final String WORLD_EDITOR_TOOL_BRUSH = "Brush";
    public static final String WORLD_EDITOR_TOOL_FILL = "Fill";
    public static final String WORLD_EDITOR_TOOL_PICK = "Pick";
    public static final String WORLD_EDITOR_TOOL_PLACE = "Place";
    public static final String WORLD_EDITOR_TOOL_CLEAR = "Clear";
    public static final String WORLD_EDITOR_TOOL_SMOOTH = "Smooth";
    public static final String WORLD_EDITOR_LAYER_BIOMES = "Biomes";
    public static final String WORLD_EDITOR_LAYER_HEIGHTS = "Heights";
    public static final int WORLD_EDITOR_HEIGHT_LEVEL_STEP = 16;
    public static final float WORLD_EDITOR_HEIGHT_PERCENT_SCALE = 100f;
    public static final String WORLD_EDITOR_HEIGHT_RAISE = "Raise ";
    public static final String WORLD_EDITOR_HEIGHT_LOWER = "Lower ";
    public static final String WORLD_EDITOR_HEIGHT_PERCENT = "%";
    public static final String WORLD_EDITOR_HEIGHT_NEUTRAL = "Neutral";
    public static final String WORLD_EDITOR_MESSAGE_SMOOTH_HEIGHTS_ONLY = "Smooth paints the height layer only";
    public static final double WORLD_EDITOR_CLEAR_RADIUS_BLOCKS = 384.0;
    public static final int WORLD_EDITOR_PLACEMENT_REACH_PIXELS = 5;
    public static final String WORLD_EDITOR_PLACEABLE_SETTLEMENT = "Settlement: ";
    public static final String WORLD_EDITOR_PLACEABLE_STRUCTURE = "Structure: ";
    public static final String WORLD_EDITOR_STATUS_TURNS = "Turns ";
    public static final String WORLD_EDITOR_STATUS_NO_PLACEABLE = "Nothing to place";
    public static final String WORLD_EDITOR_MESSAGE_PLACED = "Placed ";
    public static final String WORLD_EDITOR_MESSAGE_CLEARED = "Cleared ";
    public static final String WORLD_EDITOR_MESSAGE_NOTHING_TO_CLEAR = "No placement near the pointer";
    public static final String WORLD_EDITOR_MESSAGE_PLACEMENTS_SAVE_FAILED = "Could not save placements of ";
    public static final String WORLD_EDITOR_MESSAGE_PLACEMENTS_RELOAD_FAILED = "Could not reload placements of ";
    public static final String WORLD_EDITOR_PLAYERS_ON = "Players On";
    public static final String WORLD_EDITOR_PLAYERS_OFF = "Players Off";
    public static final String WORLD_EDITOR_DIRTY_MARKER = "*";
    public static final String WORLD_EDITOR_STATUS_SEPARATOR = "  |  ";
    public static final String WORLD_EDITOR_STATUS_RADIUS = "Radius ";
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

    // Item Status
    public static final String ITEM_EDITOR_DIRTY_MARKER = "*";
    public static final String ITEM_EDITOR_STATUS_SEPARATOR = "  |  ";
    public static final String ITEM_EDITOR_STATUS_BRUSH = "Brush: ";
    public static final String ITEM_EDITOR_STATUS_NO_ITEM = "No item open. Pick one in the Hierarchy or press New.";
    public static final String ITEM_EDITOR_TEXTURE_SEPARATOR = "  :  ";
    public static final String ITEM_EDITOR_TOOL_PLACE = "Place";
    public static final String ITEM_EDITOR_TOOL_WALL = "Wall";
    public static final String ITEM_EDITOR_TOOL_ERASE = "Erase";
    public static final String ITEM_EDITOR_TOOL_PAINT = "Paint";
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

    // Tabs
    public static final float DIVIDER_HIT_TOLERANCE = 6f;
    public static final String MENU_TAB_GHOST = "editor/TabFrame/TabGhost";
    public static final String MENU_TAB_SHELL = "editor/TabFrame/TabFrame";
    public static final float RATIO_DEFAULT = 0.5f;
    public static final float RATIO_MAX = 0.9f;
    public static final float RATIO_MIN = 0.1f;
    public static final float TAB_DRAG_EDGE_FRACTION = 0.25f;
    public static final int TAB_DRAG_PREVIEW_H = 144;
    public static final int TAB_DRAG_PREVIEW_W = 256;
    public static final int TAB_ENTRY_TITLE = 0;
    public static final String TAB_TITLE_DEV = "Dev Mode";
    public static final String TAB_TITLE_PREVIEW = "Preview";
    public static final String WINDOW_TITLE_EDITOR_SECONDARY = "Secondary";
}
