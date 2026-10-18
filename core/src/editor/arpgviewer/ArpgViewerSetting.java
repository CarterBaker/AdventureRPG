package editor.arpgviewer;

public class ArpgViewerSetting {

    /*
     * Constants used only by ArpgViewerContext — its menu, line templates, the
     * entry points its title and lines are filled through, and its messages.
     */

    // Menus
    public static final String MENU_ARPG_VIEWER = "editor/ArpgViewer/ArpgViewer";
    public static final String MENU_LINE = "editor/ArpgViewer/arpg_viewer_line";
    public static final String MENU_LINE_ERROR = "editor/ArpgViewer/arpg_viewer_line_error";

    // Entry Points
    public static final int ENTRY_LINES = 0;
    public static final int ENTRY_TITLE = 1;

    // Lines
    public static final String LINE_SEPARATOR = "\n";

    // Messages
    public static final String TITLE_NO_SELECTION = "ARPG Viewer";
    public static final String MESSAGE_NO_SELECTION = "Select a file in the Hierarchy to view its decoded contents.";
    public static final String MESSAGE_NOT_ON_DISK = "This file has not been saved to disk yet.";
}
