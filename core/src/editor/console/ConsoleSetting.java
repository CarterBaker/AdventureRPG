package editor.console;

public class ConsoleSetting {

    /*
     * Constants used only by ConsoleContext — its menu, line templates, and
     * how many log lines it keeps on screen.
     */

    // Menus
    public static final String MENU_CONSOLE = "editor/Console/Console";
    public static final String MENU_LINE = "editor/Console/console_line";
    public static final String MENU_LINE_ERROR = "editor/Console/console_line_error";

    // Entry Points
    public static final int ENTRY_LINES = 0;

    // Lines
    public static final int MAX_LINES = 200;
    public static final String TAB_CHARACTER = "\t";
    public static final String TAB_REPLACEMENT = "    ";
}
