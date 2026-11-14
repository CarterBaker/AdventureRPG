package editor.runtime.settlementplanner;

public class SettlementPlannerSetting {

    /*
     * Constants used only by SettlementPlannerContext — its toolbar menu with
     * the catalogue, architecture and placed lists and the labels beside
     * them. The design and its placements are the editor's.
     */

    // Menus
    public static final String MENU_TOOLBAR = "editor/SettlementPlanner/Toolbar";
    public static final String TEMPLATE_ENTRY_ROW = "editor/SettlementPlanner/settlement_planner_entry_row";
    public static final String TEMPLATE_ENTRY_ROW_SELECTED =
            "editor/SettlementPlanner/settlement_planner_entry_row_selected";
    public static final String TEMPLATE_ARCHITECTURE_ROW =
            "editor/SettlementPlanner/settlement_planner_architecture_row";
    public static final String TEMPLATE_ARCHITECTURE_ROW_SELECTED =
            "editor/SettlementPlanner/settlement_planner_architecture_row_selected";
    public static final String TEMPLATE_PLACED_ROW = "editor/SettlementPlanner/settlement_planner_placed_row";
    public static final String TEMPLATE_PLACED_ROW_SELECTED =
            "editor/SettlementPlanner/settlement_planner_placed_row_selected";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_ENTRIES = 1;
    public static final int ENTRY_ARCHITECTURES = 2;
    public static final int ENTRY_DESIGN = 3;
    public static final int ENTRY_PLACED = 4;
    public static final int ENTRY_EDITED = 5;

    // Elements
    public static final String ELEMENT_ENTRY_LABEL = "settlement_planner_entry_label";
    public static final String ELEMENT_ENTRY_SWATCH = "settlement_planner_entry_swatch";
    public static final String ELEMENT_ARCHITECTURE_LABEL = "settlement_planner_architecture_label";
    public static final String ELEMENT_PLACED_LABEL = "settlement_planner_placed_label";
    public static final String ELEMENT_PLACED_SWATCH = "settlement_planner_placed_swatch";
}
