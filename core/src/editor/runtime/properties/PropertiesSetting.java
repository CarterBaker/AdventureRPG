package editor.runtime.properties;

public class PropertiesSetting {

    /*
     * Constants used only by PropertiesContext — its menu, the Info and biome
     * row templates and the element ids they are filled through, its entry
     * points, how Info rows are indented, and the title the biome palette is
     * shown under. Value rows come from their own tools' menus.
     */

    // Menus
    public static final String MENU_PROPERTIES = "editor/Properties/Properties";
    public static final String TEMPLATE_VALUE_ROW = "editor/Properties/info_value_row";
    public static final String TEMPLATE_GROUP_ROW = "editor/Properties/info_group_row";
    public static final String TEMPLATE_MISSING_ROW = "editor/Properties/info_missing_row";
    public static final String TEMPLATE_BIOME_ROW = "editor/Properties/properties_biome_row";
    public static final String TEMPLATE_BIOME_ROW_SELECTED = "editor/Properties/properties_biome_row_selected";

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
    public static final String ELEMENT_REMOVE_BUTTON = "info_remove_button";
    public static final String ELEMENT_BIOME_LABEL = "properties_biome_label";
    public static final String ELEMENT_BIOME_SWATCH = "properties_biome_swatch";

    // Layout
    public static final float ROW_OFFSET_PIXELS = 4f;
    public static final float INDENT_PIXELS = 14f;
    public static final float TOGGLE_WIDTH_PIXELS = 16f;

    // Text
    public static final String TITLE_BIOMES = "Biome Palette";
}
