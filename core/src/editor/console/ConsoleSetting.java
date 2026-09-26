package editor.console;

public class ConsoleSetting {

    /*
     * Constants used only by ConsoleContext — its menu, line templates, how
     * many log lines it keeps on screen, and its command line.
     */

    // Menus
    public static final String MENU_CONSOLE = "editor/Console/Console";
    public static final String MENU_LINE = "editor/Console/console_line";
    public static final String MENU_LINE_ERROR = "editor/Console/console_line_error";

    // Entry Points
    public static final int ENTRY_LINES = 0;
    public static final int ENTRY_COMMAND = 1;

    // Lines
    public static final int MAX_LINES = 200;
    public static final String TAB_CHARACTER = "\t";
    public static final String TAB_REPLACEMENT = "    ";

    // Command Line
    public static final int COMMAND_MAX_LENGTH = 256;
    public static final String COMMAND_CARET = "|";
    public static final float COMMAND_CARET_BLINK_SECONDS = 0.5f;
}
