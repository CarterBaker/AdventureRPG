package editor.runtime.palette;

public class PaletteSetting {

    /*
     * Constants used only by PaletteContext — its menu with its biome and
     * height row templates. The palettes are the editor's.
     */

    // Menus
    public static final String MENU_PALETTE = "editor/Palette/Palette";
    public static final String TEMPLATE_BIOME_ROW = "editor/Palette/palette_biome_row";
    public static final String TEMPLATE_BIOME_ROW_SELECTED = "editor/Palette/palette_biome_row_selected";
    public static final String TEMPLATE_HEIGHT_ROW = "editor/Palette/palette_height_row";
    public static final String TEMPLATE_HEIGHT_ROW_SELECTED = "editor/Palette/palette_height_row_selected";
    public static final int ENTRY_TITLE = 0;
    public static final int ENTRY_ROWS = 1;

    // Elements
    public static final String ELEMENT_BIOME_LABEL = "palette_biome_label";
    public static final String ELEMENT_BIOME_SWATCH = "palette_biome_swatch";
    public static final String ELEMENT_HEIGHT_LABEL = "palette_height_label";
    public static final String ELEMENT_HEIGHT_SWATCH = "palette_height_swatch";
}
