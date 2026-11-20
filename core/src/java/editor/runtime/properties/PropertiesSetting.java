package editor.runtime.properties;

public class PropertiesSetting {

    /*
     * Constants used only by PropertiesContext — its menu, the Info value,
     * number, choice, group and missing row templates and the biome palette's
     * grid row and cell templates with the element ids they are filled
     * through, the palette's cell sizes, its entry
     * points and how Info rows are indented. A tool window's rows come from
     * its own menu.
     */

    // Menus
    public static final String MENU_PROPERTIES = "editor/Properties/Properties";
    public static final String TEMPLATE_VALUE_ROW = "editor/Properties/info_value_row";
    public static final String TEMPLATE_NUMBER_ROW = "editor/Properties/info_number_row";
    public static final String TEMPLATE_CHOICE_ROW = "editor/Properties/info_choice_row";
    public static final String TEMPLATE_GROUP_ROW = "editor/Properties/info_group_row";
    public static final String TEMPLATE_MISSING_ROW = "editor/Properties/info_missing_row";
    public static final String TEMPLATE_BIOME_CELL = "editor/Properties/biome_cell";
    public static final String TEMPLATE_BIOME_CELL_SELECTED = "editor/Properties/biome_cell_selected";
    public static final String TEMPLATE_BIOME_GRID_ROW = "editor/Properties/biome_grid_row";

    // Entry Points
    public static final int ENTRY_ROWS = 0;
    public static final int ENTRY_TITLE = 1;
    public static final int ENTRY_STATUS = 2;

    // Elements
    public static final String ELEMENT_KEY_LABEL = "info_key_label";
    public static final String ELEMENT_TOGGLE_LABEL = "info_toggle_label";
    public static final String ELEMENT_VALUE_BUTTON = "info_value_button";
    public static final String ELEMENT_VALUE_LABEL = "info_value_label";
    public static final String ELEMENT_ADD_BUTTON = "info_add_button";
    public static final String ELEMENT_DUPLICATE_BUTTON = "info_duplicate_button";
    public static final String ELEMENT_DECREASE_BUTTON = "info_decrease_button";
    public static final String ELEMENT_INCREASE_BUTTON = "info_increase_button";
    public static final String ELEMENT_TRACK = "info_track";
    public static final String ELEMENT_KNOB = "info_knob";
    public static final String ELEMENT_REMOVE_BUTTON = "info_remove_button";
    public static final String ELEMENT_BIOME_SWATCH = "biome_swatch";
    public static final String ELEMENT_BIOME_LABEL = "biome_label";

    // Layout
    public static final float ROW_OFFSET_PIXELS = 4f;
    public static final float INDENT_PIXELS = 14f;
    public static final float TOGGLE_WIDTH_PIXELS = 16f;

    // Biome Palette — the view's title, a swatch cell's narrowest and widest, its height and the room between cells
    public static final String TITLE_BIOME_PALETTE = "Biome Palette";
    public static final float BIOME_CELL_MIN_WIDTH_PIXELS = 92f;
    public static final float BIOME_CELL_MAX_WIDTH_PIXELS = 150f;
    public static final float BIOME_CELL_HEIGHT_PIXELS = 46f;
    public static final float BIOME_CELL_SPACING_PIXELS = 2f;
}
