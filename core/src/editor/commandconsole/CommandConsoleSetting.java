package editor.commandconsole;

public class CommandConsoleSetting {

    /*
     * Constants used only by CommandConsoleContext — its menu, command tree
     * templates, tile grids and their icons, chunk fields, and command line.
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

    // Command Line
    public static final int COMMAND_MAX_LENGTH = 256;
    public static final String COMMAND_CARET = "|";
    public static final float COMMAND_CARET_BLINK_SECONDS = 0.5f;
}
