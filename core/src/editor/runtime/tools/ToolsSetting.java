package editor.runtime.tools;

public class ToolsSetting {

    /*
     * Constants used only by ToolsContext — its menu, its tool row templates
     * and the entry points each mode's tools and the brush size are shown in.
     * The tools are the editor's.
     */

    // Menus
    public static final String MENU_TOOLS = "editor/Tools/Tools";
    public static final String TEMPLATE_TOOL_ROW = "editor/Tools/tools_tool_row";
    public static final String TEMPLATE_TOOL_ROW_SELECTED = "editor/Tools/tools_tool_row_selected";
    public static final int ENTRY_RADIUS = 0;

    // Entry Points — each mode's tools, indexed by the mode's ordinal
    public static final int[] ENTRY_MODE_TOOLS = { 1, 2 };

    // Elements
    public static final String ELEMENT_TOOL_LABEL = "tools_tool_label";
}
