package editor.infopanel;

public class InfoPanelSetting {

    /*
     * Constants used only by InfoPanelContext — its menu, row templates, the
     * element ids rows are filled through, and how rows are indented.
     */

    // Menus
    public static final String MENU_INFO_PANEL = "editor/InfoPanel/InfoPanel";
    public static final String TEMPLATE_VALUE_ROW = "editor/InfoPanel/info_value_row";
    public static final String TEMPLATE_GROUP_ROW = "editor/InfoPanel/info_group_row";
    public static final String TEMPLATE_MISSING_ROW = "editor/InfoPanel/info_missing_row";

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

    // Layout
    public static final float ROW_OFFSET_PIXELS = 4f;
    public static final float INDENT_PIXELS = 14f;
    public static final float TOGGLE_WIDTH_PIXELS = 16f;
}
