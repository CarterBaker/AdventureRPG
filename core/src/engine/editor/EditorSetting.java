package engine.editor;

public class EditorSetting {

    /*
     * Editor-only constants — chrome menus, cursors, input keys, menu entry
     * point indices, layout persistence, tool tabs, the name dialog, the info
     * pipeline's schemas, hierarchy keys, and status text, and the item
     * editor's library and status text.
     */

    // Cursors \\
    public static final String CURSOR_RESIZE_H = "menus/CursorStretchIconHorizontal";
    public static final String CURSOR_RESIZE_V = "menus/CursorStretchIconVertical";

    // Layouts \\
    public static final String LAYOUT_FILE_EXTENSION = "json";
    public static final int LAYOUT_NAME_MAX_LENGTH = 32;
    public static final String LAYOUT_SESSION_NAME = "LastSession";

    // Resize \\
    public static final float RESIZE_EDGE_TOLERANCE = 8f;

    // Menus \\
    public static final String MENU_EDITOR_BASE = "editor/EditorWindow/Base";
    public static final String MENU_EDITOR_SECONDARY = "editor/EditorWindow/Secondary";
    public static final String MENU_EDITOR_TOOLBAR = "editor/EditorWindow/Toolbar";
    public static final String MENU_EDITOR_NAME_DIALOG = "editor/EditorWindow/NameDialog";
    public static final String MENU_EDITOR_LAYOUT_ITEM_TEMPLATE = "editor/EditorWindow/layout_item_template";

    // Elements \\
    public static final String ELEMENT_LAYOUT_ITEM_LABEL = "layout_item_label";

    // Keys \\
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

    // Name Dialog \\
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

    // Tabs \\
    public static final String TAB_TITLE_CONSOLE = "Console";
    public static final String TAB_TITLE_HIERARCHY = "Hierarchy";
    public static final String TAB_TITLE_INFO_PANEL = "Info Panel";
    public static final String TAB_TITLE_ITEM_EDITOR = "Item Editor";
    public static final String TAB_TITLE_TEXTURE_VIEWER = "Texture Viewer";

    // Viewports \\
    public static final String FBO_EDITOR_SCENE = "EditorScene";

    // Texture Viewer \\
    public static final String TEXTURE_VIEWER_STATUS_HOVERED = "Texture: ";
    public static final String TEXTURE_VIEWER_STATUS_NO_BRUSH = "No brush. Click a texture to paint items with it.";

    // Item Library \\
    public static final String ITEM_EDITOR_FILE_EXTENSION = "json";
    public static final String ITEM_EDITOR_MESH_DIRECTORY = "items";
    public static final String ITEM_EDITOR_DEFINITION_FILE = "EditorItems";
    public static final String ITEM_EDITOR_TEXTURE_ARRAY = "items/standard";
    public static final String ITEM_EDITOR_DEFAULT_PART_NAME = "Body";

    // Info Schemas \\
    public static final String INFO_SCHEMA_PATH = "schemas";
    public static final String INFO_FILE_EXTENSION = "json";
    public static final String INFO_SCHEMA_ITEMS = "Items";
    public static final String INFO_ITEM_MESH_FIELD = "mesh";
    public static final String INFO_FOLDER_SEPARATOR = "/";
    public static final String INFO_PATH_SEPARATOR = "|";
    public static final String[] INFO_SUMMARY_FIELDS = { "name", "block", "clip", "time" };

    // Info Hierarchy \\
    public static final String HIERARCHY_FOLDER_KEY_PREFIX = "folder:";
    public static final String HIERARCHY_FILE_KEY_PREFIX = "file:";
    public static final String HIERARCHY_ENTRY_KEY_PREFIX = "entry:";
    public static final String HIERARCHY_KEY_SEPARATOR = "|";

    // Info Text \\
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

    // Item Status \\
    public static final String ITEM_EDITOR_DIRTY_MARKER = "*";
    public static final String ITEM_EDITOR_STATUS_SEPARATOR = "  |  ";
    public static final String ITEM_EDITOR_STATUS_BRUSH = "Brush: ";
    public static final String ITEM_EDITOR_STATUS_NO_ITEM = "No item open. Pick one in the Hierarchy or press New.";
    public static final String ITEM_EDITOR_TEXTURE_SEPARATOR = "  :  ";
    public static final String ITEM_EDITOR_TOOL_PLACE = "Place";
    public static final String ITEM_EDITOR_TOOL_ERASE = "Erase";
    public static final String ITEM_EDITOR_TOOL_PAINT = "Paint";
    public static final String ITEM_EDITOR_MESSAGE_SAVED = "Saved ";
    public static final String ITEM_EDITOR_MESSAGE_RELOADED = "Reloaded ";
    public static final String ITEM_EDITOR_MESSAGE_CREATED = "Created ";
    public static final String ITEM_EDITOR_MESSAGE_DELETED = "Deleted ";
    public static final String ITEM_EDITOR_MESSAGE_CONVERTED = "Converted from a quad mesh: save to keep it";
    public static final String ITEM_EDITOR_MESSAGE_EMPTY = "Nothing to save: the item has no cubes";
    public static final String ITEM_EDITOR_MESSAGE_NOT_SAVED = "Not saved yet: nothing to reload";
    public static final String ITEM_EDITOR_MESSAGE_NEW_MESH = "No mesh file yet: save to create it";
    public static final String ITEM_EDITOR_MESSAGE_NO_MESH = "This item names no mesh: set its mesh in the Info Panel";
    public static final String ITEM_EDITOR_MESSAGE_RENAMED = "Renamed part to ";
    public static final String ITEM_EDITOR_MESSAGE_MESH_LIMIT = "Edit refused: the item would pass the vertex limit";
    public static final String ITEM_EDITOR_MESSAGE_LAST_PART = "An item needs at least one part";
    public static final String ITEM_EDITOR_MESSAGE_PART_LIMIT = "An item cannot hold any more parts";
}
