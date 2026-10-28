package editor.runtime.biomeplanner;

public class BiomePlannerSetting {

    /*
     * Constants used only by BiomePlannerContext — its menu, its catalogue's
     * biome row templates, and the entry points its catalogue, its grid cells
     * and its status line are shown in. The grid
     * and its world are the editor's.
     */

    // Menus
    public static final String MENU_PLANNER = "editor/BiomePlanner/Planner";
    public static final String TEMPLATE_BIOME_ROW = "editor/BiomePlanner/biome_planner_biome_row";
    public static final String TEMPLATE_BIOME_ROW_SELECTED = "editor/BiomePlanner/biome_planner_biome_row_selected";
    public static final int ENTRY_CATALOGUE = 0;
    public static final int ENTRY_FIRST_CELL = 1;
    public static final int ENTRY_STATUS = 10;

    // Elements
    public static final String ELEMENT_BIOME_LABEL = "biome_planner_biome_label";
    public static final String ELEMENT_BIOME_SWATCH = "biome_planner_biome_swatch";
    public static final String ELEMENT_CELL_SWATCH = "biome_planner_cell_swatch";
    public static final String ELEMENT_CELL_LABEL = "biome_planner_cell_label";

    // Text
    public static final String CELL_CENTRE_SUFFIX = "  (centre)";
    public static final String CELL_FOLLOWING_SUFFIX = "  (follows)";
}
