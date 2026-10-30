package editor.runtime.commandconsole;

public class CommandConsoleSetting {

    /*
     * Constants used only by CommandConsoleContext — its menu, command tree
     * templates, tile grids and their icons, chunk fields, time dials, and
     * command line.
     */

    // Menus
    public static final String MENU_COMMAND_CONSOLE = "editor/CommandConsole/CommandConsole";
    public static final String MENU_COMMAND_GROUP = "editor/CommandConsole/command_group";
    public static final String MENU_COMMAND_ENTRY = "editor/CommandConsole/command_entry";
    public static final String MENU_TILE_HEADER = "editor/CommandConsole/command_tile_header";
    public static final String MENU_TILE_CATEGORY = "editor/CommandConsole/command_tile_category";
    public static final String MENU_TILE_GRID = "editor/CommandConsole/command_tile_grid";
    public static final String MENU_TILE_ROW = "editor/CommandConsole/command_tile_row";
    public static final String MENU_TILE = "editor/CommandConsole/command_tile";
    public static final String MENU_CHUNK_ENTRY = "editor/CommandConsole/command_chunk_entry";
    public static final String MENU_DIAL_HEADER = "editor/CommandConsole/command_dial_header";
    public static final String MENU_DIAL_ROW = "editor/CommandConsole/command_dial_row";
    public static final String MENU_DIAL = "editor/CommandConsole/command_dial";

    // Entry Points
    public static final int ENTRY_COMMAND_LINE = 0;
    public static final int ENTRY_COMMAND_TREE = 1;

    // Command Tree
    public static final String ELEMENT_GROUP_MARKER = "command_group_marker";
    public static final String ELEMENT_GROUP_LABEL = "command_group_label";
    public static final String ELEMENT_COMMAND_LABEL = "command_entry_label";
    public static final String CATEGORY_KEY_SEPARATOR = ":";

    // Tile Grid
    public static final String ELEMENT_TILE_HEADER_LABEL = "command_tile_header_label";
    public static final String ELEMENT_TILE_CATEGORY_MARKER = "command_tile_category_marker";
    public static final String ELEMENT_TILE_CATEGORY_LABEL = "command_tile_category_label";
    public static final String ELEMENT_TILE_ICON = "command_tile_icon";
    public static final String ELEMENT_TILE_LABEL = "command_tile_label";
    public static final String TILE_CATEGORY_BLOCKS = "Blocks";
    public static final int TILE_GRID_MIN_COLUMNS = 1;

    // Tile Icons
    public static final String MATERIAL_TILE_VEHICLE = "items/InventoryVehicleMaterial";
    public static final int DEPTH_TILE_ICON = 0;

    // Chunk Fields
    public static final String ELEMENT_CHUNK_LABEL = "command_chunk_label";
    public static final String ELEMENT_CHUNK_FIELD_X = "command_chunk_field_x";
    public static final String ELEMENT_CHUNK_FIELD_Y = "command_chunk_field_y";
    public static final String ELEMENT_CHUNK_FIELD_X_TEXT = "command_chunk_field_x_text";
    public static final String ELEMENT_CHUNK_FIELD_Y_TEXT = "command_chunk_field_y_text";
    public static final String ELEMENT_CHUNK_GO = "command_chunk_go";
    public static final String CHUNK_FIELD_KEY_SEPARATOR = ":";
    public static final String CHUNK_FIELD_CHARACTERS = "-0123456789";
    public static final int CHUNK_FIELD_COUNT = 2;
    public static final int CHUNK_FIELD_MAX_LENGTH = 11;

    // Dials
    public static final String ELEMENT_DIAL_HEADER_LABEL = "command_dial_header_label";
    public static final String ELEMENT_DIAL_LABEL = "command_dial_label";
    public static final String ELEMENT_DIAL_FACE = "command_dial_face";
    public static final String ELEMENT_DIAL_NEEDLE = "command_dial_needle";
    public static final String ELEMENT_DIAL_VALUE = "command_dial_value";
    public static final String ELEMENT_DIAL_DECREASE = "command_dial_decrease";
    public static final String ELEMENT_DIAL_INCREASE = "command_dial_increase";
    public static final int DIAL_STEP = 1;
    public static final int DIAL_OPEN_STEPS_PER_TURN = 12;
    public static final float DIAL_DEAD_ZONE = 0.2f;
    public static final float DIAL_FULL_TURN_DEGREES = 360f;
    public static final float DIAL_HALF_TURN_DEGREES = 180f;

    // Command Line
    public static final int COMMAND_MAX_LENGTH = 256;
    public static final int COMMAND_HISTORY_LIMIT = 64;
    public static final String COMMAND_CARET = "|";
    public static final float COMMAND_CARET_BLINK_SECONDS = 0.5f;
}
