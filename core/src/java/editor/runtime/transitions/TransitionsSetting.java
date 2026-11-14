package editor.runtime.transitions;

public class TransitionsSetting {

    /*
     * Constants used only by TransitionsContext — the row templates its
     * panels are filled from and their elements, its entry points, and every
     * word the tab shows.
     */

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/Transitions/transitions_header_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/Transitions/transitions_text_row";
    public static final String TEMPLATE_DETAIL_ROW = "editor/Transitions/transitions_detail_row";
    public static final String TEMPLATE_TRANSITION_ROW = "editor/Transitions/transitions_transition_row";
    public static final String TEMPLATE_TRANSITION_ROW_SELECTED =
            "editor/Transitions/transitions_transition_row_selected";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "transitions_row_label";
    public static final String ELEMENT_ROW_VALUE = "transitions_row_value";

    // Entry Points
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_TRANSITIONS = 1;
    public static final int ENTRY_DETAILS = 2;
    public static final int ENTRY_LIST_TITLE = 3;

    // Text
    public static final String TEXT_NO_TRANSITIONS = "No transitions yet: make one in the Hierarchy";
    public static final String TEXT_KIND = "Kind";
    public static final String TEXT_DIRECTION = "Direction";
    public static final String TEXT_COLOR = "Colour";
    public static final String TEXT_SOFTNESS = "Edge softness";
    public static final String TEXT_EASING = "Pace";
    public static final String TEXT_LENGTH = "First laid for";
    public static final String TEXT_NAME = "Name";
    public static final String TEXT_DRAG_HINT = "Drag a transition onto a clip's start or end in a Recorder tab";
    public static final String TEXT_CUT_HINT = "Let go on the end of a clip another follows to blend the two";
    public static final String TEXT_EDGE_HINT = "Alone on a clip's edge it fades from or to its colour";
    public static final String TEXT_LENGTH_HINT = "Drag a laid transition on the clip to make it run longer or shorter";
    public static final String TEXT_EDIT_HINT = "Edit transitions in the Hierarchy and Info Panel";

    // Formats
    public static final String FORMAT_LIST_TITLE = "Transitions (%d)";
    public static final String FORMAT_SECONDS = "%.2fs";
    public static final String FORMAT_COLOR = "%.2f  %.2f  %.2f";
    public static final String FORMAT_DECIMAL = "%.2f";
}
