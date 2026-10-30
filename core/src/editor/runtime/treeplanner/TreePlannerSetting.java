package editor.runtime.treeplanner;

public class TreePlannerSetting {

    /*
     * Constants used only by TreePlannerContext — its menu, its catalogue's
     * tree row and field row templates, the entry points its catalogue, its
     * fields, its graph nodes and its status line are shown in, and how a
     * node is shaded and summed up. The planner itself is the editor's.
     */

    // Menus
    public static final String MENU_PLANNER = "editor/TreePlanner/Planner";
    public static final String TEMPLATE_TREE_ROW = "editor/TreePlanner/tree_planner_tree_row";
    public static final String TEMPLATE_TREE_ROW_SELECTED = "editor/TreePlanner/tree_planner_tree_row_selected";
    public static final String TEMPLATE_PARAMETER_ROW = "editor/TreePlanner/tree_planner_parameter_row";
    public static final int ENTRY_CATALOGUE = 0;
    public static final int ENTRY_PARAMETERS = 1;
    public static final int ENTRY_PROPERTIES_TITLE = 2;
    public static final int ENTRY_FIRST_NODE = 3;
    public static final int ENTRY_STATUS = 10;

    // Elements
    public static final String ELEMENT_TREE_LABEL = "tree_planner_tree_label";
    public static final String ELEMENT_NODE_SUMMARY = "tree_planner_node_summary";
    public static final String ELEMENT_PARAMETER_LABEL = "tree_planner_parameter_label";
    public static final String ELEMENT_PARAMETER_VALUE = "tree_planner_parameter_value";
    public static final String ELEMENT_PARAMETER_LOWER = "tree_planner_parameter_lower";
    public static final String ELEMENT_PARAMETER_RAISE = "tree_planner_parameter_raise";

    // Graph — the open node's shade, and how many of a node's fields its summary names
    public static final float NODE_OPEN_ALPHA = 0.45f;
    public static final float NODE_ALPHA = 1f;
    public static final int NODE_SUMMARY_PARAMETERS = 2;

    // Steps
    public static final int STEP_DOWN = -1;
    public static final int STEP_UP = 1;

    // Text
    public static final String NODE_SUMMARY_SEPARATOR = "  ·  ";
    public static final String FIELDS_TITLE_SUFFIX = " Fields";
}
