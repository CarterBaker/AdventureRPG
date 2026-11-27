package editor.runtime;

import application.runtime.RuntimeSetting;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.filemenupipeline.util.FileMenuAction;
import editor.bootstrap.imagepipeline.util.ImageMap;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import engine.input.Buttons;
import engine.root.EngineSetting;

public class EditorSetting {

    /*
     * Editor-only constants — chrome menus, the window frame, cursors, input
     * keys, menu entry point indices, layout persistence, tool tabs, the File
     * and Edit menus with every tool's actions in them, the name, export and
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

    // Context — what a shared tab or menu item acts on, named after its own text in parentheses
    public static final String CONTEXT_OPEN = " (";
    public static final String CONTEXT_CLOSE = ")";

    // Window Icon — the sprite every editor OS window shows on the taskbar and in the window switcher
    public static final String WINDOW_ICON = "menus/Sword";

    // Layouts
    public static final int LAYOUT_NAME_MAX_LENGTH = 32;
    public static final String LAYOUT_SESSION_NAME = "LastSession";

    // Resize
    public static final float RESIZE_EDGE_TOLERANCE = 8f;

    // Frame — every editor window draws its own; a press this close to an edge resizes from it, and along an edge
    // this close to a corner from the corner too, in screen units. Two presses on the title bar this close together
    // maximize or restore it, and a maximized window dragged this far restores under the cursor
    public static final float FRAME_RESIZE_BORDER = 4f;
    public static final float FRAME_RESIZE_CORNER = 12f;
    public static final long FRAME_DOUBLE_PRESS_MILLIS = 400L;
    public static final float FRAME_RESTORE_DISTANCE = 6f;

    // Frame — the edges a gesture holds, as bits; a move holds all four
    public static final int FRAME_EDGE_NONE = 0;
    public static final int FRAME_EDGE_LEFT = 1;
    public static final int FRAME_EDGE_RIGHT = 2;
    public static final int FRAME_EDGE_TOP = 4;
    public static final int FRAME_EDGE_BOTTOM = 8;
    public static final int FRAME_EDGES_X = FRAME_EDGE_LEFT | FRAME_EDGE_RIGHT;
    public static final int FRAME_EDGES_Y = FRAME_EDGE_TOP | FRAME_EDGE_BOTTOM;
    public static final int FRAME_EDGES_ALL = FRAME_EDGES_X | FRAME_EDGES_Y;

    // Frame — a window let go of this close to its monitor's work area edge snaps to it, and along an edge this close
    // to a corner into the corner; the top edge alone maximizes. Each area is left, top, right and bottom as shares
    // of the work area, indexed by the edges' bits, null where nothing snaps
    public static final float FRAME_SNAP_REACH = 8f;
    public static final float FRAME_SNAP_CORNER = 64f;
    public static final int FRAME_SNAP_MAXIMIZE = FRAME_EDGE_TOP;
    public static final float[][] FRAME_SNAP_AREAS = {
            null,
            { 0f, 0f, 0.5f, 1f },
            { 0.5f, 0f, 1f, 1f },
            null,
            null,
            { 0f, 0f, 0.5f, 0.5f },
            { 0.5f, 0f, 1f, 0.5f },
            null,
            null,
            { 0f, 0.5f, 0.5f, 1f },
            { 0.5f, 0.5f, 1f, 1f },
            null,
            null,
            null,
            null,
            null };

    // Frame — the cursor shown for each set of held edges, indexed by its bits
    public static final int[] FRAME_EDGE_CURSORS = {
            EngineSetting.CURSOR_DEFAULT,
            EngineSetting.CURSOR_RESIZE_H,
            EngineSetting.CURSOR_RESIZE_H,
            EngineSetting.CURSOR_DEFAULT,
            EngineSetting.CURSOR_RESIZE_V,
            EngineSetting.CURSOR_RESIZE_NWSE,
            EngineSetting.CURSOR_RESIZE_NESW,
            EngineSetting.CURSOR_DEFAULT,
            EngineSetting.CURSOR_RESIZE_V,
            EngineSetting.CURSOR_RESIZE_NESW,
            EngineSetting.CURSOR_RESIZE_NWSE,
            EngineSetting.CURSOR_DEFAULT,
            EngineSetting.CURSOR_DEFAULT,
            EngineSetting.CURSOR_DEFAULT,
            EngineSetting.CURSOR_DEFAULT,
            EngineSetting.CURSOR_DEFAULT };

    // Menus
    public static final String MENU_EDITOR_BASE = "editor/EditorWindow/Base";
    public static final String MENU_EDITOR_SECONDARY = "editor/EditorWindow/Secondary";
    public static final String MENU_EDITOR_TOOLBAR = "editor/EditorWindow/Toolbar";
    public static final String MENU_EDITOR_TITLE_BAR = "editor/EditorWindow/TitleBar";

    // Menu Bar — the main window's menus between the divider after File, Edit and Settings and the search: each its
    // preferred width while the bar has room, all shrinking together as it narrows, then wrapping onto more rows,
    // the bar growing and the dock shrinking beneath it; a divider closes them, and the search is centred between it
    // and the divider before Layouts, as wide as the room left allows within its bounds
    public static final String MENU_BAR_ELEMENT = "toolbar";
    public static final String[] MENU_BAR_MENU_ELEMENTS = {
            "tools_menu",
            "assets_menu",
            "characters_menu",
            "mesh_menu",
            "textures_menu",
            "materials_menu",
            "world_menu",
            "media_menu",
            "debug_menu" };
    public static final String MENU_BAR_END_DIVIDER_ELEMENT = "menus_divider";
    public static final float MENU_BAR_LEFT_PIXELS = 114f;
    public static final float MENU_BAR_RIGHT_EDGE_PIXELS = 240f;
    public static final float MENU_BAR_DIVIDER_WIDTH_PIXELS = 12f;
    public static final float MENU_BAR_SEARCH_GAP_PIXELS = 12f;
    public static final float MENU_BAR_SEARCH_MIN_WIDTH_PIXELS = 160f;
    public static final float MENU_BAR_SEARCH_MAX_WIDTH_PIXELS = 320f;
    public static final float MENU_BAR_RIGHT_RESERVE_PIXELS = MENU_BAR_RIGHT_EDGE_PIXELS
            + MENU_BAR_DIVIDER_WIDTH_PIXELS
            + 2f * MENU_BAR_SEARCH_GAP_PIXELS
            + MENU_BAR_SEARCH_MIN_WIDTH_PIXELS;
    public static final float MENU_BAR_MENU_WIDTH_PIXELS = 100f;
    public static final float MENU_BAR_MENU_MIN_WIDTH_PIXELS = 72f;
    public static final float MENU_BAR_ROW_PIXELS = 32f;
    public static final String MENU_BAR_DOCK_ELEMENT = "dock_canvas";
    public static final float MENU_BAR_DOCK_INSET_X_PIXELS = 8f;
    public static final float MENU_BAR_DOCK_INSET_Y_PIXELS = 60f;
    public static final String MENU_EDITOR_NAME_DIALOG = "editor/EditorWindow/NameDialog";

    // Search — the toolbar's search everywhere over the toolbar menus' items, and the Settings dialog's search over
    // its rows, both typed through SearchFieldStruct
    public static final int SEARCH_QUERY_MAX_LENGTH = 48;
    public static final String SEARCH_CARET = "|";
    public static final String SEARCH_EDITOR_PLACEHOLDER = "Search the editor";
    public static final String SEARCH_SETTINGS_PLACEHOLDER = "Search settings";
    public static final String SEARCH_SECTION_SUFFIX = "_header";
    public static final String SEARCH_PATH_SEPARATOR = " / ";
    public static final int SEARCH_RESULTS_MAX = 10;
    public static final float SEARCH_RESULT_ROW_PIXELS = 28f;
    public static final float SEARCH_RESULTS_PADDING_PIXELS = 14f;
    public static final float SEARCH_RESULTS_MIN_WIDTH_PIXELS = 320f;
    public static final int ENTRY_SEARCH_RESULTS = 4;
    public static final String MENU_EDITOR_SEARCH_RESULTS_PANEL = "editor/EditorWindow/search_results_panel";
    public static final String MENU_EDITOR_SEARCH_RESULT_TEMPLATE = "editor/EditorWindow/search_result_template";
    public static final String ELEMENT_SEARCH_MENU = "search_menu";
    public static final String ELEMENT_SEARCH_LABEL = "search_label";
    public static final String ELEMENT_SEARCH_RESULTS_CONTENT = "search_results_content";
    public static final String ELEMENT_SEARCH_RESULTS_LIST = "search_results_list";
    public static final String ELEMENT_SEARCH_RESULT_LABEL = "search_result_label";
    public static final String ELEMENT_SEARCH_RESULT_PATH = "search_result_path";
    public static final String ELEMENT_SETTINGS_SEARCH = "settings_dialog_search";
    public static final String ELEMENT_SETTINGS_SEARCH_LABEL = "settings_dialog_search_label";
    public static final String SETTINGS_STATUS_SEARCH_NONE = "No setting matches the search.";
    public static final String SETTINGS_STATUS_SEARCH_COUNT = " settings match the search.";
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
    public static final String ELEMENT_SETTINGS_SECTION_HEADER = "settings_section_header";
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
    public static final int KEY_PAGE_UP = 266;
    public static final int KEY_PAGE_DOWN = 267;

    // Entry point indices
    public static final int ENTRY_LAYOUTS_LIST = 0;
    public static final int ENTRY_LAYOUTS_PANEL = 5;

    // Layouts — a saved layout's row, the dropdown's width, and the room it takes besides its rows (New Layout, the
    // separator and the panel's insets), so the dropdown is always as tall as the layouts it lists
    public static final float LAYOUTS_ROW_PIXELS = 28f;
    public static final float LAYOUTS_DROPDOWN_WIDTH_PIXELS = 240f;
    public static final float LAYOUTS_DROPDOWN_BASE_PIXELS = 51f;
    public static final int ENTRY_FILE_MENU_LIST = 1;
    public static final int ENTRY_EDIT_MENU_LIST = 2;
    public static final int ENTRY_TOOLS_MENU_LIST = 3;

    // Tools menu — the labels of the shared tabs that serve another window, each named after the window it would
    // serve as the menu opens
    public static final String ELEMENT_TOOLS_MENU_PROPERTIES = "open_properties_label";
    public static final String ELEMENT_TOOLS_MENU_LAYERS = "open_layers_label";
    public static final String ELEMENT_TOOLS_MENU_TOOLS = "open_tools_label";
    public static final String ELEMENT_TOOLS_MENU_BRUSHES = "open_brushes_label";
    public static final String ELEMENT_TOOLS_MENU_STAMPS = "open_stamps_label";
    public static final String ELEMENT_TOOLS_MENU_PALETTE = "open_palette_label";
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

    // File Menu — the head naming the tool the File and Edit menus act on, and each action's item name and label,
    // by FileMenuAction's order
    public static final String ELEMENT_FILE_MENU_TARGET = "file_target_label";
    public static final String ELEMENT_EDIT_MENU_TARGET = "edit_target_label";
    public static final String FILE_MENU_NO_TARGET = "No tool worked in yet";
    public static final String[] FILE_MENU_NAMES = {
            "New",
            "Open",
            "Duplicate",
            "Save",
            "Save All",
            "Revert",
            "Delete",
            "Undo",
            "Redo",
            "Import",
            "Export" };
    public static final String[] FILE_MENU_LABELS = {
            "file_new_label",
            "file_open_label",
            "file_duplicate_label",
            "file_save_label",
            "file_save_all_label",
            "file_revert_label",
            "file_delete_label",
            "edit_undo_label",
            "edit_redo_label",
            "file_import_label",
            "file_export_label" };

    // File Menu — each tool's actions, the methods its own keys and buttons run
    public static final FileMenuTableStruct FILE_MENU_CHARACTER = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.CharacterEditorBranch")
            .with(FileMenuAction.NEW, "newModel")
            .with(FileMenuAction.OPEN, "openModel")
            .with(FileMenuAction.DUPLICATE, "copyCharacter")
            .with(FileMenuAction.SAVE, "saveCharacter")
            .with(FileMenuAction.REVERT, "revertCharacter")
            .with(FileMenuAction.DELETE, "deleteCharacter")
            .with(FileMenuAction.UNDO, "undo")
            .with(FileMenuAction.REDO, "redo")
            .with(FileMenuAction.IMPORT, "importModel")
            .with(FileMenuAction.EXPORT, "exportModel");
    public static final FileMenuTableStruct FILE_MENU_HAIR = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.CharacterEditorBranch")
            .with(FileMenuAction.NEW, "newHair")
            .with(FileMenuAction.OPEN, "openHair")
            .with(FileMenuAction.SAVE, "saveHair")
            .with(FileMenuAction.REVERT, "revertHair")
            .with(FileMenuAction.UNDO, "undoHair")
            .with(FileMenuAction.REDO, "redoHair")
            .with(FileMenuAction.IMPORT, "importHair")
            .with(FileMenuAction.EXPORT, "exportHair");
    public static final FileMenuTableStruct FILE_MENU_ANIMATION = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.AnimationEditorBranch")
            .with(FileMenuAction.NEW, "newClip")
            .with(FileMenuAction.SAVE, "saveClip")
            .with(FileMenuAction.REVERT, "revertClip")
            .with(FileMenuAction.DELETE, "deleteClip")
            .with(FileMenuAction.UNDO, "undo")
            .with(FileMenuAction.REDO, "redo");
    public static final FileMenuTableStruct FILE_MENU_ANIMATION_TREE = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.AnimationTreeEditorBranch")
            .with(FileMenuAction.NEW, "newTree")
            .with(FileMenuAction.SAVE, "saveTree")
            .with(FileMenuAction.REVERT, "revertTree")
            .with(FileMenuAction.DELETE, "deleteTree");
    public static final FileMenuTableStruct FILE_MENU_WORLD = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.WorldEditorBranch")
            .with(FileMenuAction.SAVE, "saveImage")
            .with(FileMenuAction.REVERT, "reloadImage")
            .with(FileMenuAction.UNDO, "undo")
            .with(FileMenuAction.REDO, "redo");
    public static final FileMenuTableStruct FILE_MENU_IMAGE_EDITOR = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.ImageEditorBranch")
            .with(FileMenuAction.NEW, "newImage")
            .with(FileMenuAction.OPEN, "openImage")
            .with(FileMenuAction.DUPLICATE, "copyImage")
            .with(FileMenuAction.SAVE, "saveImage")
            .with(FileMenuAction.SAVE_ALL, "saveAllImages")
            .with(FileMenuAction.REVERT, "revertImage")
            .with(FileMenuAction.DELETE, "deleteImage")
            .with(FileMenuAction.UNDO, "undo")
            .with(FileMenuAction.REDO, "redo")
            .with(FileMenuAction.IMPORT, "importImage")
            .with(FileMenuAction.EXPORT, "exportImage");
    public static final FileMenuTableStruct FILE_MENU_ITEM = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.ItemEditorBranch")
            .with(FileMenuAction.NEW, "newItem")
            .with(FileMenuAction.OPEN, "openItem")
            .with(FileMenuAction.SAVE, "saveItem")
            .with(FileMenuAction.REVERT, "reloadItem")
            .with(FileMenuAction.DELETE, "deleteItem")
            .with(FileMenuAction.IMPORT, "importItem")
            .with(FileMenuAction.EXPORT, "exportItem");
    public static final FileMenuTableStruct FILE_MENU_MATERIALS = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.MaterialsBranch")
            .with(FileMenuAction.NEW, "newMaterial")
            .with(FileMenuAction.OPEN, "openMaterial")
            .with(FileMenuAction.SAVE, "saveMaterial")
            .with(FileMenuAction.REVERT, "revertMaterial")
            .with(FileMenuAction.DELETE, "deleteMaterial")
            .with(FileMenuAction.UNDO, "undoMaterial")
            .with(FileMenuAction.REDO, "redoMaterial")
            .with(FileMenuAction.IMPORT, "importMaterial")
            .with(FileMenuAction.EXPORT, "exportMaterial");
    public static final FileMenuTableStruct FILE_MENU_INFO = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.InfoPanelBranch")
            .with(FileMenuAction.NEW, "newEntry")
            .with(FileMenuAction.DUPLICATE, "copySelection")
            .with(FileMenuAction.SAVE, "save")
            .with(FileMenuAction.SAVE_ALL, "saveAll")
            .with(FileMenuAction.REVERT, "revert")
            .with(FileMenuAction.DELETE, "deleteSelection");
    public static final FileMenuTableStruct FILE_MENU_RECORDER = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.RecorderBranch")
            .with(FileMenuAction.SAVE, "saveRecording")
            .with(FileMenuAction.UNDO, "undo")
            .with(FileMenuAction.REDO, "redo")
            .with(FileMenuAction.EXPORT, "exportMedia");
    public static final FileMenuTableStruct FILE_MENU_RECORDINGS = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.RecordingsBranch")
            .with(FileMenuAction.NEW, "newRecording")
            .with(FileMenuAction.DUPLICATE, "duplicateRecording")
            .with(FileMenuAction.SAVE, "saveRecording")
            .with(FileMenuAction.DELETE, "deleteRecording")
            .with(FileMenuAction.UNDO, "undo")
            .with(FileMenuAction.REDO, "redo");
    public static final FileMenuTableStruct FILE_MENU_TREES = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.TreeViewerBranch")
            .with(FileMenuAction.NEW, "newTree")
            .with(FileMenuAction.DUPLICATE, "duplicateTree")
            .with(FileMenuAction.SAVE, "save")
            .with(FileMenuAction.REVERT, "revert");
    public static final FileMenuTableStruct FILE_MENU_ARCHITECTURES = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.ArchitectureBuilderBranch")
            .with(FileMenuAction.NEW, "newArchitecture")
            .with(FileMenuAction.DUPLICATE, "duplicateArchitecture")
            .with(FileMenuAction.SAVE, "save")
            .with(FileMenuAction.REVERT, "revert");
    public static final FileMenuTableStruct FILE_MENU_STRUCTURES = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.StructureEditorBranch")
            .with(FileMenuAction.NEW, "newStructure")
            .with(FileMenuAction.DUPLICATE, "duplicateStructure")
            .with(FileMenuAction.SAVE, "save")
            .with(FileMenuAction.REVERT, "revert")
            .with(FileMenuAction.UNDO, "undo")
            .with(FileMenuAction.REDO, "redo");
    public static final FileMenuTableStruct FILE_MENU_BIOME_PLANNER = new FileMenuTableStruct(
            "editor.runtime.menueventsmanager.menus.BiomePlannerBranch")
            .with(FileMenuAction.NEW, "newBiome")
            .with(FileMenuAction.DUPLICATE, "duplicateBiome");
    public static final FileMenuTableStruct FILE_MENU_SHIPWRIGHT = new FileMenuTableStruct(
            "application.runtime.menueventsmanager.menus.shipwright.ShipwrightBranch")
            .with(FileMenuAction.NEW, "newShip")
            .with(FileMenuAction.SAVE, "saveShip")
            .with(FileMenuAction.UNDO, "undo")
            .with(FileMenuAction.REDO, "redo");

    // Name Dialog
    public static final int NAME_INPUT_MAX_LENGTH = 32;
    public static final int TEXT_INPUT_MAX_LENGTH = 256;
    public static final String DIALOG_TITLE_CREATE_LAYOUT = "New Layout";
    public static final String DIALOG_TITLE_NEW_ITEM = "New Item";
    public static final String DIALOG_TITLE_RENAME_PART = "Rename Part";
    public static final String DIALOG_TITLE_NEW_ENTRY = "New";
    public static final String DIALOG_TITLE_NEW_KEY = "New Key";
    public static final String DIALOG_TITLE_DUPLICATE_KEY = "Duplicate As";
    public static final String DIALOG_TITLE_EDIT_PREFIX = "Edit ";
    public static final String DIALOG_TITLE_DELETE_PREFIX = "Type ";
    public static final String DIALOG_TITLE_DELETE_SUFFIX = " to delete it";
    public static final String DIALOG_TITLE_SAVE_OVER_PREFIX = "Save over ";
    public static final String DIALOG_TITLE_SAVE_OVER_SUFFIX = "?";
    public static final String DIALOG_TITLE_COPY_PREFIX = "Copy ";
    public static final String DIALOG_TITLE_COPY_SUFFIX = " as";
    public static final String DIALOG_TITLE_UNIQUE_PREFIX = "Unique ";
    public static final String DIALOG_TITLE_EDIT_VALUE = "Edit Value";
    public static final String DIALOG_TITLE_NEW_IMAGE = "Name the New Image";
    public static final String DIALOG_TITLE_NEW_MATERIAL = "Name the New Material";
    public static final String DIALOG_TITLE_NEW_CLIP = "New Clip";
    public static final String DIALOG_TITLE_NEW_TREE = "New Animation Tree";
    public static final String DIALOG_TITLE_NEW_LAYER = "New Layer";
    public static final String DIALOG_TITLE_RENAME_LAYER = "Rename Layer";
    public static final String DIALOG_TITLE_NEW_NODE = "New Node";
    public static final String DIALOG_TITLE_RENAME_NODE = "Rename Node";
    public static final String DIALOG_TITLE_NEW_RECORDING = "New media file: type its name";
    public static final String DIALOG_TITLE_RENAME_RECORDING = "Rename Recording";
    public static final String DIALOG_TITLE_DUPLICATE_RECORDING = "Copy Recording As";
    public static final String DIALOG_TITLE_RENAME_TAKE = "Rename Take";
    public static final String DIALOG_TITLE_NEST_SELECTION = "Nest Selection As";
    public static final String DIALOG_TITLE_KEY_VALUE = "Key Value";
    public static final String DIALOG_TITLE_SOUND_VOLUME = "Sound Volume (%)";
    public static final String DIALOG_TITLE_LANE_NAME = "Lane Name (empty for its number)";
    public static final String DIALOG_TITLE_SOUND_PITCH = "Sound Pitch (%)";
    public static final String DIALOG_TITLE_SOUND_LENGTH = "Sound Length (seconds)";
    public static final String DIALOG_TITLE_TRANSITION_LENGTH = "Transition Length (seconds)";
    public static final String DIALOG_TITLE_PLACEMENT_X = "Placement X";
    public static final String DIALOG_TITLE_PLACEMENT_Z = "Placement Z";
    public static final String DIALOG_TITLE_CAMERA_NAME = "Camera Name";
    public static final String DIALOG_TITLE_CAMERA_HEIGHT = "Camera Height";
    public static final String DIALOG_TITLE_CAMERA_YAW = "Camera Yaw";
    public static final String DIALOG_TITLE_CAMERA_PITCH = "Camera Pitch";

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
    public static final String SETTINGS_GROUP_PREVIEW = "Preview";
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
    public static final String SETTINGS_SECTION_FRAME = "Window Frame";
    public static final String SETTINGS_NOTE_WINDOW = "Sets the editor's own window";
    public static final String SETTINGS_NOTE_SAVED = "Every setting is saved to EditorSettings when this dialog closes";
    public static final String SETTINGS_NOTE_PREVIEW = "Applies to every Preview tab";
    public static final String SETTINGS_NOTE_WORLD_APPLY = "World changes apply when this dialog closes";
    public static final String SETTINGS_NOTE_CONTROLS = "Click an action, then press its new key or button";
    public static final String SETTINGS_NOTE_SHORTCUTS =
            "Click a shortcut, then press its new keys; it saves to its tool's file";
    public static final String SETTINGS_NOTE_COLORS = "Click a color to edit it; the editor follows as you change it";

    // Settings Dialog — the active tab and a row waiting for keys are tinted with the accent
    public static final float SETTINGS_TAB_ACTIVE_ALPHA = 0.32f;
    public static final float SETTINGS_CAPTURE_ALPHA = 0.45f;

    // Sliders — every slider in the editor sets its value at one of this many steps along its track
    public static final int SLIDER_STEPS = 200;

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
    public static final String SETTINGS_COLOR_FRAME = "Title Bar";
    public static final String SETTINGS_COLOR_FRAME_OUTLINE = "Window Border";
    public static final String SETTINGS_COLOR_WINDOW_ACTIVE = "Active Window";
    public static final String SETTINGS_COLOR_WINDOW_HOVER = "Hovered Window";
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
    public static final String TAB_TITLE_STAMPS = "Stamps";
    public static final String TAB_TITLE_CAMERA_PLANNER = "Camera Planner";
    public static final String TAB_TITLE_CHARACTER_APPEARANCE = "Appearance";
    public static final String TAB_TITLE_CHARACTER_EQUIPMENT = "Equipment";
    public static final String TAB_TITLE_CHARACTER_MESH = "Mesh";
    public static final String TAB_TITLE_CHARACTER_SCULPT = "Sculpt";
    public static final String TAB_TITLE_CHARACTER_WEIGHTS = "Weights";
    public static final String TAB_TITLE_CHARACTER_PAINT = "Paint";
    public static final String TAB_TITLE_CHARACTER_RIG = "Rig";
    public static final String TAB_TITLE_CHARACTER_POSE = "Pose";
    public static final String TAB_TITLE_CHARACTER_UV = "UV";
    public static final String TAB_TITLE_CHARACTER_TOPOLOGY = "Topology";
    public static final String TAB_TITLE_CHARACTER_HAIR = "Hair";
    public static final String TAB_TITLE_MATERIALS = "Materials";
    public static final String TAB_TITLE_MATERIAL_VIEWER = "Material Viewer";
    public static final String TAB_TITLE_UV_EDITOR = "UV Editor";
    public static final String TAB_TITLE_CONSOLE = "Console";
    public static final String TAB_TITLE_HIERARCHY = "Hierarchy";
    public static final String TAB_TITLE_PROPERTIES = "Properties";
    public static final String TAB_TITLE_LAYERS = "Layers";
    public static final String TAB_TITLE_PALETTE = "Palette";
    public static final String TAB_TITLE_IMAGE_EDITOR = "Image Editor";
    public static final String TAB_TITLE_ITEM_EDITOR = "Item Editor";
    public static final String TAB_TITLE_PROFILER = "Profiler";
    public static final String TAB_TITLE_RECORDER = "Recorder";
    public static final String TAB_TITLE_TIMELINE = "Timeline";
    public static final String TAB_TITLE_RECORDER_KEYS = "Keys";
    public static final String TAB_TITLE_RECORDER_KEY_GRAPH = "Keys Graph";
    public static final String TAB_TITLE_RECORDINGS = "Recordings";
    public static final String TAB_TITLE_SOUND_LIBRARY = "Sound Library";
    public static final String TAB_TITLE_NOTE_LIBRARY = "Note Library";
    public static final String TAB_TITLE_SCENE_ASSETS = "Scene Assets";
    public static final String TAB_TITLE_SETTLEMENT_PLANNER = "Settlement Planner";
    public static final String TAB_TITLE_SHIPWRIGHT = "Shipwright";
    public static final String TAB_TITLE_SHIP_VIEWER = "Ship Viewer";
    public static final String TAB_TITLE_TOOLS = "Tools";
    public static final String TAB_TITLE_TRANSITIONS = "Transitions";
    public static final String TAB_TITLE_FILTERS = "Filters";
    public static final String TAB_TITLE_TREE_PLANNER = "Tree Planner";
    public static final String TAB_TITLE_ARCHITECTURE_BUILDER = "Architecture Builder";
    public static final String TAB_TITLE_STRUCTURE_EDITOR = "Structure Editor";
    public static final String TAB_TITLE_TREE_VIEWER = "Tree Viewer";
    public static final String TAB_TITLE_WORLD_EDITOR = "World Editor";
    public static final String TAB_TITLE_WORLD_MAP = "World Map";

    // Shortcuts
    public static final String SHORTCUT_PATH = "shortcuts";
    public static final String SHORTCUT_NAME_SEPARATOR = "/";

    // Hint Bar
    public static final String HINT_BAR_ENTRY_SEPARATOR = "     ";
    public static final String HINT_BAR_CONTEXT_OPEN = "(";
    public static final int HINT_BAR_ENTRY_LABEL = 0;
    public static final String HINT_BAR_KEY_SEPARATOR = ": ";
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
    public static final String COMMAND_MESSAGE_TIME_AFTER_CALENDAR =
            ": cannot move the clock past the last day of age ";
    public static final String COMMAND_MESSAGE_TIME_MOVED = ": the clock now reads ";

    // Commands — Character, run in the character tabs on the character open there; left is +x
    public static final String COMMAND_MIRROR_BODY_LEFT = "mirrorbodyleft";
    public static final String COMMAND_MIRROR_BODY_RIGHT = "mirrorbodyright";
    public static final String COMMAND_MIRROR_HAIR_LEFT = "mirrorhairleft";
    public static final String COMMAND_MIRROR_HAIR_RIGHT = "mirrorhairright";
    public static final String COMMAND_MESSAGE_NO_CHARACTER_ACTION = "The character tabs have no action for command: ";
    public static final String COMMAND_MESSAGE_NO_PART_MESH =
            "No character is open in the character tabs, or it wears no mesh of its own for its ";

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
    public static final String FBO_TREE_IMPOSTOR_BAKE = "TreeImpostorBake";
    public static final String FBO_EDITOR_OVERLAY = "EditorOverlay";
    public static final String MESH_EDITOR_VIEWPORT_QUAD = "editor/EditorViewportQuad";
    public static final String MESH_EDITOR_OVERLAY_VAO = "editor/CharacterMeshOverlayVAO";

    // Flat View — the view of the UV Editor and the Image Editor over their unit square: the share of the window the
    // square fills when fitted, the zoom step per wheel notch, how far in and out it may zoom in window pixels per
    // unit, the button that pans it and the uniform its mapping is handed to the shaders by
    public static final float FLAT_VIEW_FIT_SHARE = 0.85f;
    public static final float FLAT_VIEW_ZOOM_STEP = 1.15f;
    public static final float FLAT_VIEW_MIN_PIXELS_PER_UNIT = 32f;
    public static final float FLAT_VIEW_MAX_PIXELS_PER_UNIT = 65536f;
    public static final int FLAT_VIEW_BUTTON_PAN = Buttons.RIGHT;
    public static final int FLAT_VIEW_BUTTON_HAND = Buttons.LEFT;
    public static final String FLAT_VIEW_UNIFORM_UV_TO_CLIP = "u_uvToClip";

    // View Camera — the one camera of every 3D viewport: the orbit button turns it and the pan button slides it, a
    // drag begun on a press no tool claimed; each notch of the wheel zooms by a factor; distances are in the bounding
    // radius of what is framed; a view snapped straight up or down leans this far so the camera keeps its heading
    public static final int VIEW_CAMERA_BUTTON_ORBIT = Buttons.LEFT;
    public static final int VIEW_CAMERA_BUTTON_PAN = Buttons.RIGHT;
    public static final float VIEW_CAMERA_DISTANCE_DEFAULT = 2.2f;
    public static final float VIEW_CAMERA_DISTANCE_MIN = 0.15f;
    public static final float VIEW_CAMERA_DISTANCE_MAX = 8f;
    public static final float VIEW_CAMERA_ZOOM_FACTOR = 1.12f;
    public static final float VIEW_CAMERA_NEAR_SHARE = 0.02f;
    public static final float VIEW_CAMERA_NEAR_MIN = 0.001f;
    public static final float VIEW_CAMERA_INITIAL_YAW_DEGREES = 35f;
    public static final float VIEW_CAMERA_INITIAL_PITCH_DEGREES = 18f;
    public static final float VIEW_CAMERA_VERTICAL_LEAN = 0.02f;

    // View Camera Share — the kind of view a viewport hands on to the tabs stacked with it; none keeps it its own
    public static final String VIEW_CAMERA_SHARE_NONE = "";
    public static final String VIEW_CAMERA_SHARE_CHARACTER = "character";

    // View Gizmo — the axis gizmo in a viewport's corner (ViewGizmoSystem), as Blender's navigation gizmo: each axis a
    // bubble where it points on screen out to a radius in pixels, placed in steps across the gizmo, the positive ones
    // joined to the middle by an arm in pixel steps of length and whole degrees of turn; a click looks back along it,
    // or along its opposite when the view already does
    public static final String VIEW_GIZMO_MENU = "editor/ViewGizmo/ViewGizmo";
    public static final String[] VIEW_GIZMO_AXES = { "x", "-x", "y", "-y", "z", "-z" };
    public static final float[][] VIEW_GIZMO_AXIS_VECTORS = {
            { 1f, 0f, 0f }, { -1f, 0f, 0f }, { 0f, 1f, 0f }, { 0f, -1f, 0f }, { 0f, 0f, 1f }, { 0f, 0f, -1f } };
    public static final int VIEW_GIZMO_ENTRY_BUBBLES = 0;
    public static final int VIEW_GIZMO_ENTRY_ARMS = VIEW_GIZMO_AXES.length;
    public static final float VIEW_GIZMO_RADIUS_PIXELS = 32f;
    public static final int VIEW_GIZMO_STEPS = 33;
    public static final float VIEW_GIZMO_ARM_PIXELS = 2f;
    public static final float VIEW_GIZMO_FACING_COSINE = 0.999f;

    // Transform Gizmo — the handles drawn on a selection while Move, Rotate or Scale is the tool, as every 3D tool
    // shows them: each axis's handle as long on screen as the length in pixels, in its colour or the hovered colour,
    // a handle under the pointer within the pick distance taking a press as a drag held to its axis; a Move handle is
    // a shaft and an octahedral arrowhead, a Rotate handle a ring of segments, a Scale handle a shaft ending in a cube,
    // each part's size a share of the handle's length
    public static final float TRANSFORM_GIZMO_PIXELS = 72f;
    public static final float TRANSFORM_GIZMO_PICK_PIXELS = 8f;
    public static final float TRANSFORM_GIZMO_SHAFT_SHARE = 0.02f;
    public static final float TRANSFORM_GIZMO_HEAD_SHARE = 0.22f;
    public static final float TRANSFORM_GIZMO_HEAD_WIDTH_SHARE = 0.6f;
    public static final float TRANSFORM_GIZMO_CUBE_SHARE = 0.12f;
    public static final int TRANSFORM_GIZMO_RING_SEGMENTS = 32;
    public static final float[][] TRANSFORM_GIZMO_COLORS = {
            { 0.92f, 0.28f, 0.26f, 0.95f }, { 0.45f, 0.78f, 0.22f, 0.95f }, { 0.26f, 0.52f, 0.95f, 0.95f } };
    public static final float[] TRANSFORM_GIZMO_HOVER_COLOR = { 1f, 0.85f, 0.2f, 1f };

    // Character View — meshes, materials and draw order
    public static final String CHARACTER_VIEW_MESH_GRID = "editor/CharacterViewGrid";
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
    public static final float CHARACTER_VIEW_STICK_MIN_LENGTH = 0.0001f;
    public static final float CHARACTER_VIEW_STICK_PARALLEL = 0.9f;

    // Character View — marker shapes, by the index the marker pools and bone styles name them with
    public static final int CHARACTER_VIEW_MARKER_CUBE = 0;
    public static final int CHARACTER_VIEW_MARKER_OCTAHEDRAL = 1;
    public static final int CHARACTER_VIEW_MARKER_SPHERE = 2;
    public static final String[] CHARACTER_VIEW_MARKER_MESHES = {
            "editor/CharacterViewMarker", "editor/CharacterViewBoneOctahedral", "editor/CharacterViewBoneSphere" };
    public static final float[] CHARACTER_VIEW_MARKER_COLOR = { 0.55f, 0.75f, 1.0f, 0.35f };
    public static final String CHARACTER_VIEW_UNIFORM_COLOR = "u_color";

    // Bone Display — how every character viewport draws the bones (CharacterBoneDisplay, CharacterBoneKind), as
    // Blender's armature display does: each style's widths in blocks or shares, and the named bone colours
    public static final String BONE_DISPLAY_HEADER = "Bone Display";
    public static final String BONE_DISPLAY_KEY_STYLE = "bone_display_style";
    public static final String BONE_DISPLAY_KEY_BODY = "bone_display_body";
    public static final String BONE_DISPLAY_KEY_FACE = "bone_display_face";
    public static final String BONE_DISPLAY_KEY_HAIR = "bone_display_hair";
    public static final String BONE_DISPLAY_LABEL_STYLE = "Style";
    public static final String BONE_DISPLAY_LABEL_BODY = "Body Bones";
    public static final String BONE_DISPLAY_LABEL_FACE = "Face Bones";
    public static final String BONE_DISPLAY_LABEL_HAIR = "Hair Bones";
    public static final String BONE_DISPLAY_LABEL_OCTAHEDRAL = "Octahedral";
    public static final String BONE_DISPLAY_LABEL_STICK = "Stick";
    public static final String BONE_DISPLAY_LABEL_BBONE = "B-Bone";
    public static final String BONE_DISPLAY_LABEL_ENVELOPE = "Envelope";
    public static final String BONE_DISPLAY_LABEL_WIRE = "Wire";
    public static final String BONE_DISPLAY_LABEL_HIDDEN = "Hidden";
    public static final float BONE_DISPLAY_OCTAHEDRAL_LENGTH_SHARE = 0.2f;
    public static final float BONE_DISPLAY_STICK_WIDTH = 0.012f;
    public static final float BONE_DISPLAY_BBONE_SIZE_SHARE = 1f;
    public static final float BONE_DISPLAY_WIRE_WIDTH = 0.003f;
    public static final float BONE_DISPLAY_ENVELOPE_SIZE_SHARE = 0.5f;
    public static final float BONE_DISPLAY_ALPHA = 0.55f;
    public static final String[] BONE_DISPLAY_COLOR_NAMES = {
            "Grey", "White", "Red", "Orange", "Yellow", "Green", "Teal", "Blue", "Violet", "Pink" };
    public static final float[][] BONE_DISPLAY_COLORS = {
            { 0.62f, 0.66f, 0.72f }, { 0.95f, 0.95f, 0.95f }, { 0.9f, 0.25f, 0.25f }, { 0.95f, 0.55f, 0.18f },
            { 0.95f, 0.85f, 0.25f }, { 0.35f, 0.8f, 0.35f }, { 0.25f, 0.75f, 0.75f }, { 0.35f, 0.55f, 0.95f },
            { 0.65f, 0.4f, 0.9f }, { 0.95f, 0.45f, 0.75f } };
    public static final int BONE_DISPLAY_DEFAULT_BODY = 7;
    public static final int BONE_DISPLAY_DEFAULT_FACE = 9;
    public static final int BONE_DISPLAY_DEFAULT_HAIR = 3;

    // Character View — the preview's build between thin (0) and heavy (1)
    public static final float CHARACTER_VIEW_WEIGHT_RATIO = 0.5f;

    // Character View — the frame, its middle a share of the model's height up and its radius reaching the feet
    public static final float CHARACTER_VIEW_FRAME_HEIGHT_SHARE = 0.55f;

    // Character View — a number row's slider spans this many of its value's steps, centred where the value stood
    // when the row was listed
    public static final int CHARACTER_VIEW_SLIDER_STEPS = 40;

    // Character View Fold — the label in every tool's header row template that shows whether its rows are folded
    public static final String CHARACTER_VIEW_FOLD_ELEMENT = "row_fold_label";

    // Character View — panel keys, numbers and text
    public static final String CHARACTER_VIEW_KEY_SEPARATOR = ":";
    public static final float CHARACTER_VIEW_DECIMAL_SCALE = 10000f;
    public static final int CHARACTER_VIEW_LABEL_SLACK = 4;
    public static final char CHARACTER_VIEW_RESOURCE_SEPARATOR = '/';
    public static final char CHARACTER_VIEW_WORD_SEPARATOR = ' ';
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

    // Character View — docker windows: the menu each fills, its title and row entries, and the titles they show
    public static final String CHARACTER_VIEW_MENU_DOCK = "editor/CharacterDock/Dock";
    public static final int CHARACTER_VIEW_DOCK_ENTRY_TITLE = 0;
    public static final int CHARACTER_VIEW_DOCK_ENTRY_ROWS = 1;
    public static final String CHARACTER_PROPERTIES_TITLE = "Properties";
    public static final String ANIMATION_PROPERTIES_TITLE = "Key";

    // Character View — the row templates of each tool's menu its docker windows fill with
    public static final CharacterViewTemplateStruct CHARACTER_EDITOR_ROW_TEMPLATES = new CharacterViewTemplateStruct(
            "editor/CharacterEditor/character_editor_header_row",
            "editor/CharacterEditor/character_editor_item_row",
            "editor/CharacterEditor/character_editor_item_row_selected",
            "editor/CharacterEditor/character_editor_number_row",
            "editor/CharacterEditor/character_editor_text_row",
            "editor/CharacterEditor/character_editor_choice_row",
            "editor/CharacterEditor/character_editor_action_row",
            "editor/CharacterEditor/character_editor_pair_row",
            "editor/CharacterEditor/character_editor_eye_row",
            "editor/CharacterEditor/character_editor_eye_row_selected",
            "character_editor_row_label",
            "character_editor_row_value",
            "character_editor_row_value_button",
            "character_editor_row_decrease",
            "character_editor_row_increase",
            "character_editor_row_track",
            "character_editor_row_knob",
            "character_editor_row_pair_label",
            "character_editor_row_pair_value",
            "character_editor_row_pair_value_button",
            "character_editor_row_eye",
            "character_editor_row_eye_icon");
    public static final CharacterViewTemplateStruct IMAGE_EDITOR_ROW_TEMPLATES = new CharacterViewTemplateStruct(
            "editor/ImageEditor/image_editor_header_row",
            "editor/ImageEditor/image_editor_item_row",
            "editor/ImageEditor/image_editor_item_row_selected",
            "editor/ImageEditor/image_editor_number_row",
            "editor/ImageEditor/image_editor_text_row",
            "editor/ImageEditor/image_editor_choice_row",
            "editor/ImageEditor/image_editor_action_row",
            "editor/ImageEditor/image_editor_pair_row",
            "image_editor_row_label",
            "image_editor_row_value",
            "image_editor_row_value_button",
            "image_editor_row_decrease",
            "image_editor_row_increase",
            "image_editor_row_track",
            "image_editor_row_knob",
            "image_editor_row_pair_label",
            "image_editor_row_pair_value",
            "image_editor_row_pair_value_button");
    public static final CharacterViewTemplateStruct ANIMATION_EDITOR_ROW_TEMPLATES = new CharacterViewTemplateStruct(
            "editor/AnimationEditor/animation_editor_header_row",
            "editor/AnimationEditor/animation_editor_item_row",
            "editor/AnimationEditor/animation_editor_item_row_selected",
            "editor/AnimationEditor/animation_editor_number_row",
            null,
            "editor/AnimationEditor/animation_editor_choice_row",
            "editor/AnimationEditor/animation_editor_action_row",
            null,
            "animation_editor_row_label",
            "animation_editor_row_value",
            "animation_editor_row_value_button",
            "animation_editor_row_decrease",
            "animation_editor_row_increase",
            "animation_editor_row_track",
            "animation_editor_row_knob",
            null,
            null,
            null);

    // Character View — viewport gestures: the buttons, the GLFW keys a transform locks its axis with, pointer
    // distances in pixels (a click picks the nearest vertex as far off as Blender's select distance), snapping, and
    // the undo history they record
    public static final int CHARACTER_VIEW_BUTTON_SELECT = Buttons.LEFT;
    public static final int CHARACTER_VIEW_BUTTON_CANCEL = Buttons.RIGHT;
    public static final int[] CHARACTER_VIEW_KEY_AXES = { 88, 89, 90 };
    public static final float CHARACTER_VIEW_PICK_RADIUS_PIXELS = 12f;
    public static final float CHARACTER_VIEW_CLICK_PICK_RADIUS_PIXELS = 75f;
    public static final float CHARACTER_VIEW_PICK_SEGMENT_PIXELS = 7f;
    public static final float CHARACTER_VIEW_SCALE_MIN_PIXELS = 4f;
    public static final float CHARACTER_VIEW_SNAP_DEGREES = 15f;
    public static final float CHARACTER_VIEW_SNAP_SCALE = 0.1f;
    public static final float CHARACTER_VIEW_RAY_EPSILON = 0.00001f;
    public static final float CHARACTER_VIEW_GIMBAL_LIMIT = 0.99999f;
    public static final float CHARACTER_VIEW_HALF_TURN_DEGREES = 180f;
    public static final int CHARACTER_VIEW_HISTORY_MAX_STEPS = 64;

    // glTF — the binary container, component and accessor codes of the glTF 2.0 subset Import and Export use
    public static final String GLTF_FILE_EXTENSION = "glb";
    public static final int GLTF_MAGIC = 0x46546C67;
    public static final int GLTF_VERSION = 2;
    public static final int GLTF_CHUNK_JSON = 0x4E4F534A;
    public static final int GLTF_CHUNK_BIN = 0x004E4942;
    public static final int GLTF_HEADER_BYTES = 12;
    public static final int GLTF_CHUNK_HEADER_BYTES = 8;
    public static final int GLTF_ALIGNMENT = 4;
    public static final String GLTF_ASSET_VERSION = "2.0";
    public static final String GLTF_GENERATOR = "AdventureRPG";
    public static final String GLTF_EXTENSION_NGON = "FB_ngon_encoding";
    public static final String GLTF_ATTRIBUTE_MASKS = "_MASKS";
    public static final int GLTF_COMPONENT_BYTE = 5120;
    public static final int GLTF_COMPONENT_UNSIGNED_BYTE = 5121;
    public static final int GLTF_COMPONENT_SHORT = 5122;
    public static final int GLTF_COMPONENT_UNSIGNED_SHORT = 5123;
    public static final int GLTF_COMPONENT_UNSIGNED_INT = 5125;
    public static final int GLTF_COMPONENT_FLOAT = 5126;
    public static final int GLTF_TARGET_ARRAY_BUFFER = 34962;
    public static final int GLTF_TARGET_ELEMENT_ARRAY_BUFFER = 34963;
    public static final int GLTF_MODE_TRIANGLES = 4;
    public static final String GLTF_TYPE_SCALAR = "SCALAR";
    public static final String GLTF_TYPE_VEC2 = "VEC2";
    public static final String GLTF_TYPE_VEC3 = "VEC3";
    public static final String GLTF_TYPE_VEC4 = "VEC4";
    public static final String GLTF_TYPE_MAT4 = "MAT4";
    public static final int GLTF_MATRIX_FLOATS = 16;
    public static final float GLTF_QUAD_FLAT_COSINE = 0.9f;
    public static final float GLTF_QUAD_NEIGHBOUR_COSINE = -1f;
    public static final int[] GLTF_QUAD_FAN_CORNERS = { 0, 1, 2, 0, 2, 3 };
    public static final String GLTF_MORPH_NAME_PREFIX = "target_";

    // Model Import and Export — the workspace folder .glb files are read from and written to, the
    // dialogs Import asks through and what both report
    public static final String MODEL_USER_DIRECTORY = "Models";
    public static final String MODEL_CONFIRM_ADD_BONES = "add";
    public static final String MODEL_DIALOG_ADD_BONES_PREFIX = "The rig lacks ";
    public static final String MODEL_DIALOG_ADD_BONES_SUFFIX = ". Type add to add them to the rig";
    public static final String MODEL_DIALOG_HANG_BONE = "The model has no skin. Type the bone it hangs from";
    public static final String MODEL_DIALOG_TEXTURE_PREFIX = "Type the texture for material ";
    public static final String MODEL_DIALOG_ZONE_PREFIX = "Type the zone (skin, eye, brow, lash, hair, cloth, mouth, "
            + "teeth) for material ";
    public static final String MODEL_MESSAGE_NO_MESH = "Open a character's mesh first";
    public static final String MODEL_MESSAGE_EXPORTED = "Exported ";
    public static final String MODEL_MESSAGE_EXPORT_FAILED = "The model could not be written to ";
    public static final String MODEL_MESSAGE_REFUSED = "Import refused: ";
    public static final String MODEL_MESSAGE_BONES_ADDED = " bones added to the rig. Run Import again";
    public static final String MODEL_MESSAGE_IMPORTED = "Imported ";
    public static final String MODEL_MESSAGE_IMPORTED_QUADS = " quads from ";
    public static final String MODEL_UNNAMED_MATERIAL = "(unnamed)";

    // Character Editor — race fields, and the field naming an entity's race
    public static final String CHARACTER_EDITOR_FIELD_RACE = "race";
    public static final String CHARACTER_EDITOR_FIELD_APPEARANCE = "appearance";
    public static final String CHARACTER_EDITOR_FIELD_FEATURES = "features";
    public static final String CHARACTER_EDITOR_FIELD_FEATURE_VARIANTS = "feature_variants";
    public static final String CHARACTER_EDITOR_FIELD_GENDERS = "genders";
    public static final String CHARACTER_EDITOR_FIELD_RANGES = "ranges";
    public static final String CHARACTER_EDITOR_FIELD_MIN = "min";
    public static final String CHARACTER_EDITOR_FIELD_MAX = "max";
    public static final String CHARACTER_EDITOR_FIELD_SHAPES = "shapes";
    public static final String CHARACTER_EDITOR_FIELD_OVERLAYS = "overlays";
    public static final String CHARACTER_EDITOR_FIELD_DEFAULT = "default";
    public static final String CHARACTER_EDITOR_FIELD_COLORS = "colors";
    public static final String CHARACTER_EDITOR_FIELD_PALETTES = "palettes";
    public static final String CHARACTER_EDITOR_FIELD_DETAILS = "details";
    public static final String CHARACTER_EDITOR_FIELD_BUILD = "build";
    public static final String CHARACTER_EDITOR_FIELD_THIN = "thin";
    public static final String CHARACTER_EDITOR_FIELD_HEAVY = "heavy";
    public static final String CHARACTER_EDITOR_FIELD_BONES = "bones";
    public static final String CHARACTER_EDITOR_FIELD_SLIDERS = "sliders";
    public static final String CHARACTER_EDITOR_FIELD_PLAYER = "player";
    public static final String CHARACTER_EDITOR_FIELD_PLAYABLE = "playable";
    public static final String CHARACTER_EDITOR_FIELD_EQUIPMENT = "equipment";
    public static final String CHARACTER_EDITOR_FIELD_SLOT = "slot";
    public static final String CHARACTER_EDITOR_FIELD_BONE = "bone";
    public static final String CHARACTER_EDITOR_FIELD_POSITION = "position";
    public static final String CHARACTER_EDITOR_FIELD_ROTATION = "rotation";
    public static final String CHARACTER_EDITOR_FIELD_SIZE = "size";
    public static final String CHARACTER_EDITOR_FIELD_HOLD = "hold";

    // Character Editor — panel keys
    public static final String CHARACTER_EDITOR_KEY_POSE = "pose";
    public static final String CHARACTER_EDITOR_KEY_SIDE = "side";
    public static final String CHARACTER_EDITOR_KEY_OVERLAY_STYLE = "overlay_style";
    public static final String CHARACTER_EDITOR_KEY_GENDER = "gender";
    public static final String CHARACTER_EDITOR_KEY_PLAYABLE = "playable";
    public static final String CHARACTER_EDITOR_KEY_OFFERED = "offered";
    public static final String CHARACTER_EDITOR_OFFERED_SEPARATOR = "/";
    public static final String CHARACTER_EDITOR_KEY_FEATURE = "feature";
    public static final String CHARACTER_EDITOR_KEY_FEATURE_VARIANT = "feature_variant";
    public static final String CHARACTER_EDITOR_KEY_COLOR_PREFIX = "color_";
    public static final String CHARACTER_EDITOR_KEY_DETAIL = "detail";
    public static final String CHARACTER_EDITOR_KEY_SHAPE = "shape";
    public static final String CHARACTER_EDITOR_KEY_SHAPE_MIN = "shape_min";
    public static final String CHARACTER_EDITOR_KEY_SHAPE_MAX = "shape_max";
    public static final String CHARACTER_EDITOR_KEY_SHAPE_PLAYER = "shape_player";
    public static final String CHARACTER_EDITOR_KEY_SHAPE_PICK = "shape_pick";
    public static final String CHARACTER_EDITOR_KEY_DRIVE_ADJUST = "drive_adjust";
    public static final String CHARACTER_EDITOR_KEY_DRIVE_INCREASE = "drive_increase";
    public static final String CHARACTER_EDITOR_KEY_CONTROL_ADD = "control_add";
    public static final String CHARACTER_EDITOR_KEY_CONTROL_REMOVE = "control_remove";
    // A drive's key holds its control's slider number times the stride plus the drive's place among the control's
    public static final int CHARACTER_EDITOR_DRIVE_KEY_STRIDE = 256;
    public static final String CHARACTER_EDITOR_KEY_BLINK = "blink";
    public static final String CHARACTER_EDITOR_KEY_EDIT_SHAPE = "edit_shape";
    public static final String CHARACTER_EDITOR_KEY_ADD_SHAPE = "add_shape";
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
    public static final String CHARACTER_EDITOR_KEY_GEAR = "gear";
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
    public static final float CHARACTER_EDITOR_DETAIL_STEP = 0.05f;
    public static final float CHARACTER_EDITOR_DETAIL_MIN = 0f;
    public static final float CHARACTER_EDITOR_DETAIL_MAX = 2f;
    public static final float CHARACTER_EDITOR_SHAPE_STEP = 0.05f;
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
    public static final String CHARACTER_EDITOR_MODE_EQUIPMENT = "Equipment";
    public static final String CHARACTER_EDITOR_MODE_MESH = "Mesh";
    public static final String CHARACTER_EDITOR_MODE_SCULPT = "Sculpt";
    public static final String CHARACTER_EDITOR_MODE_WEIGHTS = "Weights";
    public static final String CHARACTER_EDITOR_MODE_TOPOLOGY = "Topology";
    public static final String CHARACTER_EDITOR_MODE_HAIR = "Hair";
    public static final String CHARACTER_EDITOR_MODE_RIG = "Rig";
    public static final String CHARACTER_EDITOR_MODE_PAINT = "Paint";
    public static final String CHARACTER_EDITOR_MODE_UV = "UV";
    public static final String CHARACTER_EDITOR_MODE_POSE = "Pose";
    public static final String CHARACTER_EDITOR_POSE_REST = "Bind";
    public static final String CHARACTER_EDITOR_POSE_IDLE = "Idle";
    public static final String CHARACTER_EDITOR_POSE_WALK = "Walk";
    public static final String CHARACTER_EDITOR_POSE_RUN = "Run";

    // Character Editor — panel text
    public static final String CHARACTER_EDITOR_HEADER_PREVIEW = "Preview";
    public static final String CHARACTER_EDITOR_HEADER_CREATOR = "Creator";
    public static final String CHARACTER_EDITOR_LABEL_PLAYABLE = "Playable";
    public static final String CHARACTER_EDITOR_LABEL_OVERLAY_AMOUNT = "Amount";
    public static final String CHARACTER_EDITOR_HEADER_FEATURES = "Features";
    public static final String CHARACTER_EDITOR_FEATURE_DEFAULT_SUFFIX = " (race default)";
    public static final String CHARACTER_EDITOR_VARIANT_DEFAULT_SUFFIX = " (feature default)";
    public static final String CHARACTER_EDITOR_LABEL_VARIANT_SUFFIX = "_variant";
    public static final String CHARACTER_EDITOR_HEADER_NO_APPEARANCE = "No appearance block";
    public static final String[] CHARACTER_EDITOR_COLOR_HEADERS = {
            "Skin", "Eyes", "Hair", "Brows", "Lips", "Blush", "Freckles", "Clothing", "Nipples",
            "Body Hair", "Teeth", "Eyeshadow", "Eyeliner" };
    public static final String[] CHARACTER_EDITOR_DETAIL_LABELS = {
            "Iris Size", "Pupil Size", "Hair Shine", "Lips", "Blush", "Freckles", "Eyeliner", "Blemishes",
            "Brow Thickness", "Nipples", "Chest Hair", "Stomach Hair", "Pubic Hair", "Underarm Hair", "Arm Hair",
            "Leg Hair", "Eyeshadow" };
    public static final String CHARACTER_EDITOR_HEADER_DETAILS = "Details";
    public static final String CHARACTER_EDITOR_HEADER_OVERLAYS = "Marks";
    public static final String CHARACTER_EDITOR_HEADER_SHAPE_KEYS = "Shape Keys";
    public static final String CHARACTER_EDITOR_LABEL_BLINK = "Blink";
    public static final String CHARACTER_EDITOR_LABEL_SHAPE_MIN = "Min";
    public static final String CHARACTER_EDITOR_LABEL_SHAPE_MAX = "Max";
    public static final String CHARACTER_EDITOR_LABEL_SHAPE_PLAYER = "Player";
    public static final String CHARACTER_EDITOR_LABEL_SHAPE_PICK = "Pick For Control";
    public static final String CHARACTER_EDITOR_LABEL_DRIVE_PREFIX = "Drives ";
    public static final String CHARACTER_EDITOR_LABEL_DRIVE_INCREASE = "Increase";
    public static final String CHARACTER_EDITOR_LABEL_CONTROL_ADD = "Add Control";
    public static final String CHARACTER_EDITOR_LABEL_CONTROL_REMOVE = "Remove Control";
    public static final String CHARACTER_EDITOR_LABEL_SLIDER_JOIN = " ";
    public static final String CHARACTER_EDITOR_SWITCH_ON = "On";
    public static final String CHARACTER_EDITOR_SWITCH_OFF = "Off";
    public static final String CHARACTER_EDITOR_LABEL_EDIT_SHAPE = "Sculpting";
    public static final String CHARACTER_EDITOR_LABEL_ADD_SHAPE = "Add Shape Key";
    public static final String CHARACTER_EDITOR_SHAPE_BASE = "Base";
    public static final String CHARACTER_EDITOR_SHAPE_NAME_PREFIX = "shape_";
    public static final String CHARACTER_EDITOR_WORD_SEPARATOR = "_";
    public static final String CHARACTER_EDITOR_HEADER_BONES = "Bones";
    public static final String CHARACTER_EDITOR_HEADER_BUILD_CURVE = "Build Curve";
    public static final String CHARACTER_EDITOR_HEADER_PREVIEW_BODY = "Preview Body";
    public static final String CHARACTER_EDITOR_HEADER_ANCHORS = "Anchors";
    public static final String CHARACTER_EDITOR_HEADER_GEAR = "Gear";
    public static final String CHARACTER_EDITOR_HEADER_ANCHOR = "Anchor";
    public static final String CHARACTER_EDITOR_LABEL_POSE = "Pose";
    public static final String CHARACTER_EDITOR_LABEL_SIDE = "Side";
    public static final String CHARACTER_EDITOR_SIDE_LEFT = "Left";
    public static final String CHARACTER_EDITOR_SIDE_RIGHT = "Right";
    public static final String CHARACTER_EDITOR_LABEL_GENDER = "Gender";
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

    // Character Editor — how many steps a slider's own range slides across, never finer than the editor's
    // precision
    public static final float CHARACTER_EDITOR_SLIDER_STEPS = 100f;

    // Character Editor — status text
    public static final String CHARACTER_EDITOR_STATUS_NO_TEMPLATE = "No character open."
            + " Pick an entity or a race in the Hierarchy.";
    public static final String CHARACTER_EDITOR_MESSAGE_SAVED = "Saved ";
    public static final String CHARACTER_EDITOR_MESSAGE_REVERTED = "Reverted ";
    public static final String CHARACTER_EDITOR_MESSAGE_COPIED = "Copied as ";
    public static final String CHARACTER_EDITOR_MESSAGE_DELETED = "Deleted ";

    // Character Editor — mesh parts; a feature part is keyed by its slot's name
    public static final String CHARACTER_EDITOR_PART_BODY = "body";

    // Character Editor — mesh panel keys
    public static final String CHARACTER_EDITOR_KEY_MESH_PART = "mesh_part";
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
    public static final String CHARACTER_EDITOR_KEY_SELECT_MORE = "select_more";
    public static final String CHARACTER_EDITOR_KEY_SELECT_LESS = "select_less";
    public static final int CHARACTER_LOOP_VALENCE = 4;
    public static final String CHARACTER_EDITOR_KEY_HIDE = "hide";
    public static final String CHARACTER_EDITOR_KEY_HIDE_UNSELECTED = "hide_unselected";
    public static final String CHARACTER_EDITOR_KEY_REVEAL = "reveal";
    public static final String CHARACTER_EDITOR_KEY_POSE_RESET = "pose_reset";
    public static final String CHARACTER_EDITOR_KEY_POSE_TURN = "pose_turn";
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
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_ASSIGN = "weight_assign";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_ADD = "weight_add";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_SUBTRACT = "weight_subtract";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_REMOVE = "weight_remove";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_NORMALIZE = "weight_normalize";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_SMOOTH = "weight_smooth";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_POSITIVE = "weight_mirror_positive";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_NEGATIVE = "weight_mirror_negative";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_DIGITS = "weight_digits";
    public static final String CHARACTER_EDITOR_KEY_WEIGHT_LOCKS = "weight_locks";

    // Character Editor — mesh steps and ranges, in blocks unless named otherwise
    public static final float CHARACTER_EDITOR_MESH_RADIUS_DEFAULT = 0.1f;
    public static final float CHARACTER_EDITOR_MESH_RADIUS_MIN = 0.005f;
    public static final float CHARACTER_EDITOR_MESH_RADIUS_MAX = 2f;
    public static final float CHARACTER_EDITOR_MESH_RADIUS_STEP = 0.01f;
    public static final float CHARACTER_EDITOR_MESH_RADIUS_WHEEL_FACTOR = 1.1f;
    public static final float CHARACTER_EDITOR_MESH_SNAP_DEFAULT = 0.005f;
    public static final float CHARACTER_EDITOR_MESH_SNAP_MAX = 0.5f;
    public static final float CHARACTER_EDITOR_MESH_SNAP_STEP = 0.005f;
    public static final float CHARACTER_EDITOR_MESH_ROTATE_STEP = 15f;

    // Pose — a bone's turn in the edit pose about each of its axes, in degrees, stepped this far
    public static final float CHARACTER_EDITOR_POSE_TURN_STEP = 5f;
    public static final String[] CHARACTER_EDITOR_POSE_TURN_LABELS = { "Turn X", "Turn Y", "Turn Z" };
    public static final float CHARACTER_EDITOR_PART_SIZE_DEFAULT = 0.1f;
    public static final float CHARACTER_EDITOR_PART_SIZE_MIN = 0.005f;
    public static final float CHARACTER_EDITOR_PART_SIZE_MAX = 2f;
    public static final float CHARACTER_EDITOR_WEIGHT_STEP_FINE = 0.05f;

    // Character Mesh — geometry; positions key on the decimal grid in this many signed bits per axis
    public static final int CHARACTER_MESH_KEY_BITS = 21;
    public static final long CHARACTER_MESH_KEY_MASK = (1L << CHARACTER_MESH_KEY_BITS) - 1L;
    public static final float CHARACTER_MESH_CENTER_TOLERANCE = 0.001f;
    public static final float CHARACTER_MESH_MIRROR_REACH = 0.003f;
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
    public static final String CHARACTER_EDITOR_HEADER_POSE = "Pose";
    public static final String CHARACTER_EDITOR_HEADER_TRANSFORM = "Transform";
    public static final String CHARACTER_EDITOR_HEADER_GEOMETRY = "Geometry";
    public static final String CHARACTER_EDITOR_HEADER_WEIGHTS = "Weights";
    public static final String CHARACTER_EDITOR_HEADER_FACES = "Selected Faces";
    public static final String CHARACTER_EDITOR_HEADER_NEW_PART = "New Part";
    public static final String CHARACTER_EDITOR_HEADER_MESH_FILE = "Mesh File";
    public static final String CHARACTER_EDITOR_HEADER_ACTIVE_VERTEX = "Active Vertex";
    public static final String CHARACTER_EDITOR_LABEL_VERTICES = " vertices";
    public static final String CHARACTER_EDITOR_LABEL_FACES = " faces";
    public static final String CHARACTER_EDITOR_LABEL_SELECTED = " selected";
    public static final String CHARACTER_EDITOR_LABEL_PROPORTIONAL = "Proportional";
    public static final String CHARACTER_EDITOR_LABEL_RADIUS = "Radius";
    public static final String CHARACTER_EDITOR_LABEL_SNAP = "Snap (Ctrl)";
    public static final String CHARACTER_EDITOR_LABEL_CENTER = "Center";
    public static final String CHARACTER_EDITOR_LABEL_TURN = "Turn";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_ALL = "Select All  (A)";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_NONE = "Select None  (Alt+A)";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_INVERT = "Invert Selection  (Ctrl+I)";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_MORE = "Select More  (Ctrl+Numpad +)";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_LESS = "Select Less  (Ctrl+Numpad -)";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_LINKED = "Select Linked  (L)";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_MIRROR = "Select Mirror";
    public static final String CHARACTER_EDITOR_LABEL_SELECT_BONE = "Select Weighted to Bone";
    public static final String CHARACTER_EDITOR_LABEL_HIDE = "Hide  (H)";
    public static final String CHARACTER_EDITOR_LABEL_HIDE_UNSELECTED = "Hide Unselected  (Shift+H)";
    public static final String CHARACTER_EDITOR_LABEL_REVEAL = "Reveal  (Alt+H)";
    public static final String CHARACTER_EDITOR_LABEL_POSE_RESET = "Reset Pose  (Alt+R)";
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
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_ASSIGN = "Assign Weight";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_ADD = "Add 0.05";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_SUBTRACT = "Subtract 0.05";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_REMOVE = "Remove Bone";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_NORMALIZE = "Normalize";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_SMOOTH = "Smooth Selected";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_POSITIVE = "Mirror Weights  +X to -X";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_NEGATIVE = "Mirror Weights  -X to +X";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_DIGITS = "Weigh Onto Digits";
    public static final String CHARACTER_EDITOR_LABEL_WEIGHT_LOCKS = "Weigh Onto Locks";

    // Character Editor — mesh status text
    public static final String CHARACTER_EDITOR_STATUS_MESH_HINT = "Select brushes over vertices, a soft brush selecting"
            + " part way, Shift adds, Ctrl removes; Move, Rotate and Scale follow it; drag off the mesh to orbit";
    public static final String CHARACTER_EDITOR_STATUS_SCULPT_HINT = "Drag on the mesh to sculpt with the tool in use,"
            + " Ctrl inverts the stroke; drag off it to orbit, right-drag to pan";
    public static final String CHARACTER_EDITOR_STATUS_PAINT_HINT = "Drag on the mesh to paint the map chosen; Fill"
            + " fills the island clicked, Pick takes its colour; drag off it to orbit, right-drag to pan";
    public static final String CHARACTER_EDITOR_STATUS_TOPOLOGY_HINT = "Flip Edge turns the edge clicked (Ctrl the"
            + " other way), Loop Cut cuts a loop across it, Dissolve Loop takes its loop away; Topology brushes faces"
            + " finer, Ctrl coarser; drag off the mesh to orbit";
    public static final String CHARACTER_EDITOR_STATUS_HAIR_HINT = "Draw drags a strand from the scalp, Place grows"
            + " one where it clicks; Brush paints the material chosen, Nudge combs, Raise and Lower lengthen and"
            + " shorten, Erase cuts; drag off the head to orbit";
    public static final String CHARACTER_EDITOR_STATUS_UV_HINT = "Select the vertices of the edges to cut and Mark"
            + " Seam, then select the faces and Unwrap; their UVs show in the UV Editor; drag off the mesh to orbit";
    public static final String CHARACTER_EDITOR_STATUS_RIG_HINT = "Click a joint to pick its bone; Move drags it,"
            + " Rotate turns its bind, Scale sizes it, Place adds a child, Erase removes one; the mesh stays put (Pose"
            + " on the Mesh layer moves it); drag off the joints to orbit";
    public static final String CHARACTER_EDITOR_STATUS_POSE_HINT = "Pose turns the bone whose joint you drag, Pick"
            + " picks one; the pose holds in every modelling tab until Reset Pose; drag off the joints to orbit";
    public static final String CHARACTER_EDITOR_STATUS_TRANSFORM_HINT = "X Y Z hold to an axis, Ctrl snaps, click or "
            + "Enter confirms, right click or Esc cancels";
    public static final String CHARACTER_EDITOR_MESSAGE_MESH_EMPTY = "A mesh needs at least one face";
    public static final String CHARACTER_EDITOR_MESSAGE_HIDE_NONE = "Nothing to hide";
    public static final String CHARACTER_EDITOR_MESSAGE_HIDE_ALL = "Something must stay shown: Reveal shows it all";
    public static final String CHARACTER_EDITOR_MESSAGE_MESH_LIMIT = "That edit would pass the mesh vertex limit";
    public static final String CHARACTER_EDITOR_MESSAGE_MESH_NOT_SAVED = "A mesh file could not be written";
    public static final String CHARACTER_EDITOR_MESSAGE_MESH_NOT_REVERTED = "The mesh file could not be read";
    public static final String CHARACTER_EDITOR_MESSAGE_POSE_PLAYING =
            "Sculpt strokes need a still pose: pick Bind or a struck pose under Pose, and turn Preview Springs off"
                    + " (Idle, Walk, Run and the springs move)";
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

    // Character Editor — sculpt strokes, each the share of the brush's radius or of the way a full-strength stamp
    // moves a vertex
    public static final float CHARACTER_SCULPT_DRAW_RATE = 0.1f;
    public static final float CHARACTER_SCULPT_INFLATE_RATE = 0.1f;
    public static final float CHARACTER_SCULPT_SMOOTH_RATE = 0.5f;
    public static final float CHARACTER_SCULPT_PINCH_RATE = 0.2f;
    public static final float CHARACTER_SCULPT_CREASE_RATE = 0.06f;
    public static final float CHARACTER_SCULPT_CREASE_PINCH_RATE = 0.25f;
    public static final float CHARACTER_SCULPT_RELAX_RATE = 0.5f;
    public static final float CHARACTER_SCULPT_FLATTEN_RATE = 0.5f;

    // Character Editor — the topology brush. A face is refined when the brush covers all its corners and on average
    // at least the face coverage; refining lays it on a lattice of thirds and splits it into nine (Schneiders'
    // 3-refinement), and joins it to the faces about it as quads alone by the transition templates: one edge refined,
    // two edges meeting at a corner, two opposite edges. Each template lists its quads as four lattice points (x, y),
    // x from corner 0 toward corner 1 and y from corner 0 toward corner 3, every quad wound as its face. A reducing
    // stroke collapses each frame the share of the covered faces given, at most the most given, the smallest first
    // across their shorter diagonal, refusing a collapse that turns any face about it by more than the least cosine
    // or shrinks it below the least share of its area
    public static final float CHARACTER_TOPOLOGY_FACE_COVERAGE = 0.2f;
    public static final float CHARACTER_TOPOLOGY_DETAIL_UNIT = 0.001f;
    public static final float CHARACTER_TOPOLOGY_SPLIT_ABOVE = 1.5f;
    public static final float CHARACTER_TOPOLOGY_MERGE_BELOW = 0.75f;
    public static final int CHARACTER_TOPOLOGY_LATTICE = 3;
    public static final int[] CHARACTER_TOPOLOGY_TEMPLATE_FULL = {
            0, 0, 1, 0, 1, 1, 0, 1,
            1, 0, 2, 0, 2, 1, 1, 1,
            2, 0, 3, 0, 3, 1, 2, 1,
            0, 1, 1, 1, 1, 2, 0, 2,
            1, 1, 2, 1, 2, 2, 1, 2,
            2, 1, 3, 1, 3, 2, 2, 2,
            0, 2, 1, 2, 1, 3, 0, 3,
            1, 2, 2, 2, 2, 3, 1, 3,
            2, 2, 3, 2, 3, 3, 2, 3 };
    public static final int[] CHARACTER_TOPOLOGY_TEMPLATE_SIDE = {
            0, 0, 1, 0, 1, 1, 0, 3,
            1, 0, 2, 0, 2, 1, 1, 1,
            2, 0, 3, 0, 3, 3, 2, 1,
            1, 1, 2, 1, 3, 3, 0, 3 };
    public static final int[] CHARACTER_TOPOLOGY_TEMPLATE_CORNER = {
            0, 0, 1, 0, 1, 1, 0, 3,
            1, 0, 2, 0, 2, 1, 1, 1,
            2, 0, 3, 0, 3, 1, 2, 1,
            2, 1, 3, 1, 3, 2, 2, 2,
            1, 1, 2, 1, 2, 2, 0, 3,
            2, 2, 3, 2, 3, 3, 0, 3 };
    public static final int[] CHARACTER_TOPOLOGY_TEMPLATE_STRIP = {
            0, 0, 1, 0, 1, 3, 0, 3,
            1, 0, 2, 0, 2, 3, 1, 3,
            2, 0, 3, 0, 3, 3, 2, 3 };
    public static final int CHARACTER_TOPOLOGY_MASK_FULL = 0b1111;
    public static final int CHARACTER_TOPOLOGY_MASK_SIDE = 0b0001;
    public static final int CHARACTER_TOPOLOGY_MASK_CORNER = 0b0011;
    public static final int CHARACTER_TOPOLOGY_MASK_STRIP = 0b0101;
    public static final float CHARACTER_TOPOLOGY_REDUCE_SHARE = 0.25f;
    public static final int CHARACTER_TOPOLOGY_REDUCE_MAX = 16;
    public static final int CHARACTER_TOPOLOGY_DOUBLET_FACES = 2;
    public static final float CHARACTER_TOPOLOGY_FLIP_COSINE = 0.2f;
    public static final float CHARACTER_TOPOLOGY_AREA_SHARE_MIN = 0.1f;
    public static final String CHARACTER_EDITOR_MESSAGE_TOPOLOGY_SHAPE =
            "The topology changes only on the base shape: return to it under Shape Keys";

    // Character Topology — the Topology tab's edge tools and clean-up. A loop walk ends closed, on an open edge's
    // vertex, or at a pole it cannot pass; two faces about an edge pack into one long, the second in the low bits
    public static final String CHARACTER_TOPOLOGY_KEY_MERGE_DISTANCE = "merge_distance";
    public static final String CHARACTER_TOPOLOGY_KEY_FLIP_FACES = "flip_faces";
    public static final String CHARACTER_TOPOLOGY_KEY_TRIANGULATE = "triangulate";
    public static final String CHARACTER_TOPOLOGY_KEY_JOIN_TRIANGLES = "join_triangles";
    public static final String CHARACTER_TOPOLOGY_KEY_DELETE_DEGENERATE = "delete_degenerate";
    public static final String CHARACTER_TOPOLOGY_KEY_SELECT_POLES = "select_poles";
    public static final String CHARACTER_TOPOLOGY_KEY_SELECT_TRIANGLES = "select_triangles";
    public static final String CHARACTER_TOPOLOGY_KEY_SELECT_NON_MANIFOLD = "select_non_manifold";
    public static final String CHARACTER_TOPOLOGY_HEADER_FACES = "Faces";
    public static final String CHARACTER_TOPOLOGY_HEADER_CLEAN_UP = "Clean Up";
    public static final String CHARACTER_TOPOLOGY_HEADER_STATS = "Topology";
    public static final String CHARACTER_TOPOLOGY_LABEL_MERGE_DISTANCE = "Merge by Distance";
    public static final String CHARACTER_TOPOLOGY_LABEL_FLIP_FACES = "Flip Faces";
    public static final String CHARACTER_TOPOLOGY_LABEL_TRIANGULATE = "Triangulate Faces";
    public static final String CHARACTER_TOPOLOGY_LABEL_JOIN_TRIANGLES = "Triangles to Quads";
    public static final String CHARACTER_TOPOLOGY_LABEL_DELETE_DEGENERATE = "Delete Degenerate Faces";
    public static final String CHARACTER_TOPOLOGY_LABEL_SELECT_POLES = "Select Poles";
    public static final String CHARACTER_TOPOLOGY_LABEL_SELECT_TRIANGLES = "Select Triangles";
    public static final String CHARACTER_TOPOLOGY_LABEL_SELECT_NON_MANIFOLD = "Select Non-Manifold";
    public static final String CHARACTER_TOPOLOGY_LABEL_QUADS = " quads";
    public static final String CHARACTER_TOPOLOGY_LABEL_TRIANGLES = " triangles";
    public static final String CHARACTER_TOPOLOGY_LABEL_POLES = " poles";
    public static final float CHARACTER_TOPOLOGY_MERGE_DISTANCE = 0.0001f;
    public static final float CHARACTER_TOPOLOGY_AREA_EPSILON = 1.0e-12f;
    public static final int CHARACTER_TOPOLOGY_LOOP_CLOSED = 0;
    public static final int CHARACTER_TOPOLOGY_LOOP_OPEN = 1;
    public static final int CHARACTER_TOPOLOGY_LOOP_POLE = 2;
    public static final long CHARACTER_TOPOLOGY_FACE_MASK = 0xFFFFFFFFL;
    public static final String CHARACTER_TOPOLOGY_MESSAGE_OPEN_EDGE = "That edge needs exactly two faces beside it";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_TRIANGLE =
            "The stroke would split a triangle, which only quads take: join it into quads first";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_SEAM =
            "A UV seam keeps its faces in separate islands: Clear Seam on it first";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_WINDING =
            "The faces beside that edge wind opposite ways: Flip Faces on one of them first";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_JOINED = "Those corners are joined already";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_FLIPPED = "Edge flipped";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_RING_TRIANGLE = "Loop Cut runs through quads, not triangles";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_CUT = " faces cut";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_POLE =
            "That loop meets a pole or a triangle: Dissolve Loop takes a loop that closes or runs to an open edge";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_DISSOLVED = " edges dissolved";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_MERGED = " vertices merged by distance";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_MERGED_NONE = "No vertices stand within the merge distance";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_FACES_FLIPPED = " faces flipped";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_TRIANGULATED = " quads split into triangles";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_TRIANGULATE_NONE = "Select whole quads to split";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_JOINED_TRIANGLES = " triangle pairs joined into quads";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_JOIN_NONE =
            "Select pairs of triangles that share an edge, a texture and a zone";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_DEGENERATE = " degenerate faces deleted";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_DEGENERATE_NONE = "No degenerate faces";
    public static final String CHARACTER_TOPOLOGY_MESSAGE_SELECTED = " vertices selected";

    // Character Hair — the Hair tab's grooms: the workspace folder they are kept in and the folder under Exports their
    // cards are written to, the fields of a groom file, the strands' units (Length in centimetres, Width in
    // millimetres, positions in blocks), how far apart a drawn strand's points are, how far a root stands off the
    // surface, how far a full-strength stamp grows or shortens a tip and smooths or combs a point, how near twin roots
    // stand, the undo steps kept, and the defaults a strand starts with. A card is two quads wide, its corners laid
    // across it at the u given, and its v running from 1 at the root to 0 at the tip as the strand image hangs
    public static final String CHARACTER_HAIR_WORKSPACE_DIRECTORY = "Hair";
    public static final String CHARACTER_HAIR_EXPORT_FOLDER = "Hair";
    public static final String CHARACTER_HAIR_FIELD_RIG = "rig";
    public static final String CHARACTER_HAIR_FIELD_SOURCE = "source";
    public static final String CHARACTER_HAIR_FIELD_STRANDS = "strands";
    public static final String CHARACTER_HAIR_FIELD_TEXTURE = "texture";
    public static final String CHARACTER_HAIR_FIELD_BONE = "bone";
    public static final String CHARACTER_HAIR_FIELD_WIDTH = "width";
    public static final String CHARACTER_HAIR_FIELD_TAPER = "taper";
    public static final String CHARACTER_HAIR_FIELD_NORMAL = "normal";
    public static final String CHARACTER_HAIR_FIELD_POINTS = "points";
    public static final String CHARACTER_HAIR_FIELD_SLOT = "slot";
    public static final String CHARACTER_HAIR_FIELD_MESH = "mesh";
    public static final String CHARACTER_HAIR_SLOT = "hair";
    public static final String CHARACTER_HAIR_MESH_SUFFIX = "Mesh";
    public static final String CHARACTER_HAIR_RIG_FOLDER = "hair/";
    public static final String CHARACTER_HAIR_DEFAULT_FEATURE_FOLDER = "humanoid/hair/";
    public static final String CHARACTER_HAIR_DEFAULT_BONE = "head";
    public static final String CHARACTER_HAIR_DEFAULT_TEXTURE = "characters/standard/Hair";
    public static final float CHARACTER_HAIR_LENGTH_UNIT = 0.01f;
    public static final float CHARACTER_HAIR_WIDTH_UNIT = 0.001f;
    public static final float CHARACTER_HAIR_POINT_SPACING = 0.01f;
    public static final float CHARACTER_HAIR_ROOT_LIFT = 0.001f;
    public static final float CHARACTER_HAIR_GROW_RATE = 0.004f;
    public static final float CHARACTER_HAIR_SMOOTH_RATE = 0.5f;
    public static final float CHARACTER_HAIR_SEGMENT_MIN = 0.001f;
    public static final float CHARACTER_HAIR_TWIN_REACH = 0.005f;
    public static final float CHARACTER_HAIR_TAPER_DEFAULT = 0.35f;
    public static final float CHARACTER_HAIR_TAPER_STEP = 0.05f;
    public static final float CHARACTER_HAIR_WIDTH_STEP = 0.5f;
    public static final float CHARACTER_HAIR_EPSILON = 1.0e-6f;
    public static final int CHARACTER_HAIR_MIN_POINTS = 2;
    public static final int CHARACTER_HAIR_HISTORY_MAX = 64;
    public static final float[] CHARACTER_HAIR_CARD_U = { 0f, 0.5f, 1f };
    public static final String CHARACTER_HAIR_KEY_PREFIX = "hair_";
    public static final String CHARACTER_HAIR_KEY_FROM_WORN = "hair_from_worn";
    public static final String CHARACTER_HAIR_KEY_SELECT_ALL = "hair_select_all";
    public static final String CHARACTER_HAIR_KEY_SELECT_NONE = "hair_select_none";
    public static final String CHARACTER_HAIR_KEY_DELETE = "hair_delete";
    public static final String CHARACTER_HAIR_KEY_MATERIAL = "hair_material";
    public static final String CHARACTER_HAIR_KEY_WIDTH = "hair_width";
    public static final String CHARACTER_HAIR_KEY_TAPER = "hair_taper";
    public static final String CHARACTER_HAIR_HEADER_STRANDS = "Strands";
    public static final String CHARACTER_HAIR_HEADER_NO_GROOM =
            "No hair open: File, New makes one, File, Open opens one, Strands From Worn Hair takes the hair worn";
    public static final String CHARACTER_HAIR_HEADER_SELECTED = "Selected Strands";
    public static final String CHARACTER_HAIR_LABEL_FROM_WORN = "Strands From Worn Hair";
    public static final String CHARACTER_HAIR_LABEL_SELECT_ALL = "Select All Strands";
    public static final String CHARACTER_HAIR_LABEL_SELECT_NONE = "Select No Strands";
    public static final String CHARACTER_HAIR_LABEL_DELETE = "Delete Selected Strands";
    public static final String CHARACTER_HAIR_LABEL_MATERIAL = "Material";
    public static final String CHARACTER_HAIR_LABEL_WIDTH = "Width (mm)";
    public static final String CHARACTER_HAIR_LABEL_TAPER = "Taper";
    public static final String CHARACTER_HAIR_LABEL_STRANDS = " strands";
    public static final String CHARACTER_HAIR_LABEL_SELECTED = " selected";
    public static final String CHARACTER_HAIR_DIALOG_NEW = "New hair: its name";
    public static final String CHARACTER_HAIR_DIALOG_RIG = "New hair: the rig it is worn on";
    public static final String CHARACTER_HAIR_DIALOG_OPEN = "Open Hair";
    public static final String CHARACTER_HAIR_DIALOG_IMPORT = "Import Hair";
    public static final String CHARACTER_HAIR_DIALOG_FEATURE =
            "Import: the hair feature it becomes, as folder/Name (an existing one is replaced)";
    public static final String CHARACTER_HAIR_DIALOG_SPRINGS =
            "Import: lay spring chains on the long strands and weigh them on (yes or no)";
    public static final String CHARACTER_HAIR_ANSWER_YES = "yes";
    public static final String CHARACTER_HAIR_ANSWER_NO = "no";
    public static final String CHARACTER_HAIR_MESSAGE_MADE = "Made the hair ";
    public static final String CHARACTER_HAIR_MESSAGE_OPENED = "Opened the hair ";
    public static final String CHARACTER_HAIR_MESSAGE_SAVED = "Saved the hair ";
    public static final String CHARACTER_HAIR_MESSAGE_REVERTED = "Reverted the hair ";
    public static final String CHARACTER_HAIR_MESSAGE_NOT_WRITTEN = "The hair file could not be written: ";
    public static final String CHARACTER_HAIR_MESSAGE_NOT_READ = "The hair file could not be read: ";
    public static final String CHARACTER_HAIR_MESSAGE_OUTSIDE = "Open takes a hair from the workspace's Hair folder";
    public static final String CHARACTER_HAIR_MESSAGE_NO_GROOM = "No hair is open";
    public static final String CHARACTER_HAIR_MESSAGE_NO_STRANDS = "The hair has no strands to export";
    public static final String CHARACTER_HAIR_MESSAGE_EXPORTED = "Exported the hair's cards to ";
    public static final String CHARACTER_HAIR_MESSAGE_IMPORTED = "Imported the hair as the feature ";
    public static final String CHARACTER_HAIR_MESSAGE_NOT_EXPORT = "Import takes a hair exported to Exports/Hair";
    public static final String CHARACTER_HAIR_MESSAGE_NO_WORN = "The character wears no hair to take strands from";
    public static final String CHARACTER_HAIR_MESSAGE_FROM_WORN = " strands taken from the hair worn";
    public static final String CHARACTER_HAIR_MESSAGE_NO_MATERIAL = "Pick a material in the Hair Materials tab first";
    public static final String CHARACTER_HAIR_MESSAGE_PAINTED = " strands painted";
    public static final String CHARACTER_HAIR_MESSAGE_DELETED = " strands deleted";
    public static final String CHARACTER_HAIR_MESSAGE_NO_SELECTION = "Select strands first";
    public static final String CHARACTER_HAIR_MESSAGE_SPRINGS_NO_RIG =
            "Open a character on the hair's rig in the character tabs to lay its spring chains";

    // Character Symmetry — the centre plane is x = 0; corners within CHARACTER_MESH_CENTER_TOLERANCE lie on it
    public static final int CHARACTER_SYMMETRY_SIDE_KEPT = 1;
    public static final int CHARACTER_SYMMETRY_SIDE_CENTRE = 0;
    public static final int CHARACTER_SYMMETRY_SIDE_REMOVED = -1;
    public static final float CHARACTER_SYMMETRY_HALF = 0.5f;
    public static final float CHARACTER_SYMMETRY_BONE_REACH = 0.02f;
    public static final float CHARACTER_SYMMETRY_AXIS_SPREAD_MIN = 0.0001f;
    public static final float CHARACTER_SYMMETRY_AXIS_WELD = 0.01f;
    public static final String CHARACTER_EDITOR_MESSAGE_SYMMETRIZED = "Symmetrized ";
    public static final String CHARACTER_EDITOR_MESSAGE_SYMMETRY_SEPARATOR = ": ";
    public static final String CHARACTER_EDITOR_MESSAGE_SYMMETRY_FACES = " faces, ";
    public static final String CHARACTER_EDITOR_MESSAGE_SYMMETRY_CUT = " edges cut at the centre, ";
    public static final String CHARACTER_EDITOR_MESSAGE_SYMMETRY_LAID = " vertices laid on it";
    public static final String CHARACTER_EDITOR_MESSAGE_SYMMETRY_OVERLAP_START = ", ";
    public static final String CHARACTER_EDITOR_MESSAGE_SYMMETRY_OVERLAP =
            " UV islands mirrored in place, with no twin island on the side removed to take";
    public static final String CHARACTER_EDITOR_MESSAGE_SYMMETRY_UNMATCHED =
            " bones off the centre have no twin, so the mirrored side weighs onto them too:"
                    + " Rig Hair Locks then Weigh Onto Locks give it chains of its own";

    // Character Rig — rig fields
    public static final String CHARACTER_RIG_FIELD_BONES = "bones";
    public static final String CHARACTER_RIG_FIELD_NAME = "name";
    public static final String CHARACTER_RIG_FIELD_PARENT = "parent";
    public static final String CHARACTER_RIG_FIELD_IK = "ik";
    public static final String CHARACTER_RIG_FIELD_POLE = "pole";
    public static final String CHARACTER_RIG_FIELD_SPRINGS = "springs";
    public static final String CHARACTER_RIG_FIELD_COLLIDERS = "colliders";
    public static final String CHARACTER_RIG_FIELD_BONE = "bone";
    public static final String CHARACTER_RIG_FIELD_SHAPE = "shape";
    public static final String CHARACTER_RIG_FIELD_RADIUS = "radius";
    public static final String CHARACTER_RIG_FIELD_OFFSET = "offset";
    public static final String CHARACTER_RIG_FIELD_START = "start";
    public static final String CHARACTER_RIG_FIELD_END = "end";
    public static final String CHARACTER_RIG_FIELD_LOOP = "loop";
    public static final String CHARACTER_RIG_FIELD_TRACKS = "tracks";
    public static final String CHARACTER_RIG_FIELD_CLIP = "clip";
    public static final String CHARACTER_RIG_FIELD_NODES = "nodes";
    public static final String CHARACTER_RIG_FIELD_DEFAULT = "default";
    public static final String CHARACTER_RIG_FIELD_LAYERS = "layers";

    // Rig of a Model's Own — the name New Rig gives it after the model's, the clip and tree that hold it at rest to
    // preview it by, and the one shared rig no model's bones may change
    public static final String CHARACTER_RIG_NEW_SUFFIX = "Rig";
    public static final String CHARACTER_RIG_REST_CLIP_SUFFIX = "Rest";
    public static final String CHARACTER_RIG_REST_TREE_SUFFIX = "AnimationTree";
    public static final float[] CHARACTER_RIG_REST_KEY_TIMES = { 0f, 1f };
    public static final String CHARACTER_RIG_REST_LAYER = "base";
    public static final String CHARACTER_RIG_REST_NODE = "idle";

    // Character Paint — the maps a part's texture carries, in the order the Map row steps through them, each with its
    // label, the alias whose layer it fills and the suffix its image takes beside the texture; a canvas holds at most
    // the layer count, its first the image itself; an image open for painting is named by the prefix, its texture,
    // map and layer number; a texel within the bleed of a face's UV edge is painted with it, so islands never show a
    // seam, and a face whose UV area falls under the epsilon paints nothing; values step by these steps and the
    // colour starts white
    public static final String[] CHARACTER_PAINT_MAP_LABELS = {
            "Albedo", "Normal", "Height", "AO", "Metallic", "Roughness", "Tint" };
    public static final String[] CHARACTER_PAINT_MAP_ALIASES = {
            "albedo", "normal", "height", "ao", "metallic", "roughness", "tint" };
    public static final String[] CHARACTER_PAINT_MAP_SUFFIXES = { "A", "n", "h", "ao", "m", "rough", "tint" };
    public static final ImageMap[] CHARACTER_PAINT_IMAGE_MAPS = {
            ImageMap.ALBEDO, ImageMap.NORMAL, ImageMap.HEIGHT, ImageMap.AMBIENT_OCCLUSION, ImageMap.METALLIC,
            ImageMap.ROUGHNESS, ImageMap.TINT };
    public static final String CHARACTER_PAINT_SUFFIX_SEPARATOR = "_";
    public static final String CHARACTER_PAINT_EXTENSION_SEPARATOR = ".";
    public static final int CHARACTER_PAINT_MAX_LAYERS = 8;
    public static final long CHARACTER_PAINT_PREVIEW_NANOS = 250_000_000L;
    public static final String CHARACTER_PAINT_IMAGE_PREFIX = "paint/";
    public static final String CHARACTER_PAINT_IMAGE_SEPARATOR = "/";
    public static final float CHARACTER_PAINT_BLEED_PIXELS = 1.5f;
    public static final float CHARACTER_PAINT_AREA_EPSILON = 0.000001f;
    public static final float CHARACTER_PAINT_COLOR_STEP = 0.02f;
    public static final float CHARACTER_PAINT_OPACITY_STEP = 0.05f;
    public static final float[] CHARACTER_PAINT_COLOR_DEFAULT = { 1f, 1f, 1f };
    public static final String[] CHARACTER_PAINT_COLOR_LABELS = { "Red", "Green", "Blue" };
    public static final String[] CHARACTER_PAINT_HSB_LABELS = { "Hue", "Saturation", "Brightness" };
    public static final float CHARACTER_PAINT_HSB_STEP = 0.01f;
    public static final float CHARACTER_PAINT_CHANNEL_MIN = 0f;
    public static final float CHARACTER_PAINT_CHANNEL_MAX = 1f;

    // Character Paint — row keys, labels and messages
    public static final String CHARACTER_PAINT_KEY_OPACITY = "paint_opacity";
    public static final String CHARACTER_PAINT_KEY_COLOR = "paint_color";
    public static final String CHARACTER_PAINT_KEY_HSB = "paint_hsb";
    public static final String CHARACTER_PAINT_KEY_MAP_ITEM = "paint_map_item";
    public static final String CHARACTER_PAINT_KEY_MAP_SETTING = "paint_map_setting";
    public static final String CHARACTER_PAINT_KEY_MAP_GENERATE = "paint_map_generate";
    public static final String CHARACTER_PAINT_KEY_MAP_CLEAR = "paint_map_clear";
    public static final String CHARACTER_PAINT_KEY_LAYER_ADD = "paint_layer_add";
    public static final String CHARACTER_PAINT_KEY_LAYER_REMOVE = "paint_layer_remove";
    public static final String CHARACTER_PAINT_HEADER_PAINT = "Paint ";
    public static final String CHARACTER_PAINT_HEADER_COLOR = "Colour";
    public static final String CHARACTER_PAINT_HEADER_HSB = "Hue, Saturation, Brightness";
    public static final String CHARACTER_PAINT_HEADER_RGB = "Red, Green, Blue";
    public static final String CHARACTER_PAINT_HEADER_MAPS = "Maps of ";
    public static final String CHARACTER_PAINT_HEADER_LAYERS = "Layers";
    public static final String CHARACTER_PAINT_HEADER_NO_TEXTURE = "The part worn here has no texture to paint";
    public static final String CHARACTER_PAINT_LABEL_OPACITY = "Opacity";
    public static final String CHARACTER_PAINT_LABEL_LAYER_ADD = "Add Layer";
    public static final String CHARACTER_PAINT_LABEL_LAYER_REMOVE = "Remove Layer";
    public static final String CHARACTER_PAINT_LAYER_BASE = "Base";
    public static final String CHARACTER_PAINT_LAYER_NAME = "Layer ";
    public static final String CHARACTER_PAINT_MESSAGE_LAYER_ADDED = "Layer added: ";
    public static final String CHARACTER_PAINT_MESSAGE_LAYER_REMOVED = "Layer removed: ";
    public static final String CHARACTER_PAINT_MESSAGE_LAYER_BASE = "The base layer is the image itself and stays";
    public static final String CHARACTER_PAINT_MESSAGE_LAYER_FULL = "A map holds at most "
            + CHARACTER_PAINT_MAX_LAYERS + " layers";
    public static final String CHARACTER_PAINT_MESSAGE_NOT_SAVED = "A painted map could not be written";

    // Character Rig — new bones and sizes, in blocks; a new bone is named this and a number, then its parent's side
    public static final float CHARACTER_RIG_SIZE_MIN = 0.005f;
    public static final float CHARACTER_RIG_NEW_BONE_OFFSET = 0.1f;
    public static final float CHARACTER_RIG_NEW_BONE_SIZE = 0.08f;
    public static final String CHARACTER_RIG_NEW_BONE_NAME = "bone_";

    // Character Rig — digits: a branch reaching at least this far past where it meets another is a digit, its chain
    // begun this far past its web, its joints at these shares of its length; five are named in this order, any other
    // count by number; a region with no digits apart takes one toe at this share of its length, centred in the slab
    // this deep about it; a digit's bones are this many times as thick as its vertices lie from its line on average;
    // its line is found in this many steps
    public static final float CHARACTER_RIG_DIGIT_MIN_LENGTH = 0.02f;
    public static final float CHARACTER_RIG_DIGIT_ROOT_REACH = 0.025f;
    public static final float[] CHARACTER_RIG_DIGIT_SHARES = { 0f, 0.49f, 0.78f };
    public static final String[] CHARACTER_RIG_DIGIT_NAMES = { "thumb", "index", "middle", "ring", "pinky" };
    public static final String CHARACTER_RIG_DIGIT_GENERIC = "digit";
    public static final String CHARACTER_RIG_DIGIT_SEPARATOR = "_";
    public static final String CHARACTER_RIG_TOE_NAME = "toe";
    public static final float CHARACTER_RIG_BALL_SHARE = 0.72f;
    public static final float CHARACTER_RIG_BALL_SLAB = 0.012f;
    public static final float CHARACTER_RIG_DIGIT_THICKNESS_SHARE = 2f;
    public static final int CHARACTER_RIG_AXIS_ITERATIONS = 32;

    // Character Rig — IK handles: a handle is named after its end bone with this suffix; its middle joint bends the way
    // it stands off the line from root to end when it stands at least this far off, otherwise forward across the
    // chain, mirrored on the right; its pole is kept to the places this scale holds
    public static final String CHARACTER_RIG_IK_SUFFIX = "_ik";
    public static final float CHARACTER_RIG_IK_POLE_MIN = 0.002f;
    public static final float CHARACTER_RIG_IK_POLE_SCALE = 1000f;

    // Character Rig — spring chains: the settings a chain is simulated by, in the order their rows list them, each
    // with its label, the step its row moves by, its largest value and what a new chain starts with; a new chain
    // starts near critically damped (damping close to twice the square root of its stiffness) with some drag, so
    // hair has weight and settles after a move rather than swinging on
    public static final String[] CHARACTER_RIG_SPRING_FIELDS = { "stiffness", "damping", "gravity", "drag", "radius" };
    public static final String[] CHARACTER_RIG_SPRING_LABELS = { "Stiffness", "Damping", "Gravity", "Drag", "Radius" };
    public static final float[] CHARACTER_RIG_SPRING_STEPS = { 5f, 0.5f, 0.5f, 0.1f, 0.002f };
    public static final float[] CHARACTER_RIG_SPRING_MAXIMA = {
            EngineSetting.RIG_SPRING_STIFFNESS_MAX, EngineSetting.RIG_SPRING_DAMPING_MAX,
            EngineSetting.RIG_SPRING_GRAVITY_MAX, EngineSetting.RIG_SPRING_DRAG_MAX,
            EngineSetting.RIG_SPRING_RADIUS_MAX };
    public static final float[] CHARACTER_RIG_SPRING_DEFAULTS = {
            120f, 20f, 9.8f, 3f, EngineSetting.RIG_SPRING_RADIUS_DEFAULT };
    public static final String CHARACTER_RIG_SPRING_SUFFIX = "_spring";

    // Character Rig — colliders: a new one is named after its bone's stem with this suffix and its side, numbered
    // from two when taken, a sphere at the centre share of the way to the bone's first child, its radius the width
    // share of the bone's thinner side and never under the minimum; its rows list the radius, then each end's three
    // axes, each row's key index the collider times the stride plus the field, the radius stepped by the radius
    // step and the ends by the point step; the shapes it steps through, labelled
    public static final String CHARACTER_RIG_COLLIDER_SUFFIX = "_collider";
    public static final float CHARACTER_RIG_COLLIDER_RADIUS_STEP = 0.002f;
    public static final float CHARACTER_RIG_COLLIDER_POINT_STEP = 0.002f;
    public static final float CHARACTER_RIG_COLLIDER_RADIUS_MIN = 0.002f;
    public static final float CHARACTER_RIG_COLLIDER_CENTRE_SHARE = 0.5f;
    public static final float CHARACTER_RIG_COLLIDER_WIDTH_SHARE = 0.5f;
    public static final int CHARACTER_RIG_COLLIDER_FIELD_RADIUS = 0;
    public static final int CHARACTER_RIG_COLLIDER_FIELD_START = 1;
    public static final int CHARACTER_RIG_COLLIDER_FIELD_END = 4;
    public static final int CHARACTER_RIG_COLLIDER_STRIDE = 7;
    public static final String[] CHARACTER_RIG_COLLIDER_SHAPE_LABELS = { "Sphere", "Capsule" };

    // Character Rig — hair locks: a card is a run of hair faces whose v spans at least this share root to tip, its
    // centre line read row by row where v differs by more than the row gap; a card this long or longer is a lock
    // for spring bones (shorter hair is left to the shader), cards rooted within the lock radius of one another and
    // hanging alike join one lock, matched at this many points along their length; a lock's chain takes one bone per
    // segment length, between the fewest and the most segments, plus its tip, and is this many times as thick as its
    // cards lie from its line; a lock rooted within the lock radius of a chain already rigged takes that chain; its
    // bones are named the prefix, the lock's number and the joint's
    public static final float CHARACTER_HAIR_CARD_MIN_SPAN = 0.5f;
    public static final float CHARACTER_HAIR_ROW_GAP = 0.001f;
    public static final float CHARACTER_HAIR_LOCK_MIN_LENGTH = 0.15f;
    public static final float CHARACTER_HAIR_LOCK_RADIUS = 0.045f;
    public static final int CHARACTER_HAIR_LOCK_SAMPLES = 9;
    public static final float CHARACTER_HAIR_LOCK_SEGMENT = 0.1f;
    public static final int CHARACTER_HAIR_LOCK_MIN_SEGMENTS = 2;
    public static final int CHARACTER_HAIR_LOCK_MAX_SEGMENTS = 4;
    public static final float CHARACTER_HAIR_LOCK_THICKNESS_SHARE = 2f;
    public static final String CHARACTER_HAIR_LOCK_PREFIX = "hair_";

    // Character Rig — hair weights: a card weighs onto the chain whose joints lie nearest its own line, of those
    // rooted within the reach of its root; along it each vertex is handed from the bone the hair grows on to the
    // chain over this distance from the root, and from one bone of the chain to the next over this distance either
    // side of each joint, in blocks
    public static final float CHARACTER_HAIR_WEIGHT_REACH = 0.09f;
    public static final float CHARACTER_HAIR_WEIGHT_ROOT = 0.03f;
    public static final float CHARACTER_HAIR_WEIGHT_BLEND = 0.03f;

    // Character Rig — digit weights: a chain's joints blend over this share of its length to either side, a lone
    // toe's over this share; the palm follows a digit's root bone up to this share, fading out this far from its line
    public static final float CHARACTER_RIG_WEIGHT_BLEND = 0.05f;
    public static final float CHARACTER_RIG_WEIGHT_BALL_BLEND = 0.25f;
    public static final float CHARACTER_RIG_WEIGHT_PALM_SHARE = 0.6f;
    public static final float CHARACTER_RIG_WEIGHT_PALM_REACH = 0.022f;

    // Character Rig — panel keys
    public static final String CHARACTER_RIG_KEY_BONE = "rig_bone";
    public static final String CHARACTER_RIG_KEY_NAME = "rig_name";
    public static final String CHARACTER_RIG_KEY_PARENT = "rig_parent";
    public static final String CHARACTER_RIG_KEY_POSITION = "rig_position";
    public static final String CHARACTER_RIG_KEY_ROTATION = "rig_rotation";
    public static final String CHARACTER_RIG_KEY_SIZE = "rig_size";
    public static final String CHARACTER_RIG_KEY_ADD_BONE = "rig_add_bone";
    public static final String CHARACTER_RIG_KEY_REMOVE_BONE = "rig_remove_bone";
    public static final String CHARACTER_RIG_KEY_MIRROR_BONE = "rig_mirror_bone";
    public static final String CHARACTER_RIG_KEY_SYMMETRIZE_LEFT = "rig_symmetrize_left";
    public static final String CHARACTER_RIG_KEY_SYMMETRIZE_RIGHT = "rig_symmetrize_right";
    public static final String CHARACTER_RIG_KEY_UNDO = "rig_undo";
    public static final String CHARACTER_RIG_KEY_REDO = "rig_redo";
    public static final String CHARACTER_RIG_KEY_SAVE = "rig_save";
    public static final String CHARACTER_RIG_KEY_REVERT = "rig_revert";
    public static final String CHARACTER_RIG_KEY_DIGITS = "rig_digits";
    public static final String CHARACTER_RIG_KEY_RIG_NEW = "rig_new";
    public static final String CHARACTER_RIG_KEY_RIG_OWN = "rig_own";
    public static final String CHARACTER_RIG_KEY_IK_ADD = "rig_ik_add";
    public static final String CHARACTER_RIG_KEY_IK_REMOVE = "rig_ik_remove";
    public static final String CHARACTER_RIG_KEY_PART_ADD = "rig_part_add";
    public static final String CHARACTER_RIG_KEY_PART_REMOVE = "rig_part_remove";
    public static final String CHARACTER_RIG_KEY_HAIR = "rig_hair";
    public static final String CHARACTER_RIG_KEY_SPRING = "rig_spring";
    public static final String CHARACTER_RIG_KEY_SPRING_ADD = "rig_spring_add";
    public static final String CHARACTER_RIG_KEY_SPRING_REMOVE = "rig_spring_remove";
    public static final String CHARACTER_RIG_KEY_SPRING_PREVIEW = "rig_spring_preview";
    public static final String CHARACTER_RIG_KEY_SPRING_COLLIDER = "rig_spring_collider";
    public static final String CHARACTER_RIG_KEY_COLLIDER = "rig_collider";
    public static final String CHARACTER_RIG_KEY_COLLIDER_VALUE = "rig_collider_value";
    public static final String CHARACTER_RIG_KEY_COLLIDER_SHAPE = "rig_collider_shape";
    public static final String CHARACTER_RIG_KEY_COLLIDER_CHAINS = "rig_collider_chains";
    public static final String CHARACTER_RIG_KEY_COLLIDER_ADD = "rig_collider_add";
    public static final String CHARACTER_RIG_KEY_COLLIDER_REMOVE = "rig_collider_remove";

    // Character Rig — panel text
    public static final String CHARACTER_RIG_INDENT = "   ";
    public static final String CHARACTER_RIG_ADDED_MARKER = "  +";
    public static final String CHARACTER_RIG_HEADER_NO_RIG = "This character has no rig to edit";
    public static final String CHARACTER_RIG_HEADER_STRUCTURE = "Edit Bones";
    public static final String CHARACTER_RIG_HEADER_FILE = "Rig File";
    public static final String CHARACTER_RIG_LABEL_NAME = "Name";
    public static final String CHARACTER_RIG_LABEL_PARENT = "Parent";
    public static final String CHARACTER_RIG_LABEL_ADD_BONE = "Add Child Bone";
    public static final String CHARACTER_RIG_LABEL_REMOVE_BONE = "Remove Bone";
    public static final String CHARACTER_RIG_LABEL_MIRROR_BONE = "Mirror to Other Side";
    public static final String CHARACTER_RIG_LABEL_SYMMETRIZE_LEFT = "Symmetrize  Left to Right";
    public static final String CHARACTER_RIG_LABEL_SYMMETRIZE_RIGHT = "Symmetrize  Right to Left";
    public static final String CHARACTER_RIG_LABEL_SAVE = "Save Rig";
    public static final String CHARACTER_RIG_LABEL_DIGITS = "Rig Digits";
    public static final String CHARACTER_RIG_LABEL_RIG_NEW = "New Rig For The Model";
    public static final String CHARACTER_RIG_LABEL_RIG_OWN = "Rig Of Its Own";
    public static final String CHARACTER_RIG_LABEL_IK_ADD = "Add IK Handle";
    public static final String CHARACTER_RIG_LABEL_IK_REMOVE = "Remove IK Handle";
    public static final String CHARACTER_RIG_LABEL_PART_ADD = "Add Layer From Bone";
    public static final String CHARACTER_RIG_LABEL_PART_REMOVE = "Remove Layer";
    public static final String CHARACTER_RIG_LABEL_HAIR = "Rig Hair Locks";
    public static final String CHARACTER_RIG_LABEL_SPRING_ADD = "Add Spring Chain";
    public static final String CHARACTER_RIG_LABEL_SPRING_REMOVE = "Remove Spring Chain";
    public static final String CHARACTER_RIG_LABEL_SPRING_PREVIEW = "Preview Springs";
    public static final String CHARACTER_RIG_LABEL_COLLIDER_ADD = "Add Collider";
    public static final String CHARACTER_RIG_LABEL_COLLIDER_REMOVE = "Remove Collider";
    public static final String CHARACTER_RIG_LABEL_COLLIDER = "Collider ";
    public static final String CHARACTER_RIG_LABEL_SHAPE = "Shape";
    public static final String CHARACTER_RIG_LABEL_RADIUS = "Radius";
    public static final String CHARACTER_RIG_LABEL_OFFSET = "Offset";
    public static final String CHARACTER_RIG_LABEL_START = "Start";
    public static final String CHARACTER_RIG_LABEL_END = "End";
    public static final String CHARACTER_RIG_LABEL_CHAINS = "Spring Chains";
    public static final String CHARACTER_RIG_LABEL_CHAINS_ALL = "All";
    public static final String CHARACTER_RIG_LABEL_CHAINS_SOME = " of ";
    public static final String CHARACTER_RIG_HEADER_COLLIDERS = "Colliders";
    public static final String CHARACTER_RIG_HEADER_SPRING_COLLIDERS = "Rests Against";
    public static final String CHARACTER_RIG_HEADER_HAIR = "Hair";
    public static final String CHARACTER_RIG_HEADER_SPRING = "Spring Chain ";
    public static final String CHARACTER_RIG_HEADER_ACTIONS = "Rig";
    public static final String CHARACTER_RIG_HEADER_LAYERS = "Layers";
    public static final String CHARACTER_RIG_LABEL_REVERT = "Revert Rig";

    // Character Rig — status text
    public static final String CHARACTER_RIG_MESSAGE_ADDED = "Added ";
    public static final String CHARACTER_RIG_MESSAGE_SHARED =
            "Every item shares this rig: give the model a rig of its own with New Rig For The Model first";
    public static final String CHARACTER_RIG_MESSAGE_NEW = "The model now stands on a rig of its own: ";
    public static final String CHARACTER_RIG_MESSAGE_LEADER_BONE = "This bone is the character's: edit it on ";
    public static final String CHARACTER_RIG_MESSAGE_NEW_MODEL_ONLY =
            "New Rig gives a workspace model with a body a rig of its own";
    public static final String CHARACTER_RIG_MESSAGE_DIGITS =
            " digit bones added; weigh onto them with Weigh Onto Digits";
    public static final String CHARACTER_RIG_MESSAGE_NO_DIGITS = "No new digits found under this region's bones";
    public static final String CHARACTER_RIG_MESSAGE_NO_REGION = "Rig and weigh digits on the Hands or Feet layers";
    public static final String CHARACTER_RIG_MESSAGE_WEIGHED = " vertices weighed onto the digits";
    public static final String CHARACTER_RIG_MESSAGE_UNRIGGED = "Rig the digits first";
    public static final String CHARACTER_RIG_MESSAGE_IK_ADDED = " IK handles added";
    public static final String CHARACTER_RIG_MESSAGE_IK_REMOVED = " IK handles removed";
    public static final String CHARACTER_RIG_MESSAGE_HAIR =
            " hair locks rigged on spring chains; weigh onto them with Weigh Onto Locks";
    public static final String CHARACTER_RIG_MESSAGE_NO_HAIR = "No new hair locks long enough for spring bones";
    public static final String CHARACTER_RIG_MESSAGE_HAIR_REGION = "Rig and weigh hair locks on the Hair layers";
    public static final String CHARACTER_RIG_MESSAGE_HAIR_OWN_RIG =
            "The hair stands on the character's own rig: give it Rig Of Its Own first, then start the editor again";
    public static final String CHARACTER_RIG_MESSAGE_OWN_RIG_PART =
            "Pick the layer of a hair style or a worn model on the character's own rig to give it a rig of its own";
    public static final String CHARACTER_RIG_MESSAGE_OWN_RIG_ALREADY = "The model stands on a rig of its own already: ";
    public static final String CHARACTER_RIG_MESSAGE_OWN_RIG =
            "Saved on a rig of its own, taken up once the editor starts again: ";
    public static final String CHARACTER_RIG_MESSAGE_HAIR_WEIGHED = " hair vertices weighed onto the locks";
    public static final String CHARACTER_RIG_MESSAGE_HAIR_UNRIGGED = "Rig the hair locks first";
    public static final String CHARACTER_RIG_MESSAGE_SPRING_ADDED = "Spring chain added: ";
    public static final String CHARACTER_RIG_MESSAGE_SPRING_REMOVED = "Spring chain removed: ";
    public static final String CHARACTER_RIG_MESSAGE_SPRING_NONE =
            "Pick a bone with a chain of single children below it, in no spring chain or IK handle yet";
    public static final String CHARACTER_RIG_MESSAGE_SPRING_FREE = "The selected bone is in no spring chain";
    public static final String CHARACTER_RIG_MESSAGE_COLLIDER_ADDED = "Collider added: ";
    public static final String CHARACTER_RIG_MESSAGE_COLLIDER_REMOVED = "Collider removed: ";
    public static final String CHARACTER_RIG_MESSAGE_COLLIDER_NONE = "The selected bone carries no collider";
    public static final String CHARACTER_RIG_MESSAGE_COLLIDER_FULL = "A rig carries at most "
            + EngineSetting.RIG_COLLIDER_MAX + " colliders";
    public static final String CHARACTER_RIG_MESSAGE_PREVIEW_ON = "Springs preview live in the pose chosen";
    public static final String CHARACTER_RIG_MESSAGE_PREVIEW_OFF = "Spring preview off, the bind pose held";
    public static final String CHARACTER_RIG_MESSAGE_IK_NONE =
            "Pick the bone a handle drags, two bones below a limb's root, that has none yet";
    public static final String CHARACTER_RIG_MESSAGE_REMOVED = "Removed ";
    public static final String CHARACTER_RIG_MESSAGE_PART_ADDED = "Added the layer ";
    public static final String CHARACTER_RIG_MESSAGE_PART_REMOVED = "Removed the layer ";
    public static final String CHARACTER_RIG_MESSAGE_PART_HELD = "A layer already holds the selected bone: ";
    public static final String CHARACTER_RIG_MESSAGE_PART_NAMED = "A layer of that name already exists: ";
    public static final String CHARACTER_RIG_MESSAGE_PART_NONE = "No layer holds the selected bone";
    public static final String CHARACTER_RIG_FIELD_PARTS = "parts";
    public static final String CHARACTER_RIG_FIELD_STAMPS = "stamps";
    public static final String CHARACTER_RIG_MESSAGE_MIRRORED = "Mirrored ";
    public static final String CHARACTER_RIG_MESSAGE_LOADED_BONE = "Only bones added since the editor started can be"
            + " renamed or removed";
    public static final String CHARACTER_RIG_MESSAGE_NO_SIDE = "Every bone to mirror must end in _l or _r";
    public static final String CHARACTER_RIG_MESSAGE_REFUSED = "Rig not applied: ";
    public static final String CHARACTER_RIG_MESSAGE_RESTART =
            "Changes to the bones loaded at the start take hold when the editor next starts";

    // Character Regions — the bones each part layer of the Character Editor works on, with every bone below them,
    // and the bone it frames with every bone below it; a vertex weighing on a region's bones at least this much is
    // picked and selected there, a stroke reaching each in proportion; a framed region's sphere reaches this far past
    // its farthest joint, or past this share of its frame bone's largest side, and at least this far
    public static final String CHARACTER_REGION_HAIR_FRAME = "head";
    public static final String CHARACTER_REGION_NO_PREFIX = "";
    public static final float CHARACTER_REGION_PICK_SHARE = 0.5f;
    public static final float CHARACTER_REGION_FRAME_MARGIN = 1.35f;
    public static final float CHARACTER_REGION_FRAME_MIN_RADIUS = 0.08f;
    public static final float CHARACTER_REGION_FRAME_SIZE_SHARE = 0.5f;
    public static final String CHARACTER_REGION_STATUS_NO_PART = "This character wears no ";

    // Character Regions — the name each region is listed under in the Layers tab
    public static final String CHARACTER_REGION_LABEL_WHOLE = "Body";
    public static final String CHARACTER_REGION_LABEL_HEAD = "Head";
    public static final String CHARACTER_REGION_LABEL_HAIR = "Hair";
    public static final String CHARACTER_REGION_LABEL_EARS = "Ears";
    public static final String CHARACTER_REGION_LABEL_EYES = "Eyes";
    public static final String CHARACTER_REGION_LABEL_MOUTH = "Mouth";
    public static final String CHARACTER_REGION_LABEL_HANDS = "Hands";
    public static final String CHARACTER_REGION_LABEL_FEET = "Feet";

    // Character Rig Sliders — the prime the bones' slider channels are folded together by, telling a change of them
    // from a joint dragged, the bone field they are written in, and the Rig tab's rows that offer them
    public static final long CHARACTER_RIG_SLIDER_SIGNATURE_PRIME = 1000003L;
    public static final String CHARACTER_RIG_FIELD_SLIDERS = "sliders";
    public static final String CHARACTER_RIG_FIELD_FOLLOWS = "follows";
    public static final String CHARACTER_RIG_KEY_SLIDER = "rig_slider";
    public static final String CHARACTER_RIG_HEADER_SLIDERS = "Sliders";

    // Character Controls — the fields of a rig's controls document, the name a new control takes with its number,
    // the bone sliders one needs, how near a key's place counts as the place itself, the passes a key's share is
    // worked out in, and what the Appearance rows report
    public static final String CHARACTER_CONTROL_FIELD_CONTROLS = "controls";
    public static final String CHARACTER_CONTROL_FIELD_NAME = "name";
    public static final String CHARACTER_CONTROL_FIELD_GROUP = "group";
    public static final String CHARACTER_CONTROL_FIELD_DRIVES = "drives";
    public static final String CHARACTER_CONTROL_FIELD_SLIDER = "slider";
    public static final String CHARACTER_CONTROL_FIELD_INCREASE = "increase";
    public static final String CHARACTER_CONTROL_FIELD_KEYS = "keys";
    public static final String CHARACTER_CONTROL_NEW_NAME = "control_";
    public static final int CHARACTER_CONTROL_MIN_SLIDERS = 2;
    public static final float CHARACTER_CONTROL_KEY_REACH = 0.02f;
    public static final int CHARACTER_CONTROL_SOLVE_PASSES = 4;
    public static final String CHARACTER_CONTROL_MESSAGE_PICK = "Pick two bone sliders or more for the control first";
    public static final String CHARACTER_CONTROL_MESSAGE_ADDED = "Control added: ";
    public static final String CHARACTER_CONTROL_MESSAGE_REMOVED = "Control removed: ";
    public static final String CHARACTER_CONTROL_MESSAGE_AT_ZERO = "Move the control away from zero to set a key";

    // UV Editing — the keys and labels of every UV action, the action groups they are listed under, and the status
    // the UV tabs report
    public static final String UV_KEY_MARK_SEAM = "uv_mark_seam";
    public static final String UV_KEY_CLEAR_SEAM = "uv_clear_seam";
    public static final String UV_KEY_SEAMS_FROM_ISLANDS = "uv_seams_from_islands";
    public static final String UV_KEY_UNWRAP = "uv_unwrap";
    public static final String UV_KEY_PACK = "uv_pack";
    public static final String UV_KEY_AVERAGE_SCALE = "uv_average_scale";
    public static final String UV_KEY_STITCH = "uv_stitch";
    public static final String UV_KEY_PIN = "uv_pin";
    public static final String UV_KEY_UNPIN = "uv_unpin";
    public static final String UV_KEY_FLIP_U = "uv_flip_u";
    public static final String UV_KEY_FLIP_V = "uv_flip_v";
    public static final String UV_KEY_SELECT_ALL = "uv_select_all";
    public static final String UV_KEY_SELECT_LINKED = "uv_select_linked";
    public static final String UV_KEY_CENTER = "uv_center";
    public static final String UV_LABEL_MARK_SEAM = "Mark Seam";
    public static final String UV_LABEL_CLEAR_SEAM = "Clear Seam";
    public static final String UV_LABEL_SEAMS_FROM_ISLANDS = "Seams From Islands";
    public static final String UV_LABEL_UNWRAP = "Unwrap";
    public static final String UV_LABEL_PACK = "Pack Islands";
    public static final String UV_LABEL_AVERAGE_SCALE = "Average Island Scale";
    public static final String UV_LABEL_STITCH = "Stitch";
    public static final String UV_LABEL_PIN = "Pin";
    public static final String UV_LABEL_UNPIN = "Unpin";
    public static final String UV_LABEL_FLIP_U = "Flip U";
    public static final String UV_LABEL_FLIP_V = "Flip V";
    public static final String UV_LABEL_SELECT_ALL = "Select All";
    public static final String UV_LABEL_SELECT_LINKED = "Select Linked";
    public static final String UV_HEADER_SEAMS = "Seams";
    public static final String UV_HEADER_UNWRAP = "Unwrap";
    public static final String UV_HEADER_ISLANDS = "Islands";
    public static final String UV_HEADER_SELECTION = "Selection";
    public static final String UV_HEADER_SELECTED = "UVs selected: ";
    public static final String[] UV_CENTER_LABELS = { "U", "V" };
    public static final float UV_CENTER_STEP = 0.001f;
    public static final String UV_MESSAGE_NO_EDGES = "Select the vertices of the edges to cut";
    public static final String UV_MESSAGE_NO_ISLAND_EDGES = "No edge between two UV islands is shown";
    public static final String UV_MESSAGE_SEAMS_MARKED = "Seams marked: ";
    public static final String UV_MESSAGE_SEAMS_CLEARED = "Seams cleared: ";
    public static final String UV_MESSAGE_NO_FACES = "Select the faces to unwrap";
    public static final String UV_MESSAGE_UNWRAPPED = "Islands unwrapped: ";
    public static final String UV_MESSAGE_UNWRAP_FAILED = "Islands with no area or no boundary, left as they were: ";
    public static final String UV_MESSAGE_NO_CORNERS = "Select UVs in the UV Editor first";
    public static final String UV_MESSAGE_NO_STITCH = "No selected UV meets a neighbouring island";
    public static final String UV_MESSAGE_STITCHED = "Islands stitched: ";
    public static final String UV_MESSAGE_PINNED = "UVs pinned: ";
    public static final String UV_MESSAGE_UNPINNED = "UVs unpinned: ";

    // UV Unwrap — the least squares solve: terms of one conformality row and of one triangle (three gradients and
    // the area weight), the conjugate gradient limits, and the area below which a triangle counts as none
    public static final int UV_UNWRAP_ROW_TERMS = 6;
    public static final int UV_UNWRAP_TRIANGLE_TERMS = 7;
    public static final int UV_UNWRAP_TRIANGLE_CORNERS = 3;
    public static final int UV_UNWRAP_MIN_ITERATIONS = 500;
    public static final int UV_UNWRAP_ITERATIONS_PER_UNKNOWN = 2;
    public static final double UV_UNWRAP_TOLERANCE = 1e-9;
    public static final double UV_UNWRAP_EPSILON = 1e-12;

    // UV Islands — how near two UVs must lie to count as one, the margin packed islands keep as a share of the tile,
    // and how much wider than the islands' area suggests the packing shelves run
    public static final float UV_WELD_EPSILON = 1e-5f;
    public static final float UV_PACK_MARGIN = 0.01f;
    public static final float UV_PACK_SHELF_WIDTH = 1.1f;

    // Animation Editor — clip fields
    public static final String ANIMATION_EDITOR_FIELD_RIG = "rig";
    public static final String ANIMATION_EDITOR_FIELD_LOOP = "loop";
    public static final String ANIMATION_EDITOR_FIELD_TRACKS = "tracks";
    public static final String ANIMATION_EDITOR_FIELD_PARTS = "parts";
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

    // Animation Editor — timing, steps and mirroring; an IK handle's end is held this far inside the chain's reach,
    // and a bend or turn shorter than this is none
    public static final float ANIMATION_EDITOR_FRAME_RATE = 30f;
    public static final float ANIMATION_EDITOR_TIMELINE_MIN_SPAN = 1f;
    public static final float ANIMATION_EDITOR_KEY_EPSILON = 0.0005f;
    public static final float ANIMATION_EDITOR_ROTATION_STEP = 5f;
    public static final float ANIMATION_EDITOR_POSITION_STEP = 0.005f;
    public static final float ANIMATION_EDITOR_SCALE_STEP = 0.05f;
    public static final float ANIMATION_EDITOR_SCALE_MIN = 0.01f;
    public static final float ANIMATION_EDITOR_IK_REACH_MARGIN = 0.0005f;
    public static final float ANIMATION_EDITOR_IK_POLE_EPSILON = 0.000001f;
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
    public static final String ANIMATION_EDITOR_LABEL_CHARACTER = "Entity";
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
    public static final String ANIMATION_EDITOR_MESSAGE_REFUSED = "Not keyed: ";
    public static final String ANIMATION_EDITOR_MESSAGE_CREATED = "Created ";
    public static final String ANIMATION_EDITOR_MESSAGE_SAVED = "Saved ";
    public static final String ANIMATION_EDITOR_MESSAGE_REVERTED = "Reverted ";
    public static final String ANIMATION_EDITOR_MESSAGE_DELETED = "Deleted ";
    public static final String ANIMATION_EDITOR_MESSAGE_POSE_COPIED = "Pose copied";
    public static final String ANIMATION_EDITOR_STATUS_POSE_HINT = "Press Record, then drag a joint to pose its"
            + " bone, keyed at the playhead; X Y Z hold a turn to an axis, Ctrl snaps; drag off the joints to orbit";
    public static final String ANIMATION_EDITOR_STATUS_NOT_RECORDING = "Record is off: press Record on the"
            + " Recorder, then pose; every bone you move is keyed at the playhead";

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

    // Camera Planner — the view a new camera starts with, how far each step moves it, and its status line
    public static final String CAMERA_PLANNER_DEFAULT_NAME = "Camera";
    public static final String CAMERA_PLANNER_NAME_SEPARATOR = " ";
    public static final float CAMERA_PLANNER_DEFAULT_HEIGHT = 16f;
    public static final float CAMERA_PLANNER_DEFAULT_YAW = 0f;
    public static final float CAMERA_PLANNER_DEFAULT_PITCH = -10f;
    public static final float CAMERA_PLANNER_HEIGHT_STEP = 2f;
    public static final float CAMERA_PLANNER_YAW_STEP = 15f;
    public static final float CAMERA_PLANNER_PITCH_STEP = 5f;
    public static final String CAMERA_PLANNER_VALUE_FORMAT = "%.1f";
    public static final String CAMERA_PLANNER_MESSAGE_PLACING = "Placing cameras: click the World Editor with Place";
    public static final String CAMERA_PLANNER_MESSAGE_RELEASED = "No longer editing a camera";
    public static final String CAMERA_PLANNER_MESSAGE_NOTHING_EDITED = "Select a camera first";
    public static final String CAMERA_PLANNER_MESSAGE_EDITING = "Editing ";
    public static final String CAMERA_PLANNER_MESSAGE_MOVED = "Moved";
    public static final String CAMERA_PLANNER_MESSAGE_REMOVED = "Removed ";
    public static final String CAMERA_PLANNER_MESSAGE_PREVIEWING = "Previewing the main menu from ";
    public static final String CAMERA_PLANNER_PREVIEW_RANDOM = "a random camera";
    public static final String CAMERA_PLANNER_STATUS_SEPARATOR = "  |  ";
    public static final String CAMERA_PLANNER_STATUS_COLUMN_X = "  X ";
    public static final String CAMERA_PLANNER_STATUS_Z = "  Z ";
    public static final String CAMERA_PLANNER_STATUS_HEIGHT = "Height ";
    public static final String CAMERA_PLANNER_STATUS_YAW = "  Yaw ";
    public static final String CAMERA_PLANNER_STATUS_PITCH = "  Pitch ";
    public static final String CAMERA_PLANNER_STATUS_COUNT = "Cameras the main menu picks from at random: ";
    public static final String CAMERA_PLANNER_STATUS_NONE = "No cameras: the main menu looks out over its player";
    public static final String CAMERA_PLANNER_STATUS_NOTHING_EDITED =
            "Nothing selected: place a camera on the World Editor, or pick one from the list";

    // Placement Icons — one icon per hand placement on every map, a house for a settlement, a banner turned the way
    // a structure faces and a camera turned the way it looks, the placement a planner edits in its highlight
    public static final String PLACEMENT_ICON_MESH = "editor/EditorViewportQuad";
    public static final String PLACEMENT_ICON_MATERIAL = "editor/PlacementIconMaterial";
    public static final String PLACEMENT_ICON_UNIFORM_SHAPE = "u_iconShape";
    public static final String PLACEMENT_ICON_UNIFORM_FILL_COLOR = "u_fillColor";
    public static final String PLACEMENT_ICON_UNIFORM_CENTER = "u_markerCenter";
    public static final String PLACEMENT_ICON_UNIFORM_DIRECTION = "u_markerDirection";
    public static final String PLACEMENT_ICON_UNIFORM_SCALE = "u_markerScale";
    public static final float PLACEMENT_ICON_SHAPE_SETTLEMENT = 0f;
    public static final float PLACEMENT_ICON_SHAPE_STRUCTURE = 1f;
    public static final float PLACEMENT_ICON_SHAPE_CAMERA = 2f;
    public static final float PLACEMENT_ICON_SIZE_PIXELS = 12f;
    public static final float PLACEMENT_ICON_EDITED_SIZE_PIXELS = 16f;
    public static final float[] PLACEMENT_ICON_COLOR_SETTLEMENT = { 0.95f, 0.78f, 0.32f, 1f };
    public static final float[] PLACEMENT_ICON_COLOR_STRUCTURE = { 0.45f, 0.72f, 0.95f, 1f };
    public static final float[] PLACEMENT_ICON_COLOR_CAMERA = { 0.58f, 0.9f, 0.52f, 1f };
    public static final float[] PLACEMENT_ICON_COLOR_EDITED = { 1f, 0.42f, 0.32f, 1f };

    // Image Editing
    public static final String IMAGE_FORMAT = "png";

    // Map Generation — the switch's on value, how values read, the most a non-metal reflects, and the narrowest a
    // glow's edge fades
    public static final float MAP_GENERATE_TOGGLE_ON = 0.5f;
    public static final String MAP_GENERATE_VALUE_FORMAT = "%.2f";
    public static final float MAP_GENERATE_MAX_DIELECTRIC_SPECULAR = 0.08f;
    public static final float MAP_GENERATE_MIN_GLOW_SOFTNESS = 0.001f;

    // Maps — the Maps tab's rows and status, shared by every window whose image carries maps
    public static final String PALETTE_HEADER_NONE = "Nothing to pick from here";
    public static final String MAPS_HEADER_SETTINGS_SUFFIX = " Settings";
    public static final String MAPS_HEADER_ALBEDO = "The albedo is the image's own layers";
    public static final String MAPS_GENERATED_SUFFIX = " (generated)";
    public static final String MAPS_LABEL_GENERATE = "Generate";
    public static final String MAPS_LABEL_CLEAR = "Clear";
    public static final String MAPS_MESSAGE_ALBEDO = "The albedo is the layers themselves: pick another map to"
            + " generate";
    public static final String MAPS_MESSAGE_NOT_GENERATED = "This map is not there yet: Generate makes it";
    public static final String MAPS_MESSAGE_GENERATED = "Generated ";
    public static final String MAPS_MESSAGE_CLEARED = "Cleared ";

    // Workspace — the folder beside the saves new work is made and kept in until it is imported, the file kind its
    // open dialog lists, and the titles that dialog takes
    public static final String[] WORKSPACE_FILE_PATTERNS = { "*.arpg" };
    public static final String WORKSPACE_FILE_DESCRIPTION = "AdventureRPG files (*.arpg)";
    public static final String[] WORKSPACE_MODEL_PATTERNS = { "*.arpg", "*.glb" };
    public static final String WORKSPACE_MODEL_DESCRIPTION = "Models (*.arpg) or glTF binaries (*.glb)";
    public static final String WORKSPACE_DIALOG_OPEN_IMAGE = "Open Image";
    public static final String WORKSPACE_DIALOG_OPEN_ITEM = "Open Item";
    public static final String WORKSPACE_DIALOG_OPEN_MATERIAL = "Open Material";
    public static final String WORKSPACE_MESSAGE_OUTSIDE = "Open takes a file from ";

    // Image Editor — its folder in the workspace, the keys of its files, the names of its documents in ImageManager,
    // its layers, sizes and backgrounds
    public static final String IMAGE_EDITOR_USER_DIRECTORY = "Images";

    // Image Export and Import — the workspace folder exports are written to, each PNG named for its image and map's
    // suffix after the separator a texture's alias follows, the pack first offered, the dialogs and what they report
    public static final String IMAGE_EXPORT_DIRECTORY = "Exports";
    public static final String IMAGE_STAMP_DIRECTORY = "Stamps";

    // Character Models — the workspace folder models are kept in, the prefix their mesh and preview race go by while
    // worked, the fields of a model file and of the gender a model is worn by, the folder under the game's meshes an
    // import writes to, the dialogs New, Open and Import ask through, and what they report
    public static final String CHARACTER_WORKSPACE_DIRECTORY = "Characters";
    public static final String CHARACTER_WORKSPACE_PREFIX = "workspace/";
    public static final String CHARACTER_MODEL_FIELD_RIG = "rig";
    public static final String CHARACTER_MODEL_FIELD_VAO = "vao";
    public static final String CHARACTER_MODEL_FIELD_GEOMETRY = "mesh";
    public static final String CHARACTER_MODEL_FIELD_GENDERS = "genders";
    public static final String CHARACTER_MODEL_FIELD_MESH = "mesh";
    public static final String CHARACTER_MODEL_FIELD_PLAYABLE = "playable";
    public static final String CHARACTER_MODEL_FIELD_EQUIPMENT = "equipment";
    public static final String CHARACTER_MODEL_FIELD_APPEARANCE = "appearance";
    public static final String[] CHARACTER_MODEL_LOOK_FIELDS = { "head_bone", "colors", "palettes", "details" };
    public static final String[] CHARACTER_MODEL_GENDER_FIELDS = { "rig", "material", "animation_tree" };
    public static final String CHARACTER_MODEL_FIELD_MATERIAL = "material";
    public static final String CHARACTER_MODEL_FIELD_ANIMATION_TREE = "animation_tree";
    public static final String CHARACTER_MODEL_STANDALONE_MATERIAL = "characters/StandardCharacterMaterial";
    public static final String CHARACTER_MODEL_IMPORT_FOLDER = "characters/";
    public static final String CHARACTER_ITEM_IMPORT_FOLDER = "items/painted/";
    public static final String CHARACTER_MODEL_DIALOG_ITEM = "Import: the item it paints (its full name)";
    public static final String CHARACTER_MODEL_MESSAGE_IMPORTED_ITEM = "Imported the painted model into the item ";
    public static final String CHARACTER_MODEL_DEFAULT_RIG = "HumanoidRig";
    public static final String CHARACTER_MODEL_DIALOG_NEW = "New model: its name";
    public static final String CHARACTER_MODEL_DIALOG_RIG = "New model: the rig it is built on";
    public static final String CHARACTER_MODEL_DIALOG_OPEN = "Open Model";
    public static final String CHARACTER_MODEL_DIALOG_RACE =
            "Import: the race it joins (a race on its rig, or a new name), or the item it is worn as (its full name)";
    public static final String CHARACTER_MODEL_DIALOG_WORN_GENDER =
            "Import: the gender whose body it fits (empty for every gender)";
    public static final String CHARACTER_MODEL_DIALOG_WORN_SIDE =
            "Import: the side it is worn on, left or right (empty for either)";
    public static final String CHARACTER_WORN_IMPORT_FOLDER = "worn/";
    public static final String CHARACTER_MODEL_MESSAGE_IMPORTED_WORN = "Imported the model as worn by the item ";
    public static final String CHARACTER_MODEL_DIALOG_GENDER = "Import: the gender it is the model of";
    public static final String CHARACTER_MODEL_DIALOG_ENTITY = "Import: an entity to make of the race (empty for none)";
    public static final String CHARACTER_MODEL_MESSAGE_CREATED = "Made the model ";
    public static final String CHARACTER_MODEL_MESSAGE_OPENED = "Opened the model ";
    public static final String CHARACTER_MODEL_MESSAGE_NOT_READ = "The model could not be read: ";
    public static final String CHARACTER_MODEL_MESSAGE_NOT_WRITTEN = "The model could not be written: ";
    public static final String CHARACTER_MODEL_MESSAGE_NO_RACE =
            "Neither a race nor an animation tree stands on the rig to preview the model with: ";
    public static final String CHARACTER_MODEL_MESSAGE_BLANK =
            "Blank model: choose a root part (such as the Torso) in the Stamps tab and click in the Sculpt tab";
    public static final String CHARACTER_MODEL_MESSAGE_ROOT_ONLY =
            "A blank model starts from a root part, one that joins no host, such as the Torso";
    public static final String CHARACTER_MODEL_MESSAGE_RACE_TABS =
            "Appearance and Equipment belong to a race: Import the model into one to set them";
    public static final String CHARACTER_MODEL_MESSAGE_NOT_RACE =
            "A model is copied or removed as a file in the workspace, not here";
    public static final String CHARACTER_MODEL_MESSAGE_IMPORT_MODEL = "Import takes an open model from the workspace";
    public static final String CHARACTER_MODEL_MESSAGE_IMPORTED = "Imported the model into the race ";
    public static final String IMAGE_STAMP_MESSAGE_NOT_READ = "A stamp picture could not be read: ";
    public static final String IMAGE_STAMP_MESSAGE_PRESSED = "Pressed the stamp ";
    public static final String IMAGE_EXPORT_EXTENSION = ".png";
    public static final String IMAGE_IMPORT_ALIAS_SEPARATOR = "_";
    public static final String IMAGE_IMPORT_DEFAULT_PACK = "surface/standard";
    public static final String IMAGE_IMPORT_DIALOG_PACK = "Import: texture pack (a folder under textures)";
    public static final String IMAGE_IMPORT_DIALOG_TILE = "Import: texture name";
    public static final String IMAGE_EXPORT_MESSAGE_WRITTEN = "Exported maps: ";
    public static final String IMAGE_EXPORT_MESSAGE_TO = " to ";
    public static final String IMAGE_EXPORT_MESSAGE_NOT_WRITTEN = "A map could not be written for ";
    public static final String IMAGE_IMPORT_MESSAGE_EXPORT_FIRST =
            "Export the image first; Import takes its exported maps";
    public static final String IMAGE_IMPORT_MESSAGE_NOT_COPIED = "The maps could not be copied into the textures: ";
    public static final String IMAGE_IMPORT_MESSAGE_NO_MAPS =
            "None of the exported maps is one the game's textures take";
    public static final String IMAGE_IMPORT_MESSAGE_IMPORTED = "Imported and baked the texture ";
    public static final String IMAGE_IMPORT_MESSAGE_REFUSED =
            "Import takes a pack under textures and a texture name with no underscore";
    public static final String IMAGE_FILE_KEY_WIDTH = "width";
    public static final String IMAGE_FILE_KEY_HEIGHT = "height";
    public static final String IMAGE_FILE_KEY_ACTIVE_LAYER = "active_layer";
    public static final String IMAGE_FILE_KEY_NEXT_LAYER = "next_layer";
    public static final String IMAGE_FILE_KEY_LAYERS = "layers";
    public static final String IMAGE_FILE_KEY_NAME = "name";
    public static final String IMAGE_FILE_KEY_OPACITY = "opacity";
    public static final String IMAGE_FILE_KEY_VISIBLE = "visible";
    public static final String IMAGE_FILE_KEY_PIXELS = "pixels";
    public static final String IMAGE_FILE_KEY_MAPS = "maps";
    public static final String IMAGE_FILE_KEY_SETTINGS = "settings";
    public static final String IMAGE_EDITOR_DOCUMENT_PREFIX = "image_editor/";
    public static final String IMAGE_EDITOR_DOCUMENT_SEPARATOR = "/";
    public static final String IMAGE_EDITOR_DOCUMENT_COMPOSITE = "composite";
    public static final String IMAGE_EDITOR_DOCUMENT_LAYER = "layer";
    public static final int IMAGE_EDITOR_MAX_LAYERS = 64;
    public static final String IMAGE_EDITOR_LAYER_NAME = "Layer ";
    public static final String IMAGE_EDITOR_LAYER_BACKGROUND = "Background";
    public static final int IMAGE_EDITOR_SIZE_MIN = 1;
    public static final int IMAGE_EDITOR_SIZE_MAX = 8192;
    public static final int IMAGE_EDITOR_SIZE_STEP = 16;
    public static final int IMAGE_EDITOR_SIZE_DEFAULT = 1024;
    public static final String[] IMAGE_EDITOR_BACKGROUND_LABELS = { "Transparent", "White", "Black", "Paint Colour" };
    public static final int[] IMAGE_EDITOR_BACKGROUND_COLORS = { 0x00000000, 0xFFFFFFFF, 0x000000FF, 0x00000000 };
    public static final int IMAGE_EDITOR_BACKGROUND_PAINT = 3;
    public static final int IMAGE_EDITOR_BACKGROUND_DEFAULT = 1;
    public static final float IMAGE_EDITOR_OPACITY_MIN = 0f;
    public static final float IMAGE_EDITOR_OPACITY_MAX = 1f;
    public static final float IMAGE_EDITOR_OPACITY_STEP = 0.05f;

    // Image Editor — row keys, labels and status
    public static final String IMAGE_EDITOR_KEY_IMAGE = "image_editor_image";
    public static final String IMAGE_EDITOR_KEY_LAYER_NAME = "image_editor_layer_name";
    public static final String IMAGE_EDITOR_KEY_OPACITY = "image_editor_opacity";
    public static final String IMAGE_EDITOR_KEY_MAP_ITEM = "image_editor_map_item";
    public static final String IMAGE_EDITOR_KEY_MAP_SETTING = "image_editor_map_setting";
    public static final String IMAGE_EDITOR_KEY_MAP_GENERATE = "image_editor_map_generate";
    public static final String IMAGE_EDITOR_KEY_MAP_CLEAR = "image_editor_map_clear";
    public static final String IMAGE_EDITOR_KEY_LAYER_ADD = "image_editor_layer_add";
    public static final String IMAGE_EDITOR_KEY_LAYER_REMOVE = "image_editor_layer_remove";
    public static final String IMAGE_EDITOR_LABEL_IMAGE = "Image";
    public static final String IMAGE_EDITOR_LABEL_LAYER_NAME = "Name";
    public static final String IMAGE_EDITOR_LABEL_OPACITY = "Opacity";
    public static final String IMAGE_EDITOR_LABEL_LAYER_ADD = "New Layer";
    public static final String IMAGE_EDITOR_LABEL_LAYER_REMOVE = "Delete Layer";
    public static final String IMAGE_EDITOR_VALUE_NO_IMAGE = "None";
    public static final String IMAGE_EDITOR_HEADER_NO_IMAGE = "No image open: File, New makes one";
    public static final String IMAGE_EDITOR_HEADER_SIZE = "Size ";
    public static final String IMAGE_EDITOR_SIZE_SEPARATOR = " x ";
    public static final String IMAGE_EDITOR_UNSAVED_SUFFIX = " (unsaved)";
    public static final String IMAGE_EDITOR_HEADER_LAYER = "Layer";
    public static final String IMAGE_EDITOR_HEADER_LAYERS = "Layers";
    public static final String IMAGE_EDITOR_HEADER_MAPS = "Maps of ";
    public static final String IMAGE_EDITOR_MESSAGE_NONE = "File, New makes an image; the Image row steps through those"
            + " saved";
    public static final String IMAGE_EDITOR_MESSAGE_NO_IMAGE = "No image open: File, New makes one";
    public static final String IMAGE_EDITOR_MESSAGE_CREATED = "Image made: ";
    public static final String IMAGE_EDITOR_MESSAGE_SAVED = "Image saved: ";
    public static final String IMAGE_EDITOR_MESSAGE_NOT_SAVED = "Image could not be saved: ";
    public static final String IMAGE_EDITOR_MESSAGE_NOT_READ = "Image could not be read: ";
    public static final String IMAGE_EDITOR_MESSAGE_REVERTED = "Image reverted: ";
    public static final String IMAGE_EDITOR_MESSAGE_DELETED = "Image deleted: ";
    public static final String IMAGE_EDITOR_MESSAGE_NOT_DELETED = "Image could not be deleted: ";
    public static final String IMAGE_EDITOR_MESSAGE_LAYER_ADDED = "Layer added: ";
    public static final String IMAGE_EDITOR_MESSAGE_LAYER_REMOVED = "Layer deleted: ";
    public static final String IMAGE_EDITOR_MESSAGE_LAYER_LAST = "An image keeps at least one layer";
    public static final String IMAGE_EDITOR_MESSAGE_LAYER_FULL = "An image holds at most " + IMAGE_EDITOR_MAX_LAYERS
            + " layers";

    // New Image Dialog — its menu, rows and entry points, the fields its rows hand back, and its text
    public static final String MENU_NEW_IMAGE_DIALOG = "editor/NewImageDialog/Dialog";
    public static final String MENU_NEW_IMAGE_HEADER_ROW = "editor/NewImageDialog/new_image_header_row";
    public static final String MENU_NEW_IMAGE_SLIDER_ROW = "editor/NewImageDialog/new_image_slider_row";
    public static final String MENU_NEW_IMAGE_OPTION_ROW = "editor/NewImageDialog/new_image_option_row";
    public static final String MENU_NEW_IMAGE_TEXT_ROW = "editor/NewImageDialog/new_image_text_row";
    public static final int ENTRY_NEW_IMAGE_TITLE = 0;
    public static final int ENTRY_NEW_IMAGE_OPTIONS = 1;
    public static final int ENTRY_NEW_IMAGE_STATUS = 2;
    public static final String ELEMENT_NEW_IMAGE_ROW_LABEL = "new_image_row_label";
    public static final String ELEMENT_NEW_IMAGE_ROW_VALUE = "new_image_row_value";
    public static final String ELEMENT_NEW_IMAGE_ROW_VALUE_BUTTON = "new_image_row_value_button";
    public static final String ELEMENT_NEW_IMAGE_ROW_PREVIOUS = "new_image_row_previous";
    public static final String ELEMENT_NEW_IMAGE_ROW_NEXT = "new_image_row_next";
    public static final String ELEMENT_NEW_IMAGE_SLIDER_TRACK = "new_image_slider_track";
    public static final String ELEMENT_NEW_IMAGE_SLIDER_KNOB = "new_image_slider_knob";
    public static final String NEW_IMAGE_FIELD_NAME = "name";
    public static final String NEW_IMAGE_FIELD_WIDTH = "width";
    public static final String NEW_IMAGE_FIELD_HEIGHT = "height";
    public static final String NEW_IMAGE_FIELD_BACKGROUND = "background";
    public static final String NEW_IMAGE_TITLE = "New Image";
    public static final String NEW_IMAGE_HINT = "The first layer is filled with the background";
    public static final String NEW_IMAGE_HEADER = "Image";
    public static final String NEW_IMAGE_LABEL_NAME = "Name";
    public static final String NEW_IMAGE_LABEL_WIDTH = "Width";
    public static final String NEW_IMAGE_LABEL_HEIGHT = "Height";
    public static final String NEW_IMAGE_LABEL_BACKGROUND = "Background";
    public static final String IMAGE_TEMP_SUFFIX = ".tmp";
    public static final int IMAGE_HISTORY_MAX_STEPS = 64;
    public static final long IMAGE_HISTORY_MAX_PIXELS = 16777216L;
    public static final int IMAGE_SMOOTH_REACH_PIXELS = 1;
    public static final float IMAGE_BRUSH_EDGE_PIXELS = 0.5f;
    public static final int IMAGE_BRUSH_RANDOM_SEED = 0x2545F491;
    public static final float IMAGE_BRUSH_RANDOM_SCALE = 1f / (1 << 24);
    // Levels a raise or lower stamp moves a channel by at full strength, its fraction dithered so light strokes build
    public static final float IMAGE_RAISE_LEVELS = 6f;
    public static final float IMAGE_RELIEF_SLOPE = 0.08f;
    public static final float IMAGE_RELIEF_MIN_Z = 0.05f;
    public static final int IMAGE_PAINT_HSB_BRIGHTNESS = 2;

    // Brushes — loaded from this directory, the default one standing in for any deleted
    public static final String BRUSH_PATH = "brushes";
    public static final String BRUSH_DEFAULT = "Basic";
    public static final String BRUSH_MESSAGE_REFUSED = "Brush not applied: ";
    public static final String BRUSH_SHAPE_ROUND = "Round";
    public static final String BRUSH_SHAPE_SQUARE = "Square";
    public static final String BRUSH_SHAPE_DIAMOND = "Diamond";
    public static final String BRUSH_KEY_HARDNESS = "hardness";
    public static final String BRUSH_KEY_DENSITY = "density";
    public static final String BRUSH_KEY_SPACING = "spacing";
    public static final String BRUSH_KEY_RATIO = "ratio";
    public static final String BRUSH_KEY_ANGLE = "angle";

    // Transitions — loaded from this directory, the default one standing in for any deleted or no longer found; a
    // transition runs at least and at most this long, and unless it says otherwise is first laid this long with an
    // edge this soft
    public static final String TRANSITION_PATH = "transitions";
    public static final String TRANSITION_DEFAULT = "Crossfade";
    public static final String TRANSITION_MESSAGE_REFUSED = "Transition not applied: ";
    public static final float TRANSITION_MIN_SECONDS = 0.05f;
    public static final float TRANSITION_MAX_SECONDS = 10f;
    public static final float TRANSITION_DEFAULT_SECONDS = 1f;
    public static final float TRANSITION_DEFAULT_SOFTNESS = 0.05f;
    public static final String TRANSITION_KIND_DISSOLVE = "Dissolve";
    public static final String TRANSITION_KIND_DIP = "Dip through colour";
    public static final String TRANSITION_KIND_WIPE = "Wipe";
    public static final String TRANSITION_KIND_PUSH = "Push";
    public static final String TRANSITION_KIND_IRIS = "Iris";
    public static final String TRANSITION_DIRECTION_LEFT = "Left";
    public static final String TRANSITION_DIRECTION_RIGHT = "Right";
    public static final String TRANSITION_DIRECTION_UP = "Up";
    public static final String TRANSITION_DIRECTION_DOWN = "Down";
    public static final String TRANSITION_EASING_LINEAR = "Even";
    public static final String TRANSITION_EASING_EASE_IN = "Slow start";
    public static final String TRANSITION_EASING_EASE_OUT = "Slow finish";
    public static final String TRANSITION_EASING_EASE_IN_OUT = "Slow start and finish";
    public static final float BRUSH_FIELD_MIN = 0.05f;
    public static final float BRUSH_HARDNESS_DEFAULT = 1f;
    public static final float BRUSH_DENSITY_DEFAULT = 1f;
    public static final float BRUSH_SPACING_DEFAULT = 0.1f;
    public static final float BRUSH_RATIO_DEFAULT = 1f;
    public static final float BRUSH_ANGLE_DEFAULT = 0f;
    public static final float BRUSH_ANGLE_MAX = 180f;

    // Stamps — body parts loaded from this directory, each carrying its own bones under the host bone it joins, and
    // drawn as a front view on a square thumbnail this many texels a side, a margin around it, shaded by how far each
    // face turns toward the viewer from the least shade given
    public static final String STAMP_PATH = "stamps";
    public static final String STAMP_HOST_BONE = "host";
    public static final String STAMP_RIG_PREFIX = "stamp:";
    public static final String STAMP_MESSAGE_REFUSED = "Stamp not applied: ";
    public static final int STAMP_THUMBNAIL_SIZE = 64;
    public static final float STAMP_THUMBNAIL_MARGIN = 0.08f;
    public static final float STAMP_THUMBNAIL_SHADE_MIN = 0.35f;
    public static final int STAMP_TILES_PER_ROW = 3;
    public static final String STAMP_DETAIL_CATEGORY = "Category: ";
    public static final String STAMP_DETAIL_BONES = "Bones: ";
    public static final String STAMP_DETAIL_FACES = "Faces: ";
    public static final String STAMP_DETAIL_PLUG = "Joins a host at: ";
    public static final String STAMP_DETAIL_ROOT = "Root part: placed at the centre of a blank canvas";
    public static final String STAMP_DETAIL_SOCKETS = "Sockets: ";
    public static final String STAMP_DETAIL_NONE = "No stamp chosen";
    public static final String IMAGE_STAMP_DETAIL_EMPTY = "No pictures: put PNG files in the workspace's Stamps folder";
    public static final String IMAGE_STAMP_DETAIL_SIZE = "Picture: ";
    public static final String IMAGE_STAMP_DETAIL_BY = " x ";
    public static final String STAMP_DETAIL_SOCKET_PREFIX = "Socket: ";
    public static final String STAMP_DETAIL_SEPARATOR = ", ";
    public static final String STAMP_FIELD_DISPLAY_NAME = "display_name";
    public static final String STAMP_FIELD_CATEGORY = "category";
    public static final String STAMP_FIELD_PLUG = "plug";
    public static final String STAMP_FIELD_SOCKETS = "sockets";
    public static final String STAMP_FIELD_BONES = "bones";
    public static final String STAMP_FIELD_PARTS = "parts";
    public static final String STAMP_FIELD_NAME = "name";
    public static final String STAMP_FIELD_BONE = "bone";
    public static final String STAMP_FIELD_POSITION = "position";
    public static final String STAMP_FIELD_NORMAL = "normal";
    public static final String STAMP_FIELD_RADIUS = "radius";
    public static final String STAMP_FIELD_MESH = "mesh";
    public static final String STAMP_FIELD_FUR = "fur";
    public static final String STAMP_FIELD_SPACING = "spacing";
    public static final String STAMP_FIELD_JITTER = "jitter";
    public static final String STAMP_FIELD_ANIMATIONS = "animations";
    public static final String STAMP_FIELD_TRACKS = "tracks";
    public static final String STAMP_FIELD_LEFT = "left";
    public static final String STAMP_FIELD_RIGHT = "right";

    // Stamp Animations — the track groups an animation may hold: either side's, then the left's and the right's
    public static final String[] STAMP_ANIMATION_GROUPS = { STAMP_FIELD_TRACKS, STAMP_FIELD_LEFT, STAMP_FIELD_RIGHT };
    public static final float STAMP_FUR_JITTER_DEFAULT = 0.25f;

    // Character Stamps — the plug loop a part joins by is looked for at no less than this radius
    public static final float CHARACTER_STAMP_RADIUS_MIN = 0.001f;

    // Character Stamp Placement — a part attaches where an attach point (the joint of a bone carrying the body and at
    // least the second share of the rig's height across, or an open loop's centre) lies within the first share of
    // the rig's height of the point aimed at, or snaps to an open loop whose centre projects within the pixels given
    // of the pointer; a hole is cut from the faces within the plug's radius times the hole share, the two loops
    // welded where they hold as many vertices pairing within the weld share of the plug's radius, bridged otherwise,
    // both rims stepping on together while their next vertices lie within the pace share of the shorter step;
    // a part stands on the mirror plane, joining no side, within the centre share of the rig's height; a set-down
    // part turns the degrees given for every pixel the pointer moves; a plug facing straight away from the body is
    // turned half round about the side axis unless it lies within the cosine given of it; a mirrored part's faces
    // take their corners in the mirrored order; a joined part's names take the first number past every name taken
    public static final float CHARACTER_STAMP_ATTACH_REACH_SHARE = 0.12f;
    public static final float CHARACTER_STAMP_JOINT_SIZE_SHARE = 0.05f;
    public static final float CHARACTER_STAMP_LOOP_SNAP_PIXELS = 24f;
    public static final float CHARACTER_STAMP_HOLE_SHARE = 1f;
    public static final float CHARACTER_STAMP_WELD_SHARE = 0.5f;
    public static final float CHARACTER_STAMP_PACE_SHARE = 0.5f;
    public static final float CHARACTER_STAMP_CENTRE_SHARE = 0.02f;
    public static final float CHARACTER_STAMP_TURN_DEGREES_PER_PIXEL = 0.5f;
    public static final float CHARACTER_STAMP_PARALLEL_COSINE = 0.9999f;
    public static final int[] CHARACTER_STAMP_CORNERS = { 0, 1, 2, 3 };
    public static final int[] CHARACTER_STAMP_MIRRORED_CORNERS = { 0, 3, 2, 1 };
    public static final String CHARACTER_STAMP_NUMBER_SEPARATOR = "_";
    public static final int CHARACTER_STAMP_FIRST_NUMBER = 2;
    public static final String CHARACTER_STAMP_MESSAGE_PLACED = "Joined the part ";
    public static final String CHARACTER_STAMP_MESSAGE_ANIMATED = "; its animations joined clips: ";
    public static final String CHARACTER_STAMP_MESSAGE_BODY_ONLY = "A part joins the body in a still pose: pick the"
            + " Body or a body layer, a still pose under Pose and Preview Springs off, or press Esc to put the part"
            + " down";
    public static final String CHARACTER_STAMP_MESSAGE_FAR = "A part attaches near a joint or a yellow attach point";
    public static final String CHARACTER_STAMP_MESSAGE_ROOT = "A root part starts the body over at its centre;"
            + " click to replace the body with it (Undo brings it back)";
    public static final String CHARACTER_STAMP_MESSAGE_NO_LOOP = "The part's plug loop is not open in its mesh";
    public static final String CHARACTER_STAMP_MESSAGE_NO_HOLE = "No faces to open a hole in there";

    // Character Fur — a fur stamp lays one card for every square of the spacing the faces facing it hold, each turned
    // by up to the degrees given times the stamp's jitter, combed along the surface toward the comb (down and back,
    // the character facing +z) or away from it when flipped, the fallback taken where the surface faces the comb; the
    // salt seeds where each card roots and how it varies, each card drawing its channels from its own run of hashes
    public static final float[] CHARACTER_FUR_COMB = { 0f, -1f, -1f };
    public static final float[] CHARACTER_FUR_COMB_FALLBACK = { 0f, 0f, -1f };
    public static final float CHARACTER_FUR_TURN_JITTER_DEGREES = 30f;
    public static final long CHARACTER_FUR_SALT = 0x5E3D9A17C42B6F81L;
    public static final int CHARACTER_FUR_COMB_FLIPPED = 1;
    public static final int CHARACTER_FUR_HASH_CHANNELS = 4;
    public static final int CHARACTER_FUR_CHANNEL_U = 0;
    public static final int CHARACTER_FUR_CHANNEL_V = 1;
    public static final int CHARACTER_FUR_CHANNEL_LENGTH = 2;
    public static final int CHARACTER_FUR_CHANNEL_TURN = 3;
    public static final String CHARACTER_FUR_MESSAGE_LAID = "Laid fur cards: ";
    public static final String CHARACTER_FUR_MESSAGE_NONE = "No faces face the fur there";

    // Brush Thumbnails — a stroke of the tip along one wave across a strip this many texels, stamped at this radius,
    // its path walked in steps this many texels long
    public static final int BRUSH_THUMBNAIL_WIDTH = 96;
    public static final int BRUSH_THUMBNAIL_HEIGHT = 32;
    public static final float BRUSH_THUMBNAIL_RADIUS = 9f;
    public static final float BRUSH_THUMBNAIL_WAVE = 0.22f;
    public static final float BRUSH_THUMBNAIL_PATH_STEP = 0.25f;
    public static final int BRUSH_THUMBNAIL_SEED = 0x3C6EF372;

    // Tools — sizes are brush radii, in image pixels on the world's layers and window pixels on a character; every
    // tool's emblem is its PascalCase name under the icon folder
    public static final String TOOL_ICON_FOLDER = "menus/tools/";
    public static final String TOOL_SELECT = "Select";
    public static final String TOOL_BRUSH = "Brush";
    public static final String TOOL_ERASE = "Erase";
    public static final String TOOL_SMOOTH = "Smooth";
    public static final String TOOL_SWIRL = "Swirl";
    public static final String TOOL_SCATTER = "Scatter";
    public static final String TOOL_FILL = "Fill";
    public static final String TOOL_PICK = "Pick";
    public static final String TOOL_PLACE = "Place";
    public static final String TOOL_WALL = "Wall";
    public static final String TOOL_MOVE = "Move";
    public static final String TOOL_ROTATE = "Rotate";
    public static final String TOOL_SCALE = "Scale";
    public static final String TOOL_CUT = "Cut";
    public static final String TOOL_IK = "IK Handle";
    public static final String TOOL_POSE = "Pose";
    public static final String TOOL_INFLATE = "Inflate";
    public static final String TOOL_PINCH = "Pinch";
    public static final String TOOL_CREASE = "Crease";
    public static final String TOOL_RELAX = "Relax";
    public static final String TOOL_FLATTEN = "Flatten";
    public static final String TOOL_NUDGE = "Nudge";
    public static final String TOOL_RAISE = "Raise";
    public static final String TOOL_LOWER = "Lower";
    public static final String TOOL_TOPOLOGY = "Topology";
    public static final String TOOL_FLIP_EDGE = "Flip Edge";
    public static final String TOOL_LOOP_CUT = "Loop Cut";
    public static final String TOOL_DISSOLVE_LOOP = "Dissolve Loop";
    public static final String TOOL_DRAW = "Draw";
    public static final String TOOL_LIGHT = "Light";
    public static final String TOOL_COLLIDER = "Collider";
    public static final String TOOL_HAND = "Hand";
    public static final String TOOL_CANVAS_BIOMES = "World Biomes";
    public static final String TOOL_CANVAS_HEIGHTS = "World Heights";
    public static final String TOOL_CANVAS_SETTLEMENTS = "World Placements";
    public static final String TOOL_CANVAS_CAMERAS = "World Cameras";
    public static final String TOOL_CANVAS_CHARACTER_MESH = "Mesh";
    public static final String TOOL_CANVAS_CHARACTER_WEIGHTS = "Weights";
    public static final String TOOL_CANVAS_CHARACTER_SCULPT = "Sculpt";
    public static final String TOOL_CANVAS_CHARACTER_RIG = "Rig";
    public static final String TOOL_CANVAS_CHARACTER_PAINT = "Paint";
    public static final String TOOL_CANVAS_CHARACTER_UV = "UV";
    public static final String TOOL_CANVAS_CHARACTER_TOPOLOGY = "Topology";
    public static final String TOOL_CANVAS_CHARACTER_HAIR = "Hair";
    public static final String TOOL_CANVAS_CHARACTER_POSE = "Pose";
    public static final String TOOL_CANVAS_UV_EDITOR = "UV Editor";
    public static final String TOOL_CANVAS_IMAGE_EDITOR = "Image Editor";
    public static final String TOOL_CANVAS_MATERIAL_VIEWER = "Material Viewer";
    public static final String TOOL_CANVAS_TREE = "Tree";
    public static final String TOOL_CANVAS_ANIMATION_POSE = "Animation Pose";
    public static final String TOOL_CANVAS_ITEM = "Item";
    public static final String TOOL_CANVAS_STRUCTURE = "Structure";
    public static final String TOOL_CANVAS_RECORDER = "Timeline";
    public static final String TOOL_ACTION_HEADER_NESTING = "Nesting";
    public static final String TOOL_ACTION_LABEL_NEST_SELECTION = "Nest Selection";
    public static final String TOOL_ACTION_LABEL_OPEN_NESTED = "Open Nested";
    public static final String TOOL_ACTION_HEADER_EXPORT = "Export";
    public static final String TOOL_ACTION_LABEL_EXPORT_MIDI = "Export MIDI";
    public static final String TOOL_ACTION_HEADER_TAKES = "Takes";
    public static final String TOOL_ACTION_LABEL_TAKE_PLACE = "Lay Take";
    public static final String TOOL_ACTION_LABEL_TAKE_RENAME = "Rename Take";
    public static final String TOOL_ACTION_LABEL_TAKE_DELETE = "Delete Take";
    public static final String TOOL_ACTION_HEADER_CLIP = "Clip";
    public static final String TOOL_ACTION_LABEL_TRIM_START = "Trim Start";
    public static final String TOOL_ACTION_LABEL_TRIM_END = "Trim End";
    public static final String TOOL_ACTION_LABEL_SPLIT = "Split at Playhead";
    public static final String TOOL_ACTION_LABEL_STAMP = "Stamp Edit";
    public static final String TOOL_ACTION_LABEL_TRANSITION_IN = "Transition In";
    public static final String TOOL_ACTION_LABEL_TRANSITION_OUT = "Transition Out";
    public static final String TOOL_ACTION_LABEL_REMOVE_CLIP = "Remove Clip";
    public static final String TOOL_ACTION_LABEL_FILTER_APPLY = "Apply Filter";
    public static final String TOOL_ACTION_LABEL_FILTER_REMOVE = "Remove Filter";
    public static final String TOOL_ACTION_HEADER_KEYS = "Keys";
    public static final String TOOL_ACTION_LABEL_KEY_ADD = "Add Key";
    public static final String TOOL_ACTION_LABEL_KEY_DELETE = "Delete Keys";
    public static final String TOOL_ACTION_LABEL_KEY_COPY = "Copy Keys";
    public static final String TOOL_ACTION_LABEL_KEY_PASTE = "Paste Keys";
    public static final String TOOL_ACTION_LABEL_KEY_MODE = "Key Mode";
    public static final String TOOL_ACTION_LABEL_KEY_VALUE = "Key Value";
    public static final String TOOL_ACTION_LABEL_FRAME_KEYS = "Frame Keys";
    public static final String TOOL_ACTION_LABEL_CURVE_REFIT = "Refit Curve";
    public static final String TOOL_ACTION_LABEL_TAKE_REFIT = "Refit Take";
    public static final String TOOL_ACTION_HEADER_SOUNDS = "Sounds";
    public static final String TOOL_ACTION_LABEL_SOUND_ADD = "Add Sound";
    public static final String TOOL_ACTION_LABEL_SOUND_VOLUME = "Sound Volume";
    public static final String TOOL_ACTION_LABEL_SOUND_PITCH = "Sound Pitch";
    public static final String TOOL_ACTION_LABEL_SOUND_LENGTH = "Sound Length";
    public static final String TOOL_ACTION_LABEL_SOUND_SEEK = "Playhead to Sound";
    public static final String TOOL_ACTION_LABEL_SOUND_COPY = "Copy to Playhead";
    public static final String TOOL_ACTION_LABEL_SOUND_REPLACE = "Replace Sound";
    public static final String TOOL_ACTION_LABEL_SOUND_REMOVE = "Remove Sound";
    public static final String TOOL_ACTION_LABEL_SOUND_REMOVE_ALL = "Remove Every Time";
    public static final String TOOL_ACTION_HEADER_TRANSITION = "Transition";
    public static final String TOOL_ACTION_LABEL_TRANSITION_LENGTH = "Transition Length";
    public static final String TOOL_ACTION_LABEL_TRANSITION_REPLACE = "Replace Transition";
    public static final String TOOL_ACTION_LABEL_TRANSITION_REMOVE = "Remove Transition";
    public static final String TOOL_ACTION_HEADER_NODE = "Movement";
    public static final String TOOL_ACTION_LABEL_NODE_PREVIOUS = "Previous State";
    public static final String TOOL_ACTION_LABEL_NODE_NEXT = "Next State";
    public static final String TOOL_ACTION_LABEL_NODE_REMOVE = "Remove Node";
    public static final String TOOL_OPTION_SIZE = "Size";
    public static final String TOOL_OPTION_STRENGTH = "Strength";
    public static final String TOOL_OPTION_TWIST = "Twist";
    public static final String TOOL_OPTION_SCALE = "Scale";
    public static final String TOOL_OPTION_COMB = "Comb";
    public static final String TOOL_OPTION_TURN = "Turn";
    public static final String[] TOOL_COMB_NAMES = { "Back and Down", "Forward and Up" };
    public static final int TOOL_COMB_DEFAULT = 0;
    public static final String TOOL_OPTION_MIRROR = "Mirror";
    public static final String TOOL_OPTION_XRAY = "X-Ray";
    public static final String TOOL_OPTION_WIREFRAME = "Wireframe";
    public static final String TOOL_OPTION_DETAIL = "Detail (mm)";
    public static final String TOOL_OPTION_LENGTH = "Length (cm)";
    public static final String TOOL_OPTION_WIDTH = "Width (mm)";
    public static final String TOOL_OPTION_SHAPE = "Shape";
    public static final String TOOL_OPTION_DEPTH = "Depth";
    public static final String TOOL_OPTION_SPACING = "Spacing";
    public static final String[] TOOL_SHAPE_NAMES = { "Sphere", "Cube", "Cylinder", "Plane" };
    public static final int TOOL_SHAPE_DEFAULT = 0;
    public static final String[] TOOL_OPTION_SWITCH_NAMES = { "Off", "On" };
    public static final int TOOL_OPTION_SWITCH_OFF = 0;
    public static final int TOOL_OPTION_SWITCH_ON = 1;
    public static final float TOOL_OPTION_PRECISION = 100f;
    public static final float TOOL_IMAGE_SIZE_MIN = 0f;
    public static final float TOOL_IMAGE_SIZE_MAX = 32f;
    public static final float TOOL_IMAGE_SIZE_STEP = 1f;
    public static final float TOOL_IMAGE_SIZE_DEFAULT = 1f;
    public static final float TOOL_ITEM_SIZE_MIN = 0f;
    public static final float TOOL_ITEM_SIZE_MAX = 32f;
    public static final float TOOL_ITEM_SIZE_STEP = 1f;
    public static final float TOOL_ITEM_SIZE_DEFAULT = 0f;
    public static final float TOOL_STRUCTURE_SIZE_MIN = 1f;
    public static final float TOOL_STRUCTURE_SIZE_MAX = 8f;
    public static final float TOOL_STRUCTURE_SIZE_STEP = 1f;
    public static final float TOOL_STRUCTURE_SIZE_DEFAULT = 1f;
    public static final float TOOL_ITEM_DEPTH_MIN = 1f;
    public static final float TOOL_ITEM_DEPTH_MAX = 32f;
    public static final float TOOL_ITEM_DEPTH_STEP = 1f;
    public static final float TOOL_ITEM_DEPTH_DEFAULT = 1f;

    // Item Spacing — the sub-voxels the pointer travels between two stamps of a drag, and how far it must travel from
    // the press before a press becomes a drag at all, so a click lays one stamp however the hand shakes
    public static final float TOOL_ITEM_SPACING_MIN = 1f;
    public static final float TOOL_ITEM_SPACING_MAX = 16f;
    public static final float TOOL_ITEM_SPACING_STEP = 1f;
    public static final float TOOL_ITEM_SPACING_DEFAULT = 1f;
    public static final float TOOL_ITEM_DRAG_THRESHOLD = 2f;
    public static final float TOOL_SCREEN_SIZE_MIN = 4f;
    public static final float TOOL_SCREEN_SIZE_MAX = 400f;
    public static final float TOOL_SCREEN_SIZE_STEP = 5f;
    public static final float TOOL_SCREEN_SIZE_DEFAULT = 40f;
    public static final float TOOL_STRENGTH_STEP = 0.05f;
    public static final float TOOL_STRENGTH_DEFAULT = 1f;
    public static final float TOOL_WEIGHT_STRENGTH_DEFAULT = 0.2f;
    public static final float TOOL_SCULPT_STRENGTH_DEFAULT = 0.5f;
    public static final float TOOL_TOPOLOGY_STRENGTH_DEFAULT = 0.5f;
    public static final float TOOL_TOPOLOGY_DETAIL_MIN = 1f;
    public static final float TOOL_TOPOLOGY_DETAIL_MAX = 50f;
    public static final float TOOL_TOPOLOGY_DETAIL_STEP = 0.5f;
    public static final float TOOL_TOPOLOGY_DETAIL_DEFAULT = 6f;
    public static final float TOOL_HAIR_STRENGTH_DEFAULT = 0.5f;
    public static final float TOOL_HAIR_LENGTH_MIN = 0.5f;
    public static final float TOOL_HAIR_LENGTH_MAX = 120f;
    public static final float TOOL_HAIR_LENGTH_STEP = 0.5f;
    public static final float TOOL_HAIR_LENGTH_DEFAULT = 12f;
    public static final float TOOL_HAIR_WIDTH_MIN = 1f;
    public static final float TOOL_HAIR_WIDTH_MAX = 120f;
    public static final float TOOL_HAIR_WIDTH_STEP = 0.5f;
    public static final float TOOL_HAIR_WIDTH_DEFAULT = 20f;
    public static final float TOOL_PAINT_STRENGTH_DEFAULT = 0.5f;
    public static final float TOOL_TWIST_STEP = 15f;
    public static final float TOOL_TWIST_MAX = 360f;
    public static final float TOOL_TWIST_DEFAULT = 90f;
    public static final float TOOL_STAMP_SCALE_MIN = 0.25f;
    public static final float TOOL_STAMP_SCALE_MAX = 4f;
    public static final float TOOL_STAMP_SCALE_STEP = 0.05f;
    public static final float TOOL_STAMP_SCALE_DEFAULT = 1f;
    public static final float TOOL_STAMP_TURN_MAX = 180f;
    public static final float TOOL_STAMP_TURN_DEFAULT = 0f;
    public static final String TOOL_STATUS_SIZE = "Size ";

    // Tool State — the tools as they were left, kept beside the layouts in the editor's bin directory
    public static final String TOOL_STATE_FILE_NAME = "EditorTools";
    public static final String TOOL_STATE_KEY_BRUSH = "brush";
    public static final String TOOL_STATE_KEY_CANVASES = "canvases";
    public static final String TOOL_STATE_KEY_TOOL = "tool";
    public static final String TOOL_STATE_KEY_OPTIONS = "options";

    // World Editor
    public static final String WORLD_EDITOR_MODE_PAINT = "Paint";
    public static final String WORLD_EDITOR_MODE_SETTLEMENTS = "Settlements";
    public static final String WORLD_EDITOR_MODE_CAMERAS = "Cameras";
    public static final String WORLD_EDITOR_LAYER_BIOMES = "Biomes";
    public static final String WORLD_EDITOR_LAYER_HEIGHTS = "Heights";
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
    public static final String WORLD_EDITOR_STATUS_NO_BIOME =
            "No biome selected: list the biomes this world paints with in its Biomes field (Worlds tab)";
    public static final String WORLD_EDITOR_KEY_BIOMES = "biomes";
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

    // Trees — what the Tree Planner and Tree Viewer share: the seed the preview starts from, the age it grows its
    // tree to and how far one step moves it, the least height a frame takes, the viewer's layers (every LOD level,
    // then the shared tree), the Tools tab's Preview actions, the precision a stepped or typed decimal is kept to, the
    // Palette's header and the Properties tab's title suffix, and the messages
    public static final long TREE_VIEWER_FIRST_SEED = 1L;
    public static final float TREE_VIEWER_AGE_DEFAULT = 1f;
    public static final float TREE_VIEWER_AGE_MIN = 0.05f;
    public static final float TREE_VIEWER_AGE_STEP = 0.1f;
    public static final int TREE_VIEWER_STEP_DOWN = -1;
    public static final int TREE_VIEWER_STEP_UP = 1;
    public static final float TREE_VIEWER_MIN_FRAME_BLOCKS = 2f;
    public static final int TREE_VIEWER_LAYER_SHARED = EngineSetting.TREE_LOD_LEVEL_COUNT;
    public static final int TREE_VIEWER_LAYER_COUNT = TREE_VIEWER_LAYER_SHARED + 1;
    public static final String TREE_VIEWER_HEADER_PREVIEW = "Preview";
    public static final String TREE_VIEWER_KEY_RESEED = "tree_viewer_reseed";
    public static final String TREE_VIEWER_KEY_YOUNGER = "tree_viewer_younger";
    public static final String TREE_VIEWER_KEY_OLDER = "tree_viewer_older";
    public static final String TREE_VIEWER_LABEL_RESEED = "New Seed";
    public static final String TREE_VIEWER_LABEL_YOUNGER = "Younger";
    public static final String TREE_VIEWER_LABEL_OLDER = "Older";
    public static final String TREE_VIEWER_MESSAGE_SAVED = "Saved ";
    public static final String TREE_VIEWER_MESSAGE_SAVE_FAILED = "Could not save ";
    public static final String TREE_VIEWER_MESSAGE_REVERTED = "Reverted ";
    public static final String TREE_VIEWER_MESSAGE_RESEEDED = "Grown from a new seed";
    public static final String TREE_VIEWER_MESSAGE_AGED = "Grown to a new age";
    public static final String TREE_VIEWER_MESSAGE_NO_TREE = "Select a tree first";
    public static final String TREE_VIEWER_MESSAGE_SET = "Set ";
    public static final int TREE_PLANNER_VALUE_PRECISION = 10000;
    public static final String TREE_PLANNER_VALUE_FORMAT = "%.2f";
    public static final String TREE_PLANNER_PALETTE_HEADER = "Trees";
    public static final String TREE_PLANNER_FIELDS_SUFFIX = " Fields";

    // Item Viewer Material — the material the Ship Viewer and the Architecture Builder draw meshes and items with as
    // the game's G-buffer receives them, its painted twin, and the cutaway height above which it clips, every model
    // blocks high unless a viewer cuts it away
    public static final String ITEM_VIEWER_MATERIAL = "editor/ShipViewerMaterial";
    public static final String ITEM_VIEWER_PAINTED_MATERIAL = "editor/ShipViewerPaintedMaterial";
    public static final String ITEM_VIEWER_UNIFORM_CUTAWAY_HEIGHT = "u_cutawayHeight";
    public static final float ITEM_VIEWER_CUTAWAY_NONE = 100000f;

    // Architecture Builder — the roots of its layers and the separator between a layer's kind and its slot or group,
    // the prefixes its Palette rows carry, the text of its labels and the messages of its file actions
    public static final String ARCHITECTURE_BUILDER_LAYER_BLOCKS = "Blocks";
    public static final String ARCHITECTURE_BUILDER_LAYER_FURNITURE = "Furniture";
    public static final char ARCHITECTURE_BUILDER_LAYER_SEPARATOR = ':';
    public static final String ARCHITECTURE_BUILDER_ROW_ARCHITECTURE = "architecture:";
    public static final String ARCHITECTURE_BUILDER_ROW_FILL = "fill:";
    public static final String ARCHITECTURE_BUILDER_ROW_EMPTY = "empty";
    public static final String ARCHITECTURE_BUILDER_PALETTE_ARCHITECTURES = "Architectures";
    public static final String ARCHITECTURE_BUILDER_LABEL_SEPARATOR = "  ·  ";
    public static final String ARCHITECTURE_BUILDER_EMPTY_BLOCK = "As Built";
    public static final String ARCHITECTURE_BUILDER_EMPTY_FURNITURE = "Drawn From Its Table";
    public static final String ARCHITECTURE_BUILDER_MESSAGE_NONE = "Select an architecture first";
    public static final String ARCHITECTURE_BUILDER_MESSAGE_SAVED = "Saved ";
    public static final String ARCHITECTURE_BUILDER_MESSAGE_SAVE_FAILED = "Could not save ";
    public static final String ARCHITECTURE_BUILDER_MESSAGE_REVERTED = "Reverted ";

    // Structure Editor — its jobs and the layers holding them, its layer actions, the marker an NPC is drawn with,
    // the prefixes its Palette rows carry, the characters its palette keys are drawn
    // from in order, how many strokes its history holds, the text of its labels and its messages
    public static final String STRUCTURE_EDITOR_LAYER_BLOCKS = "Blocks";
    public static final String STRUCTURE_EDITOR_LAYER_COVERAGE = "Coverage";
    public static final String STRUCTURE_EDITOR_LAYER_CONTENTS = "Contents";
    public static final String STRUCTURE_EDITOR_LAYER_FURNITURE = "Furniture";
    public static final String STRUCTURE_EDITOR_LAYER_ENTITIES = "Entities";
    public static final char STRUCTURE_EDITOR_LAYER_SEPARATOR = ':';
    public static final String STRUCTURE_EDITOR_FIRST_LAYER_NAME = "Ground";
    public static final String STRUCTURE_EDITOR_LAYER_NAME = "Layer";
    public static final String STRUCTURE_EDITOR_HEADER_LAYERS = "Layers";
    public static final String STRUCTURE_EDITOR_KEY_LAYER_ADD = "structure_layer_add";
    public static final String STRUCTURE_EDITOR_KEY_LAYER_REMOVE = "structure_layer_remove";
    public static final String STRUCTURE_EDITOR_KEY_LAYER_UP = "structure_layer_up";
    public static final String STRUCTURE_EDITOR_KEY_LAYER_DOWN = "structure_layer_down";
    public static final String STRUCTURE_EDITOR_KEY_LAYER_RENAME = "structure_layer_rename";
    public static final String STRUCTURE_EDITOR_LABEL_LAYER_ADD = "New Layer";
    public static final String STRUCTURE_EDITOR_LABEL_LAYER_REMOVE = "Delete Layer";
    public static final String STRUCTURE_EDITOR_LABEL_LAYER_UP = "Move Layer Up";
    public static final String STRUCTURE_EDITOR_LABEL_LAYER_DOWN = "Move Layer Down";
    public static final String STRUCTURE_EDITOR_LABEL_LAYER_RENAME = "Rename Layer";
    public static final String STRUCTURE_EDITOR_SPAWN_MARKER_TEXTURE = "items/standard/Crimson";
    public static final String STRUCTURE_EDITOR_ROW_STRUCTURE = "structure:";
    public static final String STRUCTURE_EDITOR_ROW_ARCHITECTURE = "architecture:";
    public static final String STRUCTURE_EDITOR_ROW_SLOT = "slot:";
    public static final String STRUCTURE_EDITOR_ROW_ENTRY = "entry:";
    public static final String STRUCTURE_EDITOR_ROW_BLOCK = "block:";
    public static final String STRUCTURE_EDITOR_ROW_COVERING = "covering:";
    public static final String STRUCTURE_EDITOR_ROW_CONTENT = "content:";
    public static final String STRUCTURE_EDITOR_ROW_FURNISHING = "furnishing:";
    public static final String STRUCTURE_EDITOR_ROW_LOOT = "loot:";
    public static final String STRUCTURE_EDITOR_ROW_TURN = "turn:";
    public static final String STRUCTURE_EDITOR_ROW_NPC = "npc:";
    public static final String STRUCTURE_EDITOR_PALETTE_LOOT = "Loot";
    public static final String STRUCTURE_EDITOR_PALETTE_TURN = "Turn";
    public static final String[] STRUCTURE_EDITOR_TURN_LABELS = { "As Placed", "Quarter Turn", "Half Turn",
            "Three Quarter Turn" };
    public static final String STRUCTURE_EDITOR_PALETTE_KEYS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.#*+=-!?$%&@^~:;,|/<>()[]{}";
    public static final int STRUCTURE_EDITOR_HISTORY_STEPS = 64;
    public static final String STRUCTURE_EDITOR_PALETTE_STRUCTURES = "Structures";
    public static final String STRUCTURE_EDITOR_PALETTE_ARCHITECTURES = "Built In";
    public static final String STRUCTURE_EDITOR_PALETTE_SLOTS = " Slots";
    public static final String STRUCTURE_EDITOR_PALETTE_ENTRIES = "This Structure";
    public static final String STRUCTURE_EDITOR_PALETTE_BLOCKS = "Blocks";
    public static final String STRUCTURE_EDITOR_LABEL_AS_AUTHORED = "As Authored";
    public static final String STRUCTURE_EDITOR_LABEL_NONE = "None";
    public static final String STRUCTURE_EDITOR_LABEL_SEPARATOR = "  ·  ";
    public static final String STRUCTURE_EDITOR_MESSAGE_NONE = "Select a structure first";
    public static final String STRUCTURE_EDITOR_MESSAGE_NO_CHOICE = "Choose what to place in the Palette tab first";
    public static final String STRUCTURE_EDITOR_MESSAGE_LAST_LAYER = "A structure keeps at least one layer";
    public static final String STRUCTURE_EDITOR_MESSAGE_HIDDEN = "The layer picked is hidden — show it to work on it";
    public static final String STRUCTURE_EDITOR_MESSAGE_TOO_MANY =
            "Too many different blocks for one palette — not written";
    public static final String STRUCTURE_EDITOR_MESSAGE_UNDONE = "Undone";
    public static final String STRUCTURE_EDITOR_MESSAGE_REDONE = "Redone";
    public static final String STRUCTURE_EDITOR_MESSAGE_SAVED = "Saved ";
    public static final String STRUCTURE_EDITOR_MESSAGE_SAVE_FAILED = "Could not save ";
    public static final String STRUCTURE_EDITOR_MESSAGE_REVERTED = "Reverted ";

    // Tree Impostor Bake — the materials the shared tree is drawn into the bake target with and the uniforms naming
    // each view's camera and the map it writes, its render order, every view of both maps (albedo first), how small a
    // frame's right may come before the world's own x stands in, how many rings out an empty texel takes a covered
    // neighbour's colour, the weight an empty texel keeps in a smaller level, and the least sphere a frame holds
    public static final String TREE_IMPOSTOR_BAKE_BARK_MATERIAL = "editor/TreeImpostorBakeBarkMaterial";
    public static final String TREE_IMPOSTOR_BAKE_LEAF_MATERIAL = "editor/TreeImpostorBakeLeafMaterial";
    public static final String UNIFORM_TREE_IMPOSTOR_BAKE_VIEW_PROJECTION = "u_bakeViewProjection";
    public static final String UNIFORM_TREE_IMPOSTOR_BAKE_MAP = "u_bakeMap";
    public static final int TREE_IMPOSTOR_BAKE_DEPTH = 0;
    public static final int TREE_IMPOSTOR_BAKE_MAP_ALBEDO = 0;
    public static final int TREE_IMPOSTOR_BAKE_VIEWS = EngineSetting.TREE_IMPOSTOR_FRAMES
            * EngineSetting.TREE_IMPOSTOR_FRAMES * 2;
    public static final float TREE_IMPOSTOR_MIN_ACROSS = 0.0001f;
    public static final int TREE_IMPOSTOR_DILATE_RINGS = 4;
    public static final float TREE_IMPOSTOR_EMPTY_WEIGHT = 0.001f;
    public static final float TREE_IMPOSTOR_MIN_RADIUS_BLOCKS = 1f;

    // Item Library — the game's item meshes, the definition file an import first offers, the pack a converted quad
    // mesh is drawn from, and the names new parts take
    public static final String ITEM_EDITOR_MESH_DIRECTORY = "items";
    public static final String ITEM_EDITOR_DEFINITION_FILE = "EditorItems";
    public static final String ITEM_EDITOR_TEXTURE_ARRAY = "items/standard";
    public static final String ITEM_EDITOR_DEFAULT_PART_NAME = "Body";
    public static final String ITEM_EDITOR_PART_NAME_PREFIX = "Part ";

    // Item Workspace — the workspace folder items are kept in until imported, and the dialog Import asks through
    public static final String ITEM_WORKSPACE_DIRECTORY = "Items";
    public static final String ITEM_IMPORT_DIALOG_DEFINITION = "Import: item file the item joins";

    // Item Parts — the Layers tab's and the Tools tab's actions on the item's parts
    public static final String ITEM_EDITOR_HEADER_PARTS = "Parts";
    public static final String ITEM_EDITOR_KEY_PART_ADD = "item_editor_part_add";
    public static final String ITEM_EDITOR_KEY_PART_REMOVE = "item_editor_part_remove";
    public static final String ITEM_EDITOR_LABEL_PART_ADD = "New Part";
    public static final String ITEM_EDITOR_LABEL_PART_REMOVE = "Delete Part";

    // Item Paint — the Tools tab's action that bakes the item and opens it in the Paint tab with the Material tool
    public static final String ITEM_EDITOR_HEADER_PAINT = "Texture";
    public static final String ITEM_EDITOR_KEY_PAINT = "item_editor_paint";
    public static final String ITEM_EDITOR_LABEL_PAINT = "Paint Item";

    // Item Stamps — a stroke's ray running along its layer, the middle of a cell the stroke's plane passes through,
    // and the seed a speckled tip's density rolls from
    public static final float ITEM_EDITOR_STROKE_PARALLEL_EPSILON = 1e-4f;
    public static final float ITEM_EDITOR_CELL_MIDDLE = 0.5f;
    public static final long ITEM_EDITOR_DENSITY_SEED = 0x1A2B3C4DL;
    public static final long ITEM_EDITOR_DENSITY_SALT = 0x5E6F7081L;

    // Item Bakes — the rig and bone every baked item is skinned to, the pack its maps are kept in, the folder its
    // model takes among the workspace's models, the most texels a sub-voxel takes and the padding round each island,
    // the atlas sides tried, each face's u and v as the item shader lays a block's texels (axis, then sign), the corner
    // orders of a face and of a wall's back, and how a corner's position packs into one key
    public static final String ITEM_BAKE_RIG = "ItemRig";
    public static final String ITEM_BAKE_ROOT_BONE = "root";
    public static final String ITEM_BAKE_PACK = "items/baked";
    public static final String ITEM_BAKE_MODEL_FOLDER = "items/";
    public static final int ITEM_BAKE_TEXELS_PER_CELL = 4;
    public static final int ITEM_BAKE_PADDING = 2;
    public static final int[] ITEM_BAKE_ATLAS_SIZES = { 128, 256, 512 };
    public static final int[] ITEM_BAKE_FACE_U_AXIS = { 0, 2, 0, 2, 0, 0 };
    public static final int[] ITEM_BAKE_FACE_U_SIGN = { 1, -1, -1, 1, 1, 1 };
    public static final int[] ITEM_BAKE_FACE_V_AXIS = { 1, 1, 1, 1, 2, 2 };
    public static final int[] ITEM_BAKE_FACE_V_SIGN = { 1, 1, 1, 1, 1, -1 };
    public static final int[] ITEM_BAKE_FRONT_CORNERS = { 0, 1, 2, 3 };
    public static final int[] ITEM_BAKE_BACK_CORNERS = { 0, 3, 2, 1 };
    public static final int ITEM_BAKE_KEY_SHIFT_X = 42;
    public static final int ITEM_BAKE_KEY_SHIFT_Y = 21;

    // Materials — the material chosen before any is picked
    public static final String MATERIALS_DEFAULT_MATERIAL = CHARACTER_HAIR_DEFAULT_TEXTURE;

    // Material Documents — the workspace folder materials are made in and the folder under Exports their maps are
    // written to, the keys of a material's file, its parameters' labels, ranges, steps and starting values in
    // MaterialParameter order, and a normal map's encoding about its middle
    public static final String MATERIALS_WORKSPACE_DIRECTORY = "Materials";
    public static final String MATERIALS_EXPORT_DIRECTORY = "Materials";
    public static final String MATERIALS_FIELD_BASE = "base";
    public static final String MATERIALS_FIELD_MAPS = "maps";
    public static final String MATERIALS_FIELD_IMAGES = "images";
    public static final String MATERIALS_FIELD_PARAMETERS = "parameters";
    public static final String[] MATERIALS_PARAMETER_KEYS = {
            "red", "green", "blue", "tiling", "normal_strength", "roughness", "metallic" };
    public static final String[] MATERIALS_PARAMETER_LABELS = {
            "Base Color Red", "Base Color Green", "Base Color Blue", "Tiling", "Normal Strength", "Roughness Offset",
            "Metallic Offset" };
    public static final float[] MATERIALS_PARAMETER_MIN = { 0f, 0f, 0f, 0.25f, 0f, -1f, -1f };
    public static final float[] MATERIALS_PARAMETER_MAX = { 1f, 1f, 1f, 8f, 2f, 1f, 1f };
    public static final float[] MATERIALS_PARAMETER_STEP = { 0.01f, 0.01f, 0.01f, 0.25f, 0.05f, 0.05f, 0.05f };
    public static final float[] MATERIALS_PARAMETER_DEFAULT = { 1f, 1f, 1f, 1f, 1f, 0f, 0f };
    public static final int MATERIALS_PARAMETER_RED = 0;
    public static final int MATERIALS_PARAMETER_GREEN = 1;
    public static final int MATERIALS_PARAMETER_BLUE = 2;
    public static final int MATERIALS_PARAMETER_TILING = 3;
    public static final int MATERIALS_PARAMETER_NORMAL_STRENGTH = 4;
    public static final int MATERIALS_PARAMETER_ROUGHNESS = 5;
    public static final int MATERIALS_PARAMETER_METALLIC = 6;
    public static final int MATERIALS_MAP_ALBEDO = 0;
    public static final int MATERIALS_MAP_NORMAL = 1;
    public static final int MATERIALS_MAP_AMBIENT_OCCLUSION = 3;
    public static final int MATERIALS_MAP_METALLIC = 4;
    public static final int MATERIALS_MAP_ROUGHNESS = 5;
    public static final float MATERIALS_NORMAL_MIDDLE = 0.5f;

    // Material Images — the files a map's image may be, the dialog that picks one, the name each map's image goes by
    // as the Material Viewer's texture, and how many edits Undo reaches back
    public static final String[] MATERIALS_IMAGE_PATTERNS = { "*.png" };
    public static final String MATERIALS_IMAGE_DESCRIPTION = "PNG images (*.png)";
    public static final String MATERIALS_DIALOG_CHOOSE_IMAGE = "Choose the Map's Image";
    public static final String MATERIALS_PREVIEW_IMAGE_PREFIX = "material/preview/";
    public static final int MATERIALS_HISTORY_MAX_STEPS = 64;

    // Material Thumbnails — the side of each in texels, the map it is made from (albedo) and how many the Materials
    // tab makes a frame
    public static final int MATERIALS_THUMBNAIL_SIZE = 96;
    public static final int MATERIALS_THUMBNAIL_MAP = 0;
    public static final int MATERIALS_THUMBNAILS_PER_FRAME = 6;

    // Material Rows — the Properties tab's keys, headers and words for the material chosen
    public static final String MATERIALS_PROPERTIES_TITLE = "Material";
    public static final String MATERIALS_KEY_MATERIAL = "material_chosen";
    public static final String MATERIALS_KEY_BASE = "material_base";
    public static final String MATERIALS_KEY_MAP = "material_map";
    public static final String MATERIALS_KEY_PARAMETER = "material_parameter";
    public static final String MATERIALS_KEY_IMAGE = "material_image";
    public static final String MATERIALS_LABEL_IMAGE_PREFIX = "Choose ";
    public static final String MATERIALS_LABEL_IMAGE_SUFFIX = " Image";
    public static final String MATERIALS_LABEL_MATERIAL = "Material";
    public static final String MATERIALS_LABEL_BASE = "Base";
    public static final String MATERIALS_HEADER_MAPS = "Maps";
    public static final String MATERIALS_HEADER_PARAMETERS = "Settings";
    public static final String MATERIALS_TEXT_FROM_BASE = "From Base";
    public static final String MATERIALS_TEXT_NEW_HINT = "File, New makes a material from this one";
    public static final CharacterViewTemplateStruct MATERIALS_ROW_TEMPLATES = new CharacterViewTemplateStruct(
            "editor/Materials/material_header_row",
            "editor/Materials/material_text_row",
            "editor/Materials/material_text_row",
            "editor/Materials/material_number_row",
            "editor/Materials/material_text_row",
            "editor/Materials/material_choice_row",
            "editor/Materials/material_action_row",
            null,
            "material_row_label",
            "material_row_value",
            "material_row_value_button",
            "material_row_decrease",
            "material_row_increase",
            "material_row_track",
            "material_row_knob",
            null,
            null,
            null);

    // Material Files — what New, Open, Save, Revert, Delete, Export and Import report
    public static final String MATERIALS_MESSAGE_CREATED = "Material made: ";
    public static final String MATERIALS_MESSAGE_NOT_READ = "The material could not be read: ";
    public static final String MATERIALS_MESSAGE_SAVED = "Material saved: ";
    public static final String MATERIALS_MESSAGE_NOT_WRITTEN = "The material could not be written: ";
    public static final String MATERIALS_MESSAGE_REVERTED = "Material reverted: ";
    public static final String MATERIALS_MESSAGE_DELETED = "Material deleted: ";
    public static final String MATERIALS_MESSAGE_NOT_DELETED = "The material could not be deleted: ";
    public static final String MATERIALS_MESSAGE_NONE_OPEN = "No material is open: File, New or Open makes one";
    public static final String MATERIALS_MESSAGE_NOT_IMAGE = "Not an image the map can take: ";
    public static final String MATERIALS_MESSAGE_IMAGE = "Image laid: ";
    public static final String MATERIALS_MESSAGE_EXPORT_FIRST =
            "Export the material first; Import takes its exported maps";

    // Info Schemas
    public static final String INFO_SCHEMA_PATH = "schemas";
    public static final String INFO_SCHEMA_BIOMES = "Biomes";
    public static final String INFO_SCHEMA_ITEMS = "Items";
    public static final String INFO_SCHEMA_TREES = "Trees";
    public static final String INFO_SCHEMA_ENTITIES = "Entities";
    public static final String INFO_SCHEMA_RACES = "Races";
    public static final String INFO_SCHEMA_FEATURES = "Features";
    public static final String INFO_SCHEMA_GENDERS = "Genders";
    public static final String INFO_SCHEMA_MATERIALS = "Materials";
    public static final String INFO_SCHEMA_ANIMATIONS = "Animations";
    public static final String INFO_SCHEMA_ANIMATION_TREES = "AnimationTrees";
    public static final String INFO_SCHEMA_RIGS = "Rigs";
    public static final String INFO_SCHEMA_BONE_CONTROLS = "BoneControls";
    public static final String INFO_SCHEMA_WORLDS = "Worlds";
    public static final String INFO_SCHEMA_VEHICLES = "Vehicles";
    public static final String INFO_SCHEMA_SHORTCUTS = "Shortcuts";
    public static final String INFO_SCHEMA_SETTLEMENTS = "Settlements";
    public static final String INFO_SCHEMA_STRUCTURES = "Structures";
    public static final String INFO_SCHEMA_BRUSHES = "Brushes";
    public static final String INFO_SCHEMA_STAMPS = "Stamps";
    public static final String INFO_SCHEMA_TRANSITIONS = "Transitions";
    public static final String INFO_SCHEMA_SOUNDS = "Sounds";
    public static final String INFO_SCHEMA_INSTRUMENTS = "Instruments";
    public static final String INFO_SCHEMA_NOTE_SETS = "NoteSets";
    public static final String INFO_SCHEMA_BLOCKS = "Blocks";
    public static final String INFO_SCHEMA_SHADERS = "Shaders";
    public static final String INFO_SCHEMA_ARCHITECTURES = "Architectures";
    public static final String INFO_SCHEMA_COVERINGS = "Coverings";
    public static final String INFO_SCHEMA_CONTENTS = "Contents";
    public static final String INFO_SCHEMA_FURNISHINGS = "Furnishings";
    public static final String INFO_SCHEMA_NPCS = "Npcs";
    public static final String INFO_SCHEMA_LOOT_TABLES = "LootTables";
    public static final String INFO_ITEM_MESH_FIELD = "mesh";
    public static final String INFO_ITEM_EQUIP_FIELD = "equip";
    public static final String INFO_ITEM_WORN_MESH_FIELD = "mesh";
    public static final String INFO_ITEM_WORN_GENDER_FIELD = "gender";
    public static final String INFO_ITEM_WORN_SIDE_FIELD = "side";
    public static final String INFO_ITEM_WORN_SIDE_NONE = "";
    public static final String INFO_FOLDER_SEPARATOR = "/";
    public static final String INFO_PATH_SEPARATOR = "|";
    public static final String[] INFO_SUMMARY_FIELDS = { "name", "block", "clip", "time" };

    // Info Values — a whole number steps by one and a decimal by this much, kept to the places this scale holds; a
    // number without both ends of its range slides across this many steps centred where it stood when listed; a
    // field's key reads as words split at this mark
    public static final double INFO_FLOAT_STEP = 0.01;
    public static final double INFO_FLOAT_SCALE = 1000.0;
    public static final int INFO_SLIDER_STEPS = 40;
    public static final char INFO_KEY_WORD_SEPARATOR = '_';
    public static final char INFO_LABEL_WORD_SEPARATOR = ' ';

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
    public static final String INFO_VALUE_NONE = "(none)";
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
    public static final String INFO_MESSAGE_DUPLICATED = "Duplicated ";
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
    public static final String INFO_LOCAL_SAME_KEY = "{.}";
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
    public static final String INFO_PROBLEM_ONE_OF = "Must be one of: ";
    public static final String INFO_PROBLEM_INDEX_PREFIX = "Must index this entry's ";
    public static final String INFO_PROBLEM_INDEX_COUNT = ", which hold ";
    public static final String INFO_PROBLEM_ASCENDING_FIRST = "The first must be ";
    public static final String INFO_PROBLEM_ASCENDING = "Must be above the one before";
    public static final String INFO_PROBLEM_RECORD_PREFIX = "Must hold whole records of ";
    public static final String INFO_PROBLEM_RECORD_SLOTS = " values: ";
    public static final String INFO_PROBLEM_SLOT_SEPARATOR = ": ";
    public static final String INFO_PROBLEM_SPAN = "Runs past ";
    public static final String INFO_PROBLEM_LOOP = "Reaches itself: ";
    public static final String INFO_PROBLEM_LOOP_SEPARATOR = " > ";

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
    public static final String ITEM_EDITOR_STATUS_NO_ITEM =
            "No item open. Pick one in the Hierarchy, or New or Open one.";
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
    public static final String ITEM_EDITOR_MESSAGE_NOT_WRITTEN = "Could not write ";
    public static final String ITEM_EDITOR_MESSAGE_NOT_READ = "Could not read ";
    public static final String ITEM_EDITOR_MESSAGE_NOT_DELETED = "Could not delete ";
    public static final String ITEM_EDITOR_MESSAGE_IMPORTED = "Imported ";
    public static final String ITEM_EDITOR_MESSAGE_IMPORT_WORKSPACE =
            "Import brings a workspace item into the game: this item is the game's already";
    public static final String ITEM_EDITOR_MESSAGE_DROP_MISSED = "Drop the material on a part of the item";
    public static final String ITEM_EDITOR_MESSAGE_MATERIAL = "Coloured ";
    public static final String ITEM_EDITOR_MATERIAL_TO = " with ";
    public static final String ITEM_EDITOR_MESSAGE_EXPORTED =
            "Baked for the mesh tools, open in the Mesh menu's tabs: ";
    public static final String ITEM_EDITOR_MESSAGE_BAKE_TOO_LARGE =
            "Too many faces to bake into the largest atlas: merge or remove some";

    // Recorder — media files, video and music alike, live in the player's Assets/Media folder with their songs
    // (EngineSetting.MEDIA_USER_DIRECTORY); what they export goes in a folder beside them, music laid out as the
    // project's assets are so it is dragged in as it lies
    public static final String RECORDER_EXPORT_DIRECTORY = "Export";
    public static final String RECORDER_EXPORT_SONG_DIRECTORY = "songs";
    public static final String RECORDER_EXPORT_SOUND_DIRECTORY = "sounds/music";
    public static final String RECORDER_DEFAULT_NAME_PREFIX = "Recording";
    public static final String RECORDER_TAKE_NAME_PREFIX = "Take ";
    public static final String RECORDER_PLAYER_DISPLAY_NAME = "Player";
    public static final int RECORDER_FIRST_NAME_NUMBER = 1;
    public static final int RECORDER_NAME_MAX_LENGTH = 32;
    public static final int RECORDER_LABEL_MAX_LENGTH = 48;
    public static final String RECORDER_VIDEO_NAME_SEPARATOR = "_";
    public static final String RECORDER_EXTENSION_SEPARATOR = ".";
    public static final int RECORDER_FILE_VERSION = 4;
    public static final int RECORDER_SOUND_TAKE_FIRST_NUMBER = 2;
    public static final float RECORDER_SOUND_JUMP_SECONDS = 0.25f;
    public static final String RECORDER_SOUND_TAKE_NAME_SEPARATOR = " ";

    // Recorder — a sound's volume and pitch are typed as a percentage within these bounds, its length in seconds no
    // shorter than this; a sound is taken hold of this near its bar
    public static final float RECORDER_SOUND_PERCENT = 100f;
    public static final float RECORDER_SOUND_VOLUME_MIN_PERCENT = 1f;
    public static final float RECORDER_SOUND_VOLUME_MAX_PERCENT = 100f;
    public static final float RECORDER_SOUND_PITCH_MIN_PERCENT = 25f;
    public static final float RECORDER_SOUND_PITCH_MAX_PERCENT = 400f;
    public static final float RECORDER_SOUND_MIN_LENGTH_SECONDS = 0.01f;
    public static final float RECORDER_SOUND_GRAB_PIXELS = 4f;
    public static final String RECORDER_SOUND_PERCENT_FORMAT = "%.0f";
    public static final String RECORDER_SOUND_LENGTH_FORMAT = "%.2f";
    public static final int RECORDER_AUDIO_SAMPLE_RATE = 48_000;
    public static final int RECORDER_AUDIO_CHANNELS = 2;
    public static final int RECORDER_AUDIO_SAMPLE_BYTES = Short.BYTES;
    public static final float RECORDER_AUDIO_PAN_EIGHTH_TURN = (float) (Math.PI / 4.0);
    public static final int RECORDER_FILE_VERSION_LAYERS = 1;
    public static final String RECORDER_FILE_CHANNEL_WORLD = "world";
    public static final String RECORDER_FILE_TRANSITION_PREFIX = "transition_";
    public static final String RECORDER_FILE_OWNER_PREFIX = "Recording ";
    public static final String RECORDER_FILE_CLIP_OWNER = " clip ";
    public static final String RECORDER_FILE_LANE_OWNER = " lane ";

    // Recorder — clip sources, as the Assets tab and the clip panel name them; a recording keeps a tempo map and a
    // meter, one tempo throughout until it is given more, its timeline shows at least this many lanes, and a nested
    // clip is opened by two clicks this close
    public static final String RECORDER_SOURCE_LABEL_TAKE = "Take";
    public static final String RECORDER_SOURCE_LABEL_RECORDING = "Nested recording";
    public static final String RECORDER_SOURCE_LABEL_SONG = "Song";
    public static final String RECORDER_SOURCE_LABEL_NOTES = "Notes";
    public static final float RECORDER_DEFAULT_BPM = 120f;
    public static final int RECORDER_SHOWN_LANES = 5;
    public static final long RECORDER_DOUBLE_CLICK_MILLIS = 400L;

    // Recorder Music — the Properties tab's titles for the Recorder's and the Timeline's rows, and the templates they
    // are built from
    public static final String RECORDER_TRANSPORT_PROPERTIES_TITLE = "Music";
    public static final String RECORDER_TIMELINE_PROPERTIES_TITLE = "Mixer";
    public static final String RECORDER_ANIMATION_PROPERTIES_TITLE = "Layer";
    public static final String RECORDER_KEYS_PROPERTIES_TITLE = "Keys";
    public static final CharacterViewTemplateStruct RECORDER_MUSIC_ROW_TEMPLATES = new CharacterViewTemplateStruct(
            "editor/Recorder/music_header_row",
            "editor/Recorder/music_item_row",
            "editor/Recorder/music_item_row_selected",
            "editor/Recorder/music_number_row",
            "editor/Recorder/music_text_row",
            "editor/Recorder/music_choice_row",
            "editor/Recorder/music_action_row",
            null,
            "music_row_label",
            "music_row_value",
            "music_row_value_button",
            "music_row_decrease",
            "music_row_increase",
            "music_row_track",
            "music_row_knob",
            null,
            null,
            null);

    // The Palette tab's rows of takes: a take pressed is picked up to lay on the Timeline's lanes, or picked when let
    // go where it was pressed
    public static final CharacterViewTemplateStruct RECORDER_TAKE_ROW_TEMPLATES = new CharacterViewTemplateStruct(
            "editor/Recorder/music_header_row",
            "editor/Recorder/recorder_take_row",
            "editor/Recorder/recorder_take_row_selected",
            null,
            null,
            null,
            null,
            null,
            "music_row_label",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    // Recorder Music — a media file with no picture is music: it holds note clips laid from the Note Library, each
    // struck this hard and named by its instrument and its note, and turns its clips into MIDI and the game's song and
    // rendered sound
    public static final int RECORDER_QUARTERS_PER_WHOLE = 4;
    public static final float RECORDER_NOTES_SOURCE_SECONDS = Float.MAX_VALUE;
    public static final String RECORDER_STATUS_NOTE_PLACED = "Note laid: ";
    public static final int RECORDER_NOTE_VELOCITY = 100;
    public static final String RECORDER_NOTE_LABEL_SEPARATOR = " ";
    public static final String RECORDER_STATUS_MIDI_EXPORTED = "MIDI exported: ";
    public static final String RECORDER_STATUS_MIDI_EXPORTED_FORMAT =
            "MIDI exported: %s (%d sound clips left out: MIDI holds no sounds)";
    public static final String RECORDER_STATUS_MIDI_FAILED = "MIDI export failed: ";
    public static final String RECORDER_STATUS_RENDER_RUNNING = "A render is already running";
    public static final String RECORDER_STATUS_RENDERING_FORMAT = "Rendering: %d%%";
    public static final float RECORDER_BPM_PRECISION = 1000f;

    // Recorder Music — the Properties rows of a media file's time, key, lane mix and clip, each by its key
    public static final String RECORDER_ROWS_NO_MUSIC = "Open a media file to set its time, key and mix";
    public static final String RECORDER_ROWS_HEADER_TIME = "Time";
    public static final String RECORDER_ROWS_HEADER_KEY = "Key";
    public static final String RECORDER_ROWS_HEADER_LANE_FORMAT = "Lane %d";
    public static final String RECORDER_ROWS_HEADER_CLIP_FORMAT = "Clip: %s";
    public static final String RECORDER_ROW_TEMPO = "tempo";
    public static final String RECORDER_ROW_BEATS_PER_BAR = "beats_per_bar";
    public static final String RECORDER_ROW_BEAT_UNIT = "beat_unit";
    public static final String RECORDER_ROW_KEY_ROOT = "key_root";
    public static final String RECORDER_ROW_KEY_SCALE = "key_scale";
    public static final String RECORDER_ROW_LANE_NAME = "lane_name";
    public static final String RECORDER_ROW_LANE_VOLUME = "lane_volume";
    public static final String RECORDER_ROW_LANE_PAN = "lane_pan";
    public static final String RECORDER_ROW_LANE_MUTED = "lane_muted";
    public static final String RECORDER_ROW_LANE_SOLO = "lane_solo";
    public static final String RECORDER_ROW_CLIP_INSTRUMENT = "clip_instrument";
    public static final String RECORDER_ROW_CLIP_VOLUME = "clip_volume";
    public static final String RECORDER_ROW_CLIP_TRANSPOSE = "clip_transpose";
    public static final String RECORDER_ROW_LABEL_TEMPO = "Tempo";
    public static final String RECORDER_ROW_LABEL_BEATS_PER_BAR = "Beats a bar";
    public static final String RECORDER_ROW_LABEL_BEAT_UNIT = "Beat unit";
    public static final String RECORDER_ROW_LABEL_KEY_ROOT = "Root";
    public static final String RECORDER_ROW_LABEL_KEY_SCALE = "Scale";
    public static final String RECORDER_ROW_LABEL_LANE_NAME = "Name";
    public static final String RECORDER_ROW_LABEL_VOLUME = "Volume";
    public static final String RECORDER_ROW_LABEL_PAN = "Pan";
    public static final String RECORDER_ROW_LABEL_MUTED = "Mute";
    public static final String RECORDER_ROW_LABEL_SOLO = "Solo";
    public static final String RECORDER_ROW_LABEL_INSTRUMENT = "Instrument";
    public static final String RECORDER_ROW_LABEL_TRANSPOSE = "Transpose";
    public static final String RECORDER_ROW_VALUE_NONE = "";
    public static final String RECORDER_ROW_VALUE_ON = "On";
    public static final String RECORDER_ROW_VALUE_OFF = "Off";
    public static final float RECORDER_ROW_MIX_STEP = 0.01f;
    public static final String RECORDER_ROW_ENUM_SEPARATOR = "_";
    public static final String RECORDER_ROW_WORD_SEPARATOR = " ";
    public static final String RECORDER_ROW_KEY_OCTAVE = "4";
    public static final String RECORDER_ROWS_HEADER_EXPORT = "Export";
    public static final String RECORDER_ROWS_NO_EXPORT = "Open a media file to set its export";
    public static final String RECORDER_ROW_LABEL_TIME_RAMP = "Time ramp";
    public static final String RECORDER_ROW_LABEL_FILTER = "Filter";
    public static final String RECORDER_FILTER_NONE = "None";
    public static final String RECORDER_ROW_LABEL_RESOLUTION = "Resolution";
    public static final String RECORDER_ROW_LABEL_FRAME_RATE = "Frame rate";
    public static final String RECORDER_ROW_LABEL_FORMAT = "Format";
    public static final String RECORDER_ROW_LABEL_MOTION_BLUR = "Motion blur";
    public static final String RECORDER_ROW_LABEL_SUPERSAMPLE = "Supersample";
    public static final String RECORDER_ROW_LABEL_RANGE_START = "Range in";
    public static final String RECORDER_ROW_LABEL_RANGE_END = "Range out";
    public static final String RECORDER_ROW_FORMAT_RESOLUTION = "%d x %d";
    public static final String RECORDER_ROW_FORMAT_FRAME_RATE = "%d fps";
    public static final String RECORDER_ROW_FORMAT_MOTION_BLUR = "%d samples";
    public static final String RECORDER_ROW_FORMAT_RAMP = "%.0fs";
    public static final String RECORDER_ROW_FORMAT_SECONDS = "%.2f";
    public static final String RECORDER_ROW_VALUE_RAMP_CUT = "Cut";
    public static final String RECORDER_ROW_VALUE_RANGE_START = "Media start";
    public static final String RECORDER_ROW_VALUE_RANGE_END = "Media end";

    // Recorder Keys — the Properties tab's first rows while the Keys or Keys Graph tab is worked in: the keys
    // selected on the curve picked, moved together by their moment and value and given one mode
    public static final String RECORDER_ROWS_NO_CURVE = "Pick a value on the Keys tab";
    public static final String RECORDER_ROWS_NO_BONE = "Pick a bone on the Keys tab";
    public static final String RECORDER_ROWS_HEADER_VALUES = "Values at the playhead";
    public static final String RECORDER_ROWS_HEADER_ANIMATION_LAYER_FORMAT = "Layer: %s";
    public static final String RECORDER_KEY_CURVE_PREFIX = "curve_";
    public static final String RECORDER_ROW_KEYS_GRAPH = "keys_graph";
    public static final String RECORDER_ROW_LABEL_KEYS_GRAPH = "Graph";
    public static final String RECORDER_ROWS_NO_KEY = "Select keys on the Keys tab or the graph";
    public static final String RECORDER_ROWS_HEADER_CURVE_FORMAT = "%s: %s";
    public static final String RECORDER_ROWS_HEADER_KEY_ONE = "1 key selected";
    public static final String RECORDER_ROWS_HEADER_KEYS_FORMAT = "%d keys selected";
    public static final String RECORDER_ROW_KEYS_TIME = "keys_time";
    public static final String RECORDER_ROW_KEYS_VALUE = "keys_value";
    public static final String RECORDER_ROW_KEYS_MODE = "keys_mode";
    public static final String RECORDER_ROW_LABEL_KEYS_TIME = "Time";
    public static final String RECORDER_ROW_LABEL_KEYS_VALUE = "Value";
    public static final String RECORDER_ROW_LABEL_KEYS_MODE = "Mode";
    public static final float RECORDER_ROW_KEYS_TIME_STEP = 0.01f;
    public static final double RECORDER_ROW_KEYS_VALUE_STEP_SHARE = 0.01;

    // Recorder History — every edit of the open recording kept as the recording stood before it, so it can be undone
    // and redone; steps of one value stepped or slid in a row are one step, as is each gesture of another window,
    // named by its number after the prefix
    public static final int RECORDER_HISTORY_MAX_STEPS = 64;
    public static final String RECORDER_GESTURE_ROW_PREFIX = "gesture/";
    public static final String RECORDER_STATUS_UNDONE = "Undone";
    public static final String RECORDER_STATUS_REDONE = "Redone";
    public static final String RECORDER_STATUS_NOTHING_TO_UNDO = "Nothing to undo";
    public static final String RECORDER_STATUS_NOTHING_TO_REDO = "Nothing to redo";
    public static final String RECORDER_HISTORY_ORIGIN_PREFIX = "the undo history of ";
    public static final int RECORDER_PACK_RECORDED_REVISION = 0;

    // Recorder Render — a media file's music mixed offline past its end for its release, a stretch at a time, into a
    // raw file encoded to a music sound
    public static final String RECORDER_STATUS_FFMPEG_MISSING =
            "Render needs ffmpeg: run .claude/Delivery.bat once to fetch it";
    public static final String RECORDER_STATUS_RENDER_EMPTY =
            "Nothing to render: the media file holds no notes, songs or sounds that play";
    public static final String RECORDER_STATUS_RENDER_FAILED = "Render failed: ";
    public static final String RECORDER_STATUS_RENDERED = "Rendered: ";
    public static final float RECORDER_RENDER_TAIL_SECONDS = 3f;
    public static final String RECORDER_RENDER_RAW_SUFFIX = ".pcm";
    public static final int RECORDER_RENDER_STRETCH_FRAMES = 4096;
    public static final int RECORDER_RENDER_PERCENT_MAX = 100;
    public static final int RECORDER_RENDER_MIXED_PERCENT = 90;
    public static final String RECORDER_RENDER_CATEGORY = "music";

    // ffmpeg — found on the PATH or as Delivery.bat's portable copy, run with Delivery.bat's sound settings
    public static final String FFMPEG_PATH_VARIABLE = "PATH";
    public static final String FFMPEG_LOCAL_APP_DATA_VARIABLE = "LOCALAPPDATA";
    public static final String FFMPEG_PORTABLE_DIRECTORY = "AdventureRPG";
    public static final String[] FFMPEG_FILE_NAMES = { "ffmpeg", "ffmpeg.exe" };
    public static final String FFMPEG_ARG_NO_STDIN = "-nostdin";
    public static final String FFMPEG_ARG_LOG_LEVEL = "-v";
    public static final String FFMPEG_LOG_ERRORS = "error";
    public static final String FFMPEG_ARG_OVERWRITE = "-y";
    public static final String FFMPEG_ARG_FORMAT = "-f";
    public static final String FFMPEG_FORMAT_PCM = "s16le";
    public static final String FFMPEG_ARG_RATE = "-ar";
    public static final String FFMPEG_ARG_CHANNELS = "-ac";
    public static final String FFMPEG_ARG_INPUT = "-i";
    public static final String FFMPEG_ARG_MAP = "-map";
    public static final String FFMPEG_MAP_FIRST_AUDIO = "0:a:0";
    public static final String FFMPEG_ARG_MAP_METADATA = "-map_metadata";
    public static final String FFMPEG_MAP_METADATA_NONE = "-1";
    public static final String FFMPEG_ARG_AUDIO_CODEC = "-c:a";
    public static final String FFMPEG_CODEC_VORBIS = "libvorbis";
    public static final String FFMPEG_ARG_AUDIO_QUALITY = "-q:a";
    public static final String FFMPEG_VORBIS_QUALITY = "5";
    public static final int FFMPEG_SOUND_RATE = 48_000;

    // Recorder — nested playback: a sound or a song is told apart by the path of clips it was reached by, each clip's
    // place added to the path above it, and the songs and notes clips heard at once follow from a fixed pool
    public static final long RECORDER_PATH_ROOT = 0L;
    public static final long RECORDER_PATH_RADIX = 1_000_003L;
    public static final int RECORDER_MUSIC_PLAYBACK_COUNT = 8;

    // Recorder — transitions; clips this near meet at a cut, a transition dropped on the first share of a clip
    // lands on its start and on the rest its end, and the length dialog writes its seconds so
    public static final float RECORDER_ADJOIN_SECONDS = 0.001f;
    public static final float RECORDER_TRANSITION_EDGE_SHARE = 0.5f;
    public static final String RECORDER_TRANSITION_SECONDS_FORMAT = "%.2f";
    public static final String RECORDER_EDGE_LABEL_START = "Start";
    public static final String RECORDER_EDGE_LABEL_END = "End";

    // Recorder — a transition shown in the Dev window: the timeline drawn with its clip and without it, one side a
    // frame, each into its own target, and blended by this material
    public static final int RECORDER_TRANSITION_SIDES = 2;
    public static final int RECORDER_TRANSITION_SIDE_WITH = 0;
    public static final int RECORDER_TRANSITION_SIDE_WITHOUT = 1;
    public static final String RECORDER_TRANSITION_MATERIAL = "editor/RecorderTransitionMaterial";
    public static final String RECORDER_TRANSITION_UNIFORM_FROM = "u_from";
    public static final String RECORDER_TRANSITION_UNIFORM_TO = "u_to";
    public static final String RECORDER_TRANSITION_UNIFORM_MIX_PARAMS = "u_mixParams";
    public static final String RECORDER_TRANSITION_UNIFORM_SIDE_PARAMS = "u_sideParams";
    public static final String RECORDER_TRANSITION_UNIFORM_COLOR = "u_color";

    // Recorder — the transport's menu, the timeline's and the Keys', those of the tools beside them, and the lanes the
    // drags land on and the ruler and graph the wheel zooms, which every Recorder tab shares
    public static final String RECORDER_MENU = "editor/Recorder/Recorder";
    public static final String RECORDER_TIMELINE_MENU = "editor/Timeline/Timeline";
    public static final String RECORDER_KEYS_MENU = "editor/Recorder/RecorderKeys";
    public static final String RECORDER_KEY_GRAPH_MENU = "editor/Recorder/RecorderKeyGraph";
    public static final String SOUND_LIBRARY_MENU = "editor/SoundLibrary/SoundLibrary";
    public static final String NOTE_LIBRARY_MENU = "editor/NoteLibrary/NoteLibrary";
    public static final String SCENE_ASSETS_MENU = "editor/SceneAssets/SceneAssets";
    public static final String TRANSITIONS_MENU = "editor/Transitions/Transitions";
    public static final String FILTERS_MENU = "editor/Filters/Filters";
    public static final String RECORDINGS_MENU = "editor/Recordings/Recordings";
    public static final String SOUND_LIBRARY_STATUS_READY =
            "Pick a sound to sample it, or drag it onto the Timeline";
    public static final String SOUND_LIBRARY_STATUS_SELECTED = "Selected ";
    public static final String SOUND_LIBRARY_STATUS_PLAYING = "Playing ";
    public static final String SOUND_LIBRARY_STATUS_PAUSED = "Paused";
    public static final String SOUND_LIBRARY_STATUS_STOPPED = "Stopped";
    public static final String SOUND_LIBRARY_STATUS_ENDED = "Finished playing";
    public static final String SOUND_LIBRARY_STATUS_NO_SOUND = "Select a sound first";
    public static final String SOUND_LIBRARY_STATUS_SILENT = "No audio device is open, so nothing can play";
    public static final String SOUND_LIBRARY_STATUS_ADDED = "Laid on the Timeline: ";
    public static final String NOTE_LIBRARY_STATUS_READY =
            "Pick an instrument or a note set in the Hierarchy to list its notes";
    public static final String NOTE_LIBRARY_STATUS_SELECTED = "Notes of ";
    public static final String NOTE_LIBRARY_STATUS_PLAYING = "Playing ";
    public static final String NOTE_LIBRARY_STATUS_REFUSED = "The game refuses the instrument: ";
    public static final String NOTE_LIBRARY_STATUS_UNPLAYED = "No instrument plays the note set ";
    public static final float NOTE_LIBRARY_PREVIEW_SECONDS = 0.6f;

    // Scene Assets — how long a tab keeps the scene watched and a Dev window's claim on it lasts unfed, how often the
    // crosshair is cast and the view scanned, the grid of rays a scan casts and how far each reaches, and the words
    // its sections and rows are named by
    public static final float SCENE_ASSETS_WATCH_SECONDS = 0.5f;
    public static final float SCENE_ASSETS_OWNER_SECONDS = 2f;
    public static final float SCENE_ASSETS_PICK_SECONDS = 0.1f;
    public static final float SCENE_ASSETS_SCAN_SECONDS = 1f;
    public static final int SCENE_ASSETS_SCAN_COLUMNS = 32;
    public static final int SCENE_ASSETS_SCAN_ROWS = 18;
    public static final float SCENE_ASSETS_SCAN_DISTANCE = 256f;
    public static final String SCENE_ASSETS_LABEL_POINTED = "Pointed at";
    public static final String SCENE_ASSETS_LABEL_BLOCKS = "Blocks in view";
    public static final String SCENE_ASSETS_LABEL_BIOMES = "Biomes in view";
    public static final String SCENE_ASSETS_LABEL_TEXTURES = "Textures in view";
    public static final String SCENE_ASSETS_LABEL_MATERIALS = "Materials in view";
    public static final String SCENE_ASSETS_LABEL_SHADERS = "Shaders in view";
    public static final String SCENE_ASSETS_ROW_BLOCK = "Block";
    public static final String SCENE_ASSETS_ROW_BIOME = "Biome";
    public static final String SCENE_ASSETS_ROW_FACE = "Face hit";
    public static final String SCENE_ASSETS_ROW_MATERIAL = "Material";
    public static final String SCENE_ASSETS_ROW_SHADER = "Shader";
    public static final String SCENE_ASSETS_ROW_CELL = "Block position";
    public static final String SCENE_ASSETS_TEXT_NOTHING_POINTED = "Nothing under the crosshair";
    public static final String SCENE_ASSETS_TEXT_NO_TEXTURE = "untextured";
    public static final String SCENE_ASSETS_TEXT_EMPTY = "";
    public static final String SCENE_ASSETS_FORMAT_FACE = "%s: %s";
    public static final String SCENE_ASSETS_FORMAT_FACE_TEXTURE = "Texture %s";
    public static final String SCENE_ASSETS_FORMAT_TILE = "%s (%s)";
    public static final String SCENE_ASSETS_FORMAT_CELL = "%d, %d, %d";
    public static final String SCENE_ASSETS_FORMAT_TEXTURE_BLOCK = "%s  |  %s";
    public static final String SCENE_ASSETS_FORMAT_TEXTURE_BLOCKS = "%s  |  %s and %d more";
    public static final String SCENE_ASSETS_FORMAT_SHADER_MATERIALS = "%d materials";
    public static final String SCENE_ASSETS_STATUS_NO_DEV = "no Dev window";
    public static final String SCENE_ASSETS_STATUS_WATCHING = "watching ";
    public static final int RECORDER_ENTRY_CLIP_LANES = 2;
    public static final int RECORDER_ENTRY_RULER = 3;
    public static final int RECORDER_ENTRY_KEY_ROWS = 1;
    public static final int RECORDER_ENTRY_KEY_STRIPS = 2;
    public static final int RECORDER_ENTRY_KEY_GRAPH = 1;
    public static final String RECORDER_ELEMENT_KEY_STRIP = "recorder_key_strip";

    // Recorder — channels
    public static final String RECORDER_CHANNEL_LABEL_CAMERA = "Camera";
    public static final String RECORDER_CHANNEL_LABEL_PLAYER = "Player";
    public static final String RECORDER_CHANNEL_LABEL_CHARACTERS = "Characters";
    public static final String RECORDER_CHANNEL_LABEL_ITEMS = "Items";
    public static final String RECORDER_CHANNEL_LABEL_VEHICLES = "Vehicles";
    public static final String RECORDER_CHANNEL_LABEL_WEATHER = "Weather";
    public static final String RECORDER_CHANNEL_LABEL_TIME = "Time and light";
    public static final String RECORDER_CHANNEL_LABEL_INTERFACE = "Interface";
    public static final String RECORDER_CHANNEL_LABEL_SOUND = "Sound";

    // Recorder — the timeline; clips stack on a fixed number of lanes, the highest playing over the rest, and the
    // timeline runs on past the recording's end so there is room to lay a clip after it
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
    // moves along by a share of the view, the pan button dragged over the lanes or the ruler moves it along under the
    // cursor, and a playhead leaving the view brings it back a share in from the left
    public static final float RECORDER_MIN_VIEW_SECONDS = 0.25f;
    public static final int RECORDER_BUTTON_PAN = Buttons.RIGHT;
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
    public static final int RECORDER_GRAPH_FRAME_SAMPLES = 64;

    // Recorder — curves, as the Keys tab names them; a mast, sail or door is named with its number
    public static final String RECORDER_TRACK_LABEL_CAMERA = "Camera";
    public static final String RECORDER_TRACK_LABEL_WEATHER = "World weather";
    public static final String RECORDER_TRACK_LABEL_CLOCK = "World time";
    public static final String RECORDER_TRACK_LABEL_MORPHS = "Morphs";
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
    public static final String RECORDER_CURVE_LABEL_BONE_ROTATION_X = "Rotation X";
    public static final String RECORDER_CURVE_LABEL_BONE_ROTATION_Y = "Rotation Y";
    public static final String RECORDER_CURVE_LABEL_BONE_ROTATION_Z = "Rotation Z";
    public static final String RECORDER_CURVE_LABEL_BONE_POSITION_X = "Position X";
    public static final String RECORDER_CURVE_LABEL_BONE_POSITION_Y = "Position Y";
    public static final String RECORDER_CURVE_LABEL_BONE_POSITION_Z = "Position Z";
    public static final String RECORDER_CURVE_LABEL_BONE_SCALE_X = "Scale X";
    public static final String RECORDER_CURVE_LABEL_BONE_SCALE_Y = "Scale Y";
    public static final String RECORDER_CURVE_LABEL_BONE_SCALE_Z = "Scale Z";
    public static final String RECORDER_CURVE_LABEL_MORPH_WEIGHT = "Weight";
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
    // rows below, the Sounds layer standing in a place of its own with a row for each sound it names, and a press
    // takes hold of a key or a handle this near it
    public static final String RECORDER_KEY_ROW_SEPARATOR = ":";
    public static final int RECORDER_KEY_ROW_MOVEMENT = -2;
    public static final int RECORDER_KEY_ROW_GESTURES = -3;
    public static final int RECORDER_KEY_ROW_SUMMARY = -4;
    public static final int RECORDER_KEY_TRACK_SOUNDS = -1;
    public static final int RECORDER_KEY_LAYER_NONE = -2;

    // Recorder — the Layers tab; its views, a layer's key naming a track by its place in the take, the Sounds layer,
    // a group by what it holds and a take by its place in the bin, and the words its rows and title show
    public static final String RECORDER_LAYER_KEY_TRACK_PREFIX = "track_";
    public static final String RECORDER_LAYER_KEY_TAKE_PREFIX = "take_";
    public static final String RECORDER_LAYER_KEY_SOUNDS = "sounds";
    public static final String RECORDER_LAYER_KEY_CHARACTERS = "characters";
    public static final String RECORDER_LAYER_KEY_ITEMS = "items";
    public static final String RECORDER_LAYER_KEY_VEHICLES = "vehicles";
    public static final String RECORDER_LAYER_KEY_BONES = "bones";
    public static final String RECORDER_LAYER_LABEL_CHARACTERS = "Characters";
    public static final String RECORDER_LAYER_LABEL_ITEMS = "Items";
    public static final String RECORDER_LAYER_LABEL_VEHICLES = "Vehicles";
    public static final String RECORDER_LAYER_LABEL_BONES = "Bones";
    public static final String RECORDER_LAYER_SOUNDS_FORMAT = "Sounds (%d)";
    public static final String RECORDER_LAYER_TAKE_FORMAT = "%s  (%s, %.1fs)";
    public static final String RECORDER_LAYER_CHANNEL_SEPARATOR = ", ";
    public static final String RECORDER_LAYERS_TITLE_TAKE_FORMAT = "Layers of %s";
    public static final String RECORDER_PALETTE_TAKES_FORMAT = "Takes of %s";
    public static final String RECORDER_LAYERS_TITLE_NO_CLIP = "Recorder: select a clip of a take";
    public static final String RECORDER_LAYERS_TITLE_NO_RECORDING = "Recorder: no media file open";
    public static final String RECORDER_LAYERS_TITLE_ARMED = "Recorded by the next take";
    public static final String RECORDER_LAYERS_TITLE_PREVIEW = "Played in the Preview";
    public static final float RECORDER_KEY_GRAB_PIXELS = 6f;
    public static final float RECORDER_GRAPH_GRAB_PIXELS = 9f;
    public static final double RECORDER_GRAPH_MARGIN_SHARE = 0.1;
    public static final String RECORDER_KEY_STATUS_SEPARATOR = ": ";

    // Recorder — key modes, as the Keys pane names them, in CurveKeyMode order
    public static final String RECORDER_KEY_MODE_LABEL_AUTO = "Auto";
    public static final String RECORDER_KEY_MODE_LABEL_ALIGNED = "Aligned";
    public static final String RECORDER_KEY_MODE_LABEL_FREE = "Free";
    public static final String RECORDER_KEY_MODE_LABEL_LINEAR = "Linear";
    public static final String RECORDER_KEY_MODE_LABEL_CONSTANT = "Hold";
    public static final String[] RECORDER_KEY_MODE_LABELS = {
            RECORDER_KEY_MODE_LABEL_AUTO,
            RECORDER_KEY_MODE_LABEL_ALIGNED,
            RECORDER_KEY_MODE_LABEL_FREE,
            RECORDER_KEY_MODE_LABEL_LINEAR,
            RECORDER_KEY_MODE_LABEL_CONSTANT };

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
    public static final int RECORDER_SOUND_EVENT_BYTES = Float.BYTES * 9 + Integer.BYTES * 2 + Long.BYTES;
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
    public static final String RECORDER_AVI_AUDIO_STREAM = "auds";
    public static final String RECORDER_AVI_AUDIO_CHUNK = "01wb";
    public static final String RECORDER_AVI_AUDIO_STANDARD_INDEX = "ix01";
    public static final int RECORDER_AVI_WAVE_FORMAT_BYTES = 18;
    public static final int RECORDER_AVI_FRAME_RECT_BYTES = 8;
    public static final short RECORDER_AVI_WAVE_FORMAT_PCM = 1;
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
    public static final int RECORDER_AVI_AUDIO_STREAM_LIST_BYTES = 4
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_STREAM_HEADER_BYTES
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_WAVE_FORMAT_BYTES
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_SUPER_INDEX_BYTES;
    public static final int RECORDER_AVI_AUDIO_HEADER_LIST_BYTES = RECORDER_AVI_HEADER_LIST_BYTES
            + RECORDER_AVI_CHUNK_HEADER_BYTES + RECORDER_AVI_AUDIO_STREAM_LIST_BYTES;
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
    public static final int RECORDER_AVI_AUDIO_STREAM_COUNT = 2;
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
    public static final String RECORDER_MESSAGE_NESTED_LOOP = "A nested recording reaches itself and is left silent: ";
    public static final String RECORDER_MESSAGE_NESTED_MISSING = "A nested recording could not be read: ";
    public static final String RECORDER_MESSAGE_SONG_MISSING = "A song laid in a recording could not be loaded: ";
    public static final String RECORDER_MESSAGE_INSTRUMENT_MISSING =
            "A notes clip's instrument could not be loaded: ";

    // Recorder — status text
    public static final String RECORDER_STATUS_READY = "Ready";
    public static final String RECORDER_ANIMATION_BASE_LAYER = "Base";
    public static final String RECORDER_STATUS_ANIMATION_OPENED = "Opened animation ";
    public static final String RECORDER_STATUS_ANIMATION_CLOSED = "Animation deleted: ";
    public static final String RECORDER_STATUS_ANIMATION_SAVED = "Saved animation ";
    public static final String RECORDER_STATUS_ANIMATION_UNREADABLE = "Cannot open animation: ";
    public static final String RECORDER_STATUS_AUTO_KEY_ON =
            "Recording keys: every bone you pose is keyed at the playhead; press Record again to stop";
    public static final String RECORDER_STATUS_AUTO_KEY_OFF = "Stopped recording keys";
    public static final String RECORDER_STATUS_ANIMATION_CONTENT =
            "An animation is content, not media: rename, copy or delete it in the Hierarchy; it never exports";
    public static final String RECORDER_STATUS_LOOP_ON = "Loop on: playing runs round the range or the whole file";
    public static final String RECORDER_STATUS_LOOP_OFF = "Loop off";
    public static final String RECORDER_STATUS_HANDLES_FREED = "Handles freed on ";
    public static final String RECORDER_STATUS_HANDLES_JOINED = "Handles joined on ";
    public static final String RECORDER_STATUS_NO_TAKE = "Pick a take in the Palette tab first";
    public static final String RECORDER_STATUS_RECORDING = "Recording ";
    public static final String RECORDER_STATUS_PLAYING = "Playing";
    public static final String RECORDER_STATUS_PAUSED = "Paused";
    public static final String RECORDER_STATUS_STOPPED_HELD =
            "Stopped: the Preview holds at the playhead; Play picks up here, Stop again hands it back";
    public static final String RECORDER_STATUS_TAKE_SAVED = "Saved take ";
    public static final String RECORDER_STATUS_SUSPENDED = "Export paused with the game: close the pause menu";
    public static final String RECORDER_STATUS_PLACED = "Placed ";
    public static final String RECORDER_STATUS_MOVED = "Moved ";
    public static final String RECORDER_STATUS_CUT = "Cut ";
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
    public static final String RECORDER_STATUS_SOUND_MOVED = "Moved sound ";
    public static final String RECORDER_STATUS_SOUND_REMOVED = "Removed sound ";
    public static final String RECORDER_STATUS_SOUNDS_REMOVED = "Removed every ";
    public static final String RECORDER_STATUS_SOUND_ADDED = "Added sound ";
    public static final String RECORDER_STATUS_SOUND_CHANGED = "Changed sound ";
    public static final String RECORDER_STATUS_SOUND_REPLACED = "Now plays ";
    public static final String RECORDER_STATUS_NO_SOUND = "Click a sound on the Sounds layer first";
    public static final String RECORDER_STATUS_NO_SOUND_LAYER = "This take holds no sound channel to add to";
    public static final String RECORDER_STATUS_NO_LIBRARY_SOUND = "Select a sound in the Sound Library first";
    public static final String RECORDER_STATUS_CLIP_REMOVED = "Removed a clip of ";
    public static final String RECORDER_STATUS_TAKE_DELETED = "Deleted take ";
    public static final String RECORDER_STATUS_NO_CLIP = "Select a clip on the timeline first";
    public static final String RECORDER_STATUS_OUTSIDE_CLIP = "Move the playhead inside the clip first";
    public static final String RECORDER_STATUS_UPGRADED = "Opened and upgraded to takes and clips: ";
    public static final String RECORDER_STATUS_TAKE_TOO_SHORT = "Take too short to keep";
    public static final String RECORDER_STATUS_NOTHING_ARMED = "Arm at least one channel to record";
    public static final String RECORDER_STATUS_NOTHING_RECORDED = "Nothing recorded yet";
    public static final String RECORDER_STATUS_NO_RECORDING_FOR_SOUND = "Open or make a recording to lay sounds on";
    public static final String RECORDER_STATUS_NO_DEV_WINDOW = "Open a Preview tab to record or play";
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
    public static final String RECORDER_STATUS_DUPLICATED = "Copied and opened as ";
    public static final String RECORDER_STATUS_NO_RECORDING = "Open a recording in the Recordings tool first";
    public static final String RECORDER_STATUS_TRANSITION_PLACED = "Laid transition ";
    public static final String RECORDER_STATUS_TRANSITION_CHANGED = "Changed transition ";
    public static final String RECORDER_STATUS_TRANSITION_REMOVED = "Removed transition ";
    public static final String RECORDER_STATUS_FILTER_PLACED = "Laid filter ";
    public static final String RECORDER_STATUS_KEYS_COPIED_FORMAT =
            "Copied %d keys: Paste Keys lays them at the playhead";
    public static final String RECORDER_STATUS_NOTHING_COPIED = "Copy some keys first";
    public static final String RECORDER_STATUS_PASTE_NO_VALUE =
            "This take holds none of the values the keys were copied from";
    public static final String RECORDER_STATUS_KEYS_PASTED = "Pasted keys on ";
    public static final String RECORDER_STATUS_FILTER_REMOVED = "Removed filter ";
    public static final String RECORDER_STATUS_NO_TRANSITION = "Click a transition on a clip first";
    public static final String RECORDER_STATUS_EXPORT_WHILE_RECORDING = "Stop the take before exporting";
    public static final String RECORDER_STATUS_EXPORT_WHILE_PAUSED =
            "Close the pause menu in the Preview tab before exporting";
    public static final String RECORDER_STATUS_EXPORTING = "Exporting ";
    public static final String RECORDER_STATUS_EXPORTED = "Exported ";
    public static final String RECORDER_STATUS_EXPORT_FAILED = "Export failed: ";
    public static final String RECORDER_STATUS_EXPORT_CANCELLED = "Export cancelled";
    public static final String RECORDER_STATUS_LOOP_REFUSED = "Refused: it would nest a recording in itself: ";
    public static final String RECORDER_STATUS_OTHER_WORLD =
            "Refused: a nested recording with a picture must be of this world, not ";
    public static final String RECORDER_STATUS_NESTED = "Nested the selection as ";

    // Recorder — the held shortcuts its drags and wheel read, named in shortcuts/Timeline
    public static final String RECORDER_SHORTCUT_FREE_DRAG = "Timeline/FreeDrag";
    public static final String RECORDER_SHORTCUT_SCROLL_VIEW = "Timeline/ScrollView";
    public static final String RECORDER_SHORTCUT_ZOOM_VALUES = "Timeline/ZoomValues";
    public static final String RECORDER_SHORTCUT_EXTEND_SELECTION = "Timeline/ExtendSelection";
    public static final String RECORDER_SHORTCUT_SUBTRACT_SELECTION = "Timeline/SubtractSelection";

    // Recorder — the keys the Recorder tab's setting rows hand back; channel rows add the channel's key to a prefix,
    // and the rows laying a transition on a clip's edge add the edge's key to theirs
    public static final String RECORDER_KEY_CLIP_CHANNEL_PREFIX = "clip_channel_";
    public static final String RECORDER_KEY_TRANSITION_PREFIX = "transition_on_";
    public static final String RECORDER_KEY_CLIP_MUTE = "clip_mute";
    public static final String RECORDER_KEY_CLIP_RAMP = "clip_ramp";
    public static final String RECORDER_KEY_CLIP_TRIM_START = "clip_trim_start";
    public static final String RECORDER_KEY_CLIP_TRIM_END = "clip_trim_end";
    public static final String RECORDER_KEY_CLIP_SPLIT = "clip_split";
    public static final String RECORDER_KEY_CLIP_STAMP = "clip_stamp";
    public static final String RECORDER_KEY_CLIP_REMOVE = "clip_remove";
    public static final String RECORDER_KEY_CLIP_NEST = "clip_nest";
    public static final String RECORDER_KEY_CLIP_OPEN = "clip_open";
    public static final String RECORDER_KEY_NODE_NEXT = "node_next";
    public static final String RECORDER_KEY_NODE_PREVIOUS = "node_previous";
    public static final String RECORDER_KEY_NODE_REMOVE = "node_remove";
    public static final String RECORDER_KEY_KEY_ADD = "key_add";
    public static final String RECORDER_KEY_KEY_DELETE = "key_delete";
    public static final String RECORDER_KEY_KEY_MODE = "key_mode";
    public static final String RECORDER_KEY_KEY_VALUE = "key_value";
    public static final String RECORDER_KEY_CURVE_REFIT = "curve_refit";
    public static final String RECORDER_KEY_TAKE_REFIT = "take_refit";
    public static final String RECORDER_KEY_SOUND_VOLUME = "sound_volume";
    public static final String RECORDER_KEY_SOUND_PITCH = "sound_pitch";
    public static final String RECORDER_KEY_SOUND_LENGTH = "sound_length";
    public static final String RECORDER_KEY_SOUND_REPLACE = "sound_replace";
    public static final String RECORDER_KEY_SOUND_COPY = "sound_copy";
    public static final String RECORDER_KEY_SOUND_ADD = "sound_add";
    public static final String RECORDER_KEY_SOUND_SEEK = "sound_seek";
    public static final String RECORDER_KEY_SOUND_REMOVE = "sound_remove";
    public static final String RECORDER_KEY_SOUND_REMOVE_ALL = "sound_remove_all";
    public static final String RECORDER_KEY_TRANSITION_LENGTH = "transition_length";
    public static final String RECORDER_KEY_TRANSITION_REPLACE = "transition_replace";
    public static final String RECORDER_KEY_TRANSITION_REMOVE = "transition_remove";
    public static final String RECORDER_KEY_FILTER_APPLY = "filter_apply";
    public static final String RECORDER_KEY_KEY_COPY = "key_copy";
    public static final String RECORDER_KEY_KEY_PASTE = "key_paste";
    public static final String RECORDER_KEY_FILTER_REMOVE = "filter_remove";
    public static final String RECORDER_KEY_CLIP_FILTER = "clip_filter";
    public static final String RECORDER_KEY_RESOLUTION = "resolution";
    public static final String RECORDER_KEY_FRAME_RATE = "frame_rate";
    public static final String RECORDER_KEY_FORMAT = "format";
    public static final String RECORDER_KEY_MOTION_BLUR = "motion_blur";
    public static final String RECORDER_KEY_SUPERSAMPLE = "supersample";
    public static final String RECORDER_KEY_RANGE_START = "range_start";
    public static final String RECORDER_KEY_RANGE_END = "range_end";
    public static final String RECORDER_KEY_EXPORT_MIDI = "export_midi";
    public static final String RECORDER_KEY_TAKE_PLACE = "take_place";
    public static final String RECORDER_KEY_TAKE_RENAME = "take_rename";
    public static final String RECORDER_KEY_TAKE_DELETE = "take_delete";
    public static final String RECORDER_KEY_FRAME_KEYS = "frame_keys";

    // Commands — Recorder
    public static final String COMMAND_RECORD = "record";
    public static final String COMMAND_PLAY = "play";
    public static final String COMMAND_STOP = "stop";

    // Tabs
    public static final float DIVIDER_HIT_TOLERANCE = 6f;
    public static final float DOCK_PANEL_MIN_SIZE = 96f;
    public static final String ELEMENT_TAB_CANVAS = "tab_canvas";
    public static final String ELEMENT_TAB_OUTLINE = "tab_outline";
    public static final String ELEMENT_TAB_STRIP_ACCENT = "tab_strip_accent";
    public static final String ELEMENT_TAB_STRIP_CLOSE = "tab_strip_close";
    public static final String ELEMENT_TAB_STRIP_LABEL = "tab_strip_label";
    public static final String ELEMENT_TAB_TOOLBAR = "tab_toolbar";
    public static final String MENU_TAB_GHOST = "editor/TabFrame/TabGhost";
    public static final String MENU_TAB_SHELL = "editor/TabFrame/TabFrame";
    public static final String MENU_TAB_STRIP_ENTRY = "editor/TabFrame/tab_strip_entry";
    public static final String MENU_TAB_STRIP_ROW = "editor/TabFrame/tab_strip_row";
    public static final float RATIO_DEFAULT = 0.5f;
    public static final float RATIO_MAX = 0.9f;
    public static final float RATIO_MIN = 0.1f;
    public static final float TAB_DRAG_EDGE_FRACTION = 0.25f;
    public static final int TAB_DRAG_PREVIEW_H = 144;
    public static final int TAB_DRAG_PREVIEW_W = 256;
    public static final float TAB_DRAG_THRESHOLD = 6f;
    public static final int TAB_ENTRY_STRIP = 0;
    public static final float TAB_HEADER_ACTIVE_ALPHA = 0.24f;
    public static final float TAB_HEADER_HOVER_ALPHA = 0.10f;
    public static final float TAB_STRIP_ACTIVE_ALPHA = 0.32f;

    // Tabs — the strip: each entry its whole title's width (the label's, plus the chrome beside it, the template's
    // label inset) while its row has room, the widest shrunk first to no narrower than their whole title at the
    // smallest readable size, then wrapped onto another row of the header (StripLayoutUtility); the margins are the
    // header's and the canvas's insets within the tab's chrome
    public static final float TAB_STRIP_ENTRY_CHROME_PIXELS = 42f;
    public static final float TAB_STRIP_ROW_HEIGHT_PIXELS = 26f;
    public static final float TAB_STRIP_SPACING_PIXELS = 2f;
    public static final float TAB_STRIP_MARGIN_PIXELS = 12f;
    public static final float TAB_HEADER_MARGIN_PIXELS = 4f;
    public static final float TAB_HEADER_PADDING_PIXELS = 4f;
    public static final float TAB_CANVAS_MARGIN_PIXELS = 8f;
    public static final float TAB_CANVAS_GAP_PIXELS = 6f;
    public static final String TAB_TITLE_PREVIEW = "Preview";
}
