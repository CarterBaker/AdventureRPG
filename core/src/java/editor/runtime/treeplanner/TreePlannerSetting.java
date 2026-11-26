package editor.runtime.treeplanner;

import editor.runtime.characterview.panel.CharacterViewTemplateStruct;

public class TreePlannerSetting {

    /*
     * Constants used only by TreePlannerContext — the menu its graph and
     * status line are shown in, how a node is shaded and summed up, and the
     * row templates the Palette tab lists its trees with and the Properties
     * tab its node's fields with.
     */

    // Menus
    public static final String MENU_PLANNER = "editor/TreePlanner/Planner";
    public static final int ENTRY_FIRST_NODE = 0;
    public static final int ENTRY_STATUS = 7;

    // Elements
    public static final String ELEMENT_NODE_SUMMARY = "tree_planner_node_summary";

    // Graph — the open node's shade, and how many of a node's fields its summary names
    public static final float NODE_OPEN_ALPHA = 0.45f;
    public static final float NODE_ALPHA = 1f;
    public static final int NODE_SUMMARY_PARAMETERS = 2;

    // Rows — the templates of the planner's menu every tree tab's Palette and Properties rows are drawn from
    public static final String TEMPLATE_HEADER_ROW = "editor/TreePlanner/tree_planner_header_row";
    public static final String TEMPLATE_ITEM_ROW = "editor/TreePlanner/tree_planner_item_row";
    public static final String TEMPLATE_ITEM_ROW_SELECTED = "editor/TreePlanner/tree_planner_item_row_selected";
    public static final String TEMPLATE_NUMBER_ROW = "editor/TreePlanner/tree_planner_number_row";
    public static final String TEMPLATE_CHOICE_ROW = "editor/TreePlanner/tree_planner_choice_row";
    public static final String ELEMENT_ROW_LABEL = "tree_planner_row_label";
    public static final String ELEMENT_ROW_VALUE = "tree_planner_row_value";
    public static final String ELEMENT_ROW_VALUE_BUTTON = "tree_planner_row_value_button";
    public static final String ELEMENT_ROW_DECREASE = "tree_planner_row_decrease";
    public static final String ELEMENT_ROW_INCREASE = "tree_planner_row_increase";
    public static final String ELEMENT_ROW_TRACK = "tree_planner_row_track";
    public static final String ELEMENT_ROW_KNOB = "tree_planner_row_knob";
    public static final CharacterViewTemplateStruct ROW_TEMPLATES = new CharacterViewTemplateStruct(
            TEMPLATE_HEADER_ROW,
            TEMPLATE_ITEM_ROW,
            TEMPLATE_ITEM_ROW_SELECTED,
            TEMPLATE_NUMBER_ROW,
            null,
            TEMPLATE_CHOICE_ROW,
            null,
            null,
            ELEMENT_ROW_LABEL,
            ELEMENT_ROW_VALUE,
            ELEMENT_ROW_VALUE_BUTTON,
            ELEMENT_ROW_DECREASE,
            ELEMENT_ROW_INCREASE,
            ELEMENT_ROW_TRACK,
            ELEMENT_ROW_KNOB,
            null,
            null,
            null);

    // Properties — the title the Properties tab gives the node's fields
    public static final String PROPERTIES_TITLE = "Tree Planner";

    // Text
    public static final String NODE_SUMMARY_SEPARATOR = "  ·  ";
    public static final String STATUS_NONE = "No tree selected — pick one in the Palette tab or the Hierarchy";
    public static final String STATUS_SEPARATOR = "  ·  ";
    public static final String STATUS_AGE = "Age ";
    public static final String STATUS_SEED = "Seed ";
    public static final String STATUS_PERCENT_SIGN = "%";
    public static final float STATUS_PERCENT = 100f;
}
