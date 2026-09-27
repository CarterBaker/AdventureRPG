package editor.commandconsole;

public class CommandConsoleSetting {

    /*
     * Constants used only by CommandConsoleContext — its menu, command tree
     * templates, item grid, and command line.
     */

    // Menus
    public static final String MENU_COMMAND_CONSOLE = "editor/CommandConsole/CommandConsole";
    public static final String MENU_COMMAND_GROUP = "editor/CommandConsole/command_group";
    public static final String MENU_COMMAND_ENTRY = "editor/CommandConsole/command_entry";
    public static final String MENU_ITEM_HEADER = "editor/CommandConsole/command_item_header";
    public static final String MENU_ITEM_GRID = "editor/CommandConsole/command_item_grid";
    public static final String MENU_ITEM_ROW = "editor/CommandConsole/command_item_row";
    public static final String MENU_ITEM_TILE = "editor/CommandConsole/command_item_tile";

    // Entry Points
    public static final int ENTRY_COMMAND_LINE = 0;
    public static final int ENTRY_COMMAND_TREE = 1;

    // Command Tree
    public static final String ELEMENT_GROUP_MARKER = "command_group_marker";
    public static final String ELEMENT_GROUP_LABEL = "command_group_label";
    public static final String ELEMENT_COMMAND_LABEL = "command_entry_label";

    // Item Grid
    public static final String ELEMENT_ITEM_HEADER_LABEL = "command_item_header_label";
    public static final String ELEMENT_ITEM_TILE_ICON = "command_item_tile_icon";
    public static final String ELEMENT_ITEM_TILE_LABEL = "command_item_tile_label";
    public static final int ITEM_GRID_MIN_COLUMNS = 1;
    public static final int DEPTH_ITEM_ICON = 0;

    // Command Line
    public static final int COMMAND_MAX_LENGTH = 256;
    public static final String COMMAND_CARET = "|";
    public static final float COMMAND_CARET_BLINK_SECONDS = 0.5f;
}
