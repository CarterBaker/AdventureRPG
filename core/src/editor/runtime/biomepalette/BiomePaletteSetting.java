package editor.runtime.biomepalette;

public class BiomePaletteSetting {

    /*
     * Constants used only by BiomePaletteContext — its menu with its biome
     * row templates, their elements and its entry points. The palette is the
     * editor's.
     */

    // Menus
    public static final String MENU_BIOME_PALETTE = "editor/BiomePalette/BiomePalette";
    public static final String TEMPLATE_BIOME_ROW = "editor/BiomePalette/biome_palette_biome_row";
    public static final String TEMPLATE_BIOME_ROW_SELECTED = "editor/BiomePalette/biome_palette_biome_row_selected";
    public static final int ENTRY_TITLE = 0;
    public static final int ENTRY_ROWS = 1;

    // Elements
    public static final String ELEMENT_BIOME_LABEL = "biome_palette_biome_label";
    public static final String ELEMENT_BIOME_SWATCH = "biome_palette_biome_swatch";
}
