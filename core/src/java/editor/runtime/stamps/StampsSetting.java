package editor.runtime.stamps;

public class StampsSetting {

    /*
     * Constants used only by StampsContext — its menu, its row of tiles and
     * detail line templates and their elements (each tile of a row numbered
     * after its prefix), the emblem among them, the tint of the tile in use
     * and of an empty one, and the entry points the tiles and the stamp's
     * details are listed in. The stamps are the editor's.
     */

    // Menus
    public static final String MENU_STAMPS = "editor/Stamps/Stamps";
    public static final String TEMPLATE_TILE_ROW = "editor/Stamps/stamps_tile_row";
    public static final String TEMPLATE_DETAIL_LINE = "editor/Stamps/stamps_detail_line";
    public static final int ENTRY_TILES = 0;
    public static final int ENTRY_DETAILS = 1;

    // Elements
    public static final String ELEMENT_TILE = "stamps_tile_";
    public static final String ELEMENT_TILE_LABEL = "stamps_tile_label_";
    public static final String ELEMENT_TILE_THUMBNAIL = "stamps_tile_thumbnail_";
    public static final String ELEMENT_DETAIL_LABEL = "stamps_detail_label";

    // Tints
    public static final float TILE_SELECTED_ALPHA = 0.32f;
    public static final float TILE_EMPTY_ALPHA = 0f;
}
