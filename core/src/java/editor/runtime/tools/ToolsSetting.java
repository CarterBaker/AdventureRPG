package editor.runtime.tools;

public class ToolsSetting {

    /*
     * Constants used only by ToolsContext — its menu, its tool, action group,
     * action and option row templates and their elements, the number row's
     * slider, the switch row's fill and the choice row's drop-down rows among
     * them, the switch's colors, and the entry points the canvas title, the tools and
     * actions, the tool's title and its options are shown in. The tools are
     * the editor's.
     */

    // Menus
    public static final String MENU_TOOLS = "editor/Tools/Tools";
    public static final String TEMPLATE_TOOL_ROW = "editor/Tools/tools_tool_row";
    public static final String TEMPLATE_TOOL_ROW_SELECTED = "editor/Tools/tools_tool_row_selected";
    public static final String TEMPLATE_ACTION_GROUP_ROW = "editor/Tools/tools_action_group_row";
    public static final String TEMPLATE_ACTION_ROW = "editor/Tools/tools_action_row";
    public static final String[] TEMPLATE_OPTION_ROWS = {
            "editor/Tools/tools_option_row",
            "editor/Tools/tools_option_switch_row",
            "editor/Tools/tools_option_choice_row" };
    public static final String TEMPLATE_CHOICE_ROW = "editor/Tools/tools_choice_row";
    public static final String TEMPLATE_CHOICE_ROW_SELECTED = "editor/Tools/tools_choice_row_selected";
    public static final int ENTRY_CANVAS_TITLE = 0;
    public static final int ENTRY_TOOLS = 1;
    public static final int ENTRY_TOOL_TITLE = 2;
    public static final int ENTRY_OPTIONS = 3;

    // Elements
    public static final String ELEMENT_TOOL_LABEL = "tools_tool_label";
    public static final String ELEMENT_ACTION_GROUP_LABEL = "tools_action_group_label";
    public static final String ELEMENT_ACTION_LABEL = "tools_action_label";
    public static final String ELEMENT_OPTION_LABEL = "tools_option_label";
    public static final String ELEMENT_OPTION_VALUE = "tools_option_value";
    public static final String ELEMENT_OPTION_LOWER = "tools_option_lower";
    public static final String ELEMENT_OPTION_RAISE = "tools_option_raise";
    public static final String ELEMENT_OPTION_TRACK = "tools_option_track";
    public static final String ELEMENT_OPTION_KNOB = "tools_option_knob";
    public static final String ELEMENT_OPTION_VALUE_BUTTON = "tools_option_value_button";
    public static final String ELEMENT_OPTION_SWITCH_FILL = "tools_option_switch_fill";
    public static final String ELEMENT_CHOICE_LABEL = "tools_choice_label";

    // Switch
    public static final float SWITCH_ON_ALPHA = 1f;
    public static final float SWITCH_OFF_ALPHA = 0f;

    // Text
    public static final String TITLE_NO_CANVAS = "No tools for this window";
    public static final String TITLE_OPTIONS_SUFFIX = " Options";
    public static final String TITLE_NO_OPTIONS = "Tool Options";
}
