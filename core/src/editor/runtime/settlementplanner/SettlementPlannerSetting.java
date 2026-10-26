package editor.runtime.settlementplanner;

public class SettlementPlannerSetting {

    /*
     * Constants used only by SettlementPlannerContext — how its map frames
     * the design's site, and its toolbar menu with the catalogue and
     * architecture lists. The design, its preview and the map are the
     * editor's and the engine's.
     */

    // Frame — the view spans this many times the design's radius across its narrower side
    public static final double FRAME_SPAN_RADII = 5.0;
    public static final int FRAME_MIN_RADIUS_BLOCKS = 16;

    // Menus
    public static final String MENU_TOOLBAR = "editor/SettlementPlanner/Toolbar";
    public static final String TEMPLATE_ENTRY_ROW = "editor/SettlementPlanner/settlement_planner_entry_row";
    public static final String TEMPLATE_ENTRY_ROW_SELECTED =
            "editor/SettlementPlanner/settlement_planner_entry_row_selected";
    public static final String TEMPLATE_ARCHITECTURE_ROW =
            "editor/SettlementPlanner/settlement_planner_architecture_row";
    public static final String TEMPLATE_ARCHITECTURE_ROW_SELECTED =
            "editor/SettlementPlanner/settlement_planner_architecture_row_selected";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_ENTRIES = 1;
    public static final int ENTRY_ARCHITECTURES = 2;

    // Elements
    public static final String ELEMENT_ENTRY_LABEL = "settlement_planner_entry_label";
    public static final String ELEMENT_ENTRY_SWATCH = "settlement_planner_entry_swatch";
    public static final String ELEMENT_ARCHITECTURE_LABEL = "settlement_planner_architecture_label";
}
