package editor.runtime.tools;

public class ToolsSetting {

    /*
     * Constants used only by ToolsContext — its menu, its tool, option and
     * brush row templates and their elements, and the entry points the
     * canvas title, the tools, the tool's title and its options are shown
     * in. The tools are the editor's.
     */

    // Menus
    public static final String MENU_TOOLS = "editor/Tools/Tools";
    public static final String TEMPLATE_TOOL_ROW = "editor/Tools/tools_tool_row";
    public static final String TEMPLATE_TOOL_ROW_SELECTED = "editor/Tools/tools_tool_row_selected";
    public static final String TEMPLATE_OPTION_ROW = "editor/Tools/tools_option_row";
    public static final String TEMPLATE_BRUSH_ROW = "editor/Tools/tools_brush_row";
    public static final int ENTRY_CANVAS_TITLE = 0;
    public static final int ENTRY_TOOLS = 1;
    public static final int ENTRY_TOOL_TITLE = 2;
    public static final int ENTRY_OPTIONS = 3;

    // Elements
    public static final String ELEMENT_TOOL_LABEL = "tools_tool_label";
    public static final String ELEMENT_OPTION_LABEL = "tools_option_label";
    public static final String ELEMENT_OPTION_VALUE = "tools_option_value";
    public static final String ELEMENT_OPTION_LOWER = "tools_option_lower";
    public static final String ELEMENT_OPTION_RAISE = "tools_option_raise";
    public static final String ELEMENT_BRUSH_LABEL = "tools_brush_label";

    // Text
    public static final String TITLE_NO_CANVAS = "No tools for this window";
    public static final String TITLE_OPTIONS_SUFFIX = " Options";
    public static final String TITLE_NO_OPTIONS = "Tool Options";
}
