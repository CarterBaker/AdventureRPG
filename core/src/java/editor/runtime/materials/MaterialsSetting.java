package editor.runtime.materials;

public class MaterialsSetting {

    /*
     * Constants used only by MaterialsContext — the row templates its panels
     * are filled from and their elements, its entry points, and every word
     * the tab shows.
     */

    // Menus
    public static final String MENU_MATERIALS = "editor/Materials/Materials";

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/Materials/materials_header_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/Materials/materials_text_row";
    public static final String TEMPLATE_DETAIL_ROW = "editor/Materials/materials_detail_row";
    public static final String TEMPLATE_GRID_ROW = "editor/Materials/materials_grid_row";
    public static final String TEMPLATE_MATERIAL_TILE = "editor/Materials/materials_material_tile";
    public static final String TEMPLATE_MATERIAL_TILE_SELECTED = "editor/Materials/materials_material_tile_selected";
    public static final String TEMPLATE_DOCUMENT_ROW = "editor/Materials/materials_document_row";
    public static final String TEMPLATE_DOCUMENT_ROW_SELECTED = "editor/Materials/materials_document_row_selected";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "materials_row_label";
    public static final String ELEMENT_ROW_THUMBNAIL = "materials_row_thumbnail";
    public static final String ELEMENT_ROW_VALUE = "materials_row_value";

    // Grid — a tile's narrowest and widest, the gap between tiles and the room its name takes under its preview
    public static final float TILE_MIN_WIDTH_PIXELS = 76f;
    public static final float TILE_MAX_WIDTH_PIXELS = 112f;
    public static final float TILE_SPACING_PIXELS = 4f;
    public static final float TILE_LABEL_PIXELS = 18f;

    // Entry Points
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_MATERIALS = 1;
    public static final int ENTRY_DETAILS = 2;
    public static final int ENTRY_LIST_TITLE = 3;

    // Text
    public static final String TEXT_NO_MATERIALS = "No materials yet: import a texture through the Image Editor";
    public static final String TEXT_NAME = "Name";
    public static final String TEXT_PACK = "Pack";
    public static final String TEXT_BASE = "Base";
    public static final String TEXT_WORKSPACE = "Workspace";
    public static final String TEXT_DRAGGING = "Let go over an Item Editor's part to colour it";
    public static final String TEXT_PICK_HINT = "Pick a material for the Hair tab's strands and Brush";
    public static final String TEXT_DRAG_HINT = "Drag a material onto an item's part in the Item Editor";
    public static final String TEXT_HAIR_HINT = "Hair cards draw as intended only with hair materials";
    public static final String TEXT_PAINT_HINT = "The Paint tab's Material tool lays every map of the material chosen";
    public static final String TEXT_DOCUMENT_HINT = "Hair and items take its base until it is imported";

    // Formats
    public static final String FORMAT_LIST_TITLE = "Materials (%d)";
    public static final String FORMAT_STATUS = "Chosen: %s";
}
