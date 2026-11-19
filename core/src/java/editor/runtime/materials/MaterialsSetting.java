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
    public static final String TEMPLATE_MATERIAL_ROW = "editor/Materials/materials_material_row";
    public static final String TEMPLATE_MATERIAL_ROW_SELECTED = "editor/Materials/materials_material_row_selected";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "materials_row_label";
    public static final String ELEMENT_ROW_VALUE = "materials_row_value";

    // Entry Points
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_MATERIALS = 1;
    public static final int ENTRY_DETAILS = 2;
    public static final int ENTRY_LIST_TITLE = 3;

    // Text
    public static final String TEXT_NO_MATERIALS = "No materials yet: import a texture through the Image Editor";
    public static final String TEXT_NAME = "Name";
    public static final String TEXT_PACK = "Pack";
    public static final String TEXT_DRAGGING = "Let go over an Item Editor's part to colour it";
    public static final String TEXT_PICK_HINT = "Pick a material for the Hair tab's strands and Brush";
    public static final String TEXT_DRAG_HINT = "Drag a material onto an item's part in the Item Editor";
    public static final String TEXT_HAIR_HINT = "Hair cards draw as intended only with hair materials";

    // Formats
    public static final String FORMAT_LIST_TITLE = "Materials (%d)";
    public static final String FORMAT_STATUS = "Chosen: %s";
}
