package editor.runtime.biomeplanner;

public class BiomePlannerSetting {

    /*
     * Constants used only by BiomePlannerContext — its menu and the entry
     * points its grid cells and its status line are shown in. The grid and
     * its world are the editor's.
     */

    // Menus
    public static final String MENU_PLANNER = "editor/BiomePlanner/Planner";
    public static final int ENTRY_FIRST_CELL = 0;
    public static final int ENTRY_STATUS = 9;

    // Elements
    public static final String ELEMENT_CELL_SWATCH = "biome_planner_cell_swatch";
    public static final String ELEMENT_CELL_LABEL = "biome_planner_cell_label";

    // Text
    public static final String CELL_CENTRE_SUFFIX = "  (centre)";
    public static final String CELL_FOLLOWING_SUFFIX = "  (follows)";
}
