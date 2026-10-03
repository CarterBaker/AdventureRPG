package editor.runtime;

import engine.input.Buttons;

public class EditorSetting {

    /*
     * Editor-only constants — chrome menus, cursors, input keys, menu entry
     * point indices, layout persistence, tool tabs, the name dialog, console
     * commands and the command drag between windows, the info pipeline's
     * schemas, hierarchy keys, and status text, the item editor's library and
     * status text, the image editing framework's history and files, the world
     * map's status text and overlay toggles, and the world editor's brush and
     * status text.
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

    // Tabs
    public static final String TAB_TITLE_COMMAND_CONSOLE = "Command Console";
    public static final String TAB_TITLE_CONSOLE = "Console";
    public static final String TAB_TITLE_HIERARCHY = "Hierarchy";
    public static final String TAB_TITLE_INFO_PANEL = "Info Panel";
    public static final String TAB_TITLE_ITEM_EDITOR = "Item Editor";
    public static final String TAB_TITLE_PROFILER = "Profiler";
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
    public static final float DEV_VEHICLE_SPAWN_CLEARANCE = 6f;
    public static final float DEV_VEHICLE_REMOVE_REACH = 24f;

    // Viewports
    public static final String FBO_EDITOR_SCENE = "EditorScene";

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

    // World Editor
    public static final int WORLD_EDITOR_BRUSH_RADIUS_DEFAULT = 1;
    public static final int WORLD_EDITOR_BRUSH_RADIUS_MAX = 32;
    public static final int WORLD_EDITOR_BRUSH_RADIUS_MIN = 0;
    public static final String WORLD_EDITOR_TOOL_BRUSH = "Brush";
    public static final String WORLD_EDITOR_TOOL_FILL = "Fill";
    public static final String WORLD_EDITOR_TOOL_PICK = "Pick";
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

    // Item Library
    public static final String ITEM_EDITOR_MESH_DIRECTORY = "items";
    public static final String ITEM_EDITOR_DEFINITION_FILE = "EditorItems";
    public static final String ITEM_EDITOR_TEXTURE_ARRAY = "items/standard";
    public static final String ITEM_EDITOR_DEFAULT_PART_NAME = "Body";

    // Info Schemas
    public static final String INFO_SCHEMA_PATH = "schemas";
    public static final String INFO_SCHEMA_BIOMES = "Biomes";
    public static final String INFO_SCHEMA_ITEMS = "Items";
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
