package editor.runtime.filters;

public class FiltersSetting {

    /*
     * Constants used only by FiltersContext — the row templates its panels
     * are filled from and their elements, its entry points, and every word
     * the tab shows.
     */

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/Filters/filters_header_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/Filters/filters_text_row";
    public static final String TEMPLATE_DETAIL_ROW = "editor/Filters/filters_detail_row";
    public static final String TEMPLATE_FILTER_ROW = "editor/Filters/filters_filter_row";
    public static final String TEMPLATE_FILTER_ROW_SELECTED = "editor/Filters/filters_filter_row_selected";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "filters_row_label";
    public static final String ELEMENT_ROW_VALUE = "filters_row_value";

    // Entry Points
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_FILTERS = 1;
    public static final int ENTRY_DETAILS = 2;
    public static final int ENTRY_LIST_TITLE = 3;

    // Text
    public static final String TEXT_NO_FILTERS = "No photo filters yet: make one in the Hierarchy";
    public static final String TEXT_NAME = "Name";
    public static final String TEXT_CONTRAST = "Contrast";
    public static final String TEXT_VIGNETTE = "Vignette";
    public static final String TEXT_GRAIN = "Grain";
    public static final String TEXT_DRAG_HINT = "Drag a filter onto a clip in a Recorder tab to lay it over the clip";
    public static final String TEXT_FADE_HINT = "A filter fades in and out with its clip's transitions";
    public static final String TEXT_APPLY_HINT = "Apply Filter in the Tools tab lays the one picked here on the clip";
    public static final String TEXT_CHANGE_HINT = "Change or remove a clip's filter in the Properties tab";
    public static final String TEXT_EDIT_HINT = "Edit filters in the Hierarchy and Info Panel, as photo mode uses them";

    // Formats
    public static final String FORMAT_LIST_TITLE = "Filters (%d)";
    public static final String FORMAT_DECIMAL = "%.2f";
}
