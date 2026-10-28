package editor.runtime.tools;

public class ToolsSetting {

    /*
     * Constants used only by ToolsContext — its menu, its tool row templates
     * and the entry points the canvas title, the tools, the brush size and
     * the brush preset are shown in. The tools are the editor's.
     */

    // Menus
    public static final String MENU_TOOLS = "editor/Tools/Tools";
    public static final String TEMPLATE_TOOL_ROW = "editor/Tools/tools_tool_row";
    public static final String TEMPLATE_TOOL_ROW_SELECTED = "editor/Tools/tools_tool_row_selected";
    public static final int ENTRY_TITLE = 0;
    public static final int ENTRY_TOOLS = 1;
    public static final int ENTRY_SIZE = 2;
    public static final int ENTRY_BRUSH = 3;

    // Elements
    public static final String ELEMENT_TOOL_LABEL = "tools_tool_label";

    // Text
    public static final String TITLE_NO_CANVAS = "No tools for this window";
    public static final String SIZE_PREFIX = "Size ";
    public static final String NO_VALUE = "-";
}
