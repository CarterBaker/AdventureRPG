package editor.runtime.brushes;

public class BrushesSetting {

    /*
     * Constants used only by BrushesContext — its menu, its preset, shape,
     * mode and number row templates and the entry points each is shown in.
     * The presets are the editor's.
     */

    // Menus
    public static final String MENU_BRUSHES = "editor/Brushes/Brushes";
    public static final String TEMPLATE_PRESET_ROW = "editor/Brushes/brushes_preset_row";
    public static final String TEMPLATE_PRESET_ROW_SELECTED = "editor/Brushes/brushes_preset_row_selected";
    public static final String TEMPLATE_SHAPE_BUTTON = "editor/Brushes/brushes_shape_button";
    public static final String TEMPLATE_SHAPE_BUTTON_SELECTED = "editor/Brushes/brushes_shape_button_selected";
    public static final String TEMPLATE_MODE_BUTTON = "editor/Brushes/brushes_mode_button";
    public static final String TEMPLATE_MODE_BUTTON_SELECTED = "editor/Brushes/brushes_mode_button_selected";
    public static final String TEMPLATE_FIELD_ROW = "editor/Brushes/brushes_field_row";
    public static final int ENTRY_PRESETS = 0;
    public static final int ENTRY_SHAPES = 1;
    public static final int ENTRY_MODES = 2;
    public static final int ENTRY_FIELDS = 3;
    public static final int ENTRY_SIZE = 4;

    // Elements
    public static final String ELEMENT_PRESET_LABEL = "brushes_preset_label";
    public static final String ELEMENT_PRESET_DETAIL = "brushes_preset_detail";
    public static final String ELEMENT_CHOICE_LABEL = "brushes_choice_label";
    public static final String ELEMENT_FIELD_LABEL = "brushes_field_label";
    public static final String ELEMENT_FIELD_VALUE = "brushes_field_value";
    public static final String ELEMENT_FIELD_LOWER = "brushes_field_lower";
    public static final String ELEMENT_FIELD_RAISE = "brushes_field_raise";

    // Text
    public static final String DETAIL_SEPARATOR = " / ";
    public static final String SIZE_PREFIX = "Size ";
    public static final String NO_VALUE = "-";
}
