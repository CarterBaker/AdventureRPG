package editor.runtime.brushes;

public class BrushesSetting {

    /*
     * Constants used only by BrushesContext — its menu, its brush row
     * templates and their elements, and the entry point the brushes are
     * listed in. The brushes are the editor's.
     */

    // Menus
    public static final String MENU_BRUSHES = "editor/Brushes/Brushes";
    public static final String TEMPLATE_BRUSH_ROW = "editor/Brushes/brushes_preset_row";
    public static final String TEMPLATE_BRUSH_ROW_SELECTED = "editor/Brushes/brushes_preset_row_selected";
    public static final int ENTRY_BRUSHES = 0;

    // Elements
    public static final String ELEMENT_BRUSH_LABEL = "brushes_preset_label";
    public static final String ELEMENT_BRUSH_DETAIL = "brushes_preset_detail";
}
